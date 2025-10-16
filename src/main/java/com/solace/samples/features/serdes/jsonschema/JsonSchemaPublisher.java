/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 *  Apache Qpid JMS 2.0 Solace AMQP Examples: JsonSchemaPublisher
 */

package com.solace.samples.features.serdes.jsonschema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.solace.serdes.Serializer;
import com.solace.serdes.common.resolver.config.SchemaResolverProperties;
import com.solace.serdes.jsonschema.JsonSchemaSerializer;
import jakarta.jms.BytesMessage;
import jakarta.jms.DeliveryMode;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;
import jakarta.jms.Topic;
import org.apache.qpid.jms.JmsConnectionFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * This class demonstrates how to use Apache Qpid JMS 2.0 API with JSON Schema serialization to produce messages over AMQP 1.0.
 * It serializes messages using the {@link JsonSchemaSerializer} and publishes them to a topic on a Solace message broker.
 */
public class JsonSchemaPublisher {

    private static final String TOPIC_NAME = "solace/samples/json";

    private static final String REGISTRY_URL = getEnv("REGISTRY_URL", "http://localhost:8081/apis/registry/v3");
    private static final String REGISTRY_USERNAME = getEnv("REGISTRY_USERNAME", "sr-readonly");
    private static final String REGISTRY_PASSWORD = getEnv("REGISTRY_PASSWORD", "roPassword");

    private void run(String... args) throws Exception {
        String solaceHost = args[0];
        String solaceUsername = args[1];
        String solacePassword = args[2];
        System.out.printf("JsonSchemaPublisher is connecting to Solace router %s...%n", solaceHost);

        // Create the connection factory and disable property name validation to allow schema registry headers which
        // contain characters not allowed by default JMS property name validation
        JmsConnectionFactory connectionFactory = new JmsConnectionFactory(solaceUsername, solacePassword, solaceHost);
        connectionFactory.setValidatePropertyNames(false);

        // Establish connection that uses the Solace Message Router as a message broker
        try (JMSContext context = connectionFactory.createContext()) {

            System.out.printf("Connected to the Solace router with client username '%s'.%n", solaceUsername);

            // Create the publishing topic programmatically
            Topic topic = context.createTopic(TOPIC_NAME);

            // Create the JSON Schema serializer
            try (Serializer<JsonNode> serializer = new JsonSchemaSerializer<>()) {
                serializer.configure(getConfig());

                // Create producer
                JMSProducer producer = context.createProducer().setDeliveryMode(DeliveryMode.NON_PERSISTENT);

                // Create user object to serialize
                JsonNode user = createUserObject();

                // Serialize the message and extract headers
                Map<String, Object> headers = new HashMap<>();
                byte[] payloadBytes = serializer.serialize(TOPIC_NAME, user, headers);

                // Create a BytesMessage
                BytesMessage message = context.createBytesMessage();
                message.writeBytes(payloadBytes);

                // Set the schema ID and other headers as application properties
                for (Map.Entry<String, Object> entry : headers.entrySet()) {
                    message.setObjectProperty(entry.getKey(), entry.getValue());
                }

                producer.send(topic, message);

                System.out.println("Published message with record: " + user);
            }
        }
    }


    /**
     * Creates a sample user object for demonstration purposes.
     * @return A JsonNode representing a user with name, id, and email fields.
     */
    private JsonNode createUserObject() {
        ObjectNode user = new ObjectMapper().createObjectNode();
        user.put("name", "John Doe");
        user.put("id", "123");
        user.put("email", "support@solace.com");
        return user;
    }

    /**
     * Gets the configuration for the serializer.
     * @return A map of configuration properties.
     */
    private Map<String, Object> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put(SchemaResolverProperties.REGISTRY_URL, REGISTRY_URL);
        config.put(SchemaResolverProperties.AUTH_USERNAME, REGISTRY_USERNAME);
        config.put(SchemaResolverProperties.AUTH_PASSWORD, REGISTRY_PASSWORD);
        return config;
    }

    /**
     * Gets an environment variable or returns a default value.
     * @param name The name of the environment variable.
     * @param defaultValue The default value to use if the environment variable is not set.
     * @return The environment variable value or default value.
     */
    private static String getEnv(String name, String defaultValue) {
        String value = System.getenv(name);
        return value != null ? value : defaultValue;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: JsonSchemaPublisher amqp://<msg_backbone_ip:amqp_port> <username> <password>");
            System.exit(-1);
        }
        new JsonSchemaPublisher().run(args);
    }
}
