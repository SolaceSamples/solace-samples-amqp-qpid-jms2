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
 *  Apache Qpid JMS 2.0 Solace AMQP Examples: JsonSchemaConsumer
 */

package com.solace.samples.features.serdes.jsonschema;

import com.fasterxml.jackson.databind.JsonNode;
import com.solace.serdes.Deserializer;
import com.solace.serdes.common.resolver.config.SchemaResolverProperties;
import com.solace.serdes.jsonschema.JsonSchemaDeserializer;
import jakarta.jms.BytesMessage;
import jakarta.jms.JMSContext;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;
import org.apache.qpid.jms.JmsConnectionFactory;

import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * This class demonstrates how to use Apache Qpid JMS 2.0 API with JSON Schema deserialization to consume messages over AMQP 1.0.
 * It subscribes to a topic on a Solace message broker and deserializes the received messages using the {@link JsonSchemaDeserializer}.
 */
public class JsonSchemaConsumer {

    private static final String TOPIC_NAME = "solace/samples/json";

    private static final String REGISTRY_URL = getEnv("REGISTRY_URL", "http://localhost:8081/apis/registry/v3");
    private static final String REGISTRY_USERNAME = getEnv("REGISTRY_USERNAME", "sr-readonly");
    private static final String REGISTRY_PASSWORD = getEnv("REGISTRY_PASSWORD", "roPassword");

    private void run(String... args) throws Exception {
        String solaceHost = args[0];
        String solaceUsername = args[1];
        String solacePassword = args[2];
        System.out.printf("JsonSchemaConsumer is connecting to Solace router %s...%n", solaceHost);

        // Create the connection factory and disable property name validation to allow schema registry headers which
        // contain characters not allowed by default JMS property name validation
        JmsConnectionFactory connectionFactory = new JmsConnectionFactory(solaceUsername, solacePassword, solaceHost);
        connectionFactory.setValidatePropertyNames(false);

        // Establish connection that uses the Solace Message Router as a message broker
        try (JMSContext context = connectionFactory.createContext()) {

            System.out.printf("Connected to the Solace router with client username '%s'.%n", solaceUsername);

            // Create the publishing topic programmatically
            Topic topic = context.createTopic(TOPIC_NAME);

            // Create the JSON Schema deserializer
            try (Deserializer<JsonNode> deserializer = new JsonSchemaDeserializer<>()) {
                deserializer.configure(getConfig());

                System.out.println("Awaiting message...");
                // Create consumer and wait for a message to arrive.
                // The current thread blocks at the next statement until a message arrives
                Message message = context.createConsumer(topic).receive();

                // Process received message
                byte[] payloadBytes;
                Map<String, Object> headers = new HashMap<>();

                if (message instanceof BytesMessage) {
                    BytesMessage bytesMessage = (BytesMessage) message;

                    // Read the payload bytes
                    payloadBytes = new byte[(int) bytesMessage.getBodyLength()];
                    bytesMessage.readBytes(payloadBytes);
                } else if (message instanceof TextMessage) {
                    TextMessage textMessage = (TextMessage) message;

                    // Convert text to bytes
                    String text = textMessage.getText();
                    payloadBytes = text.getBytes(StandardCharsets.UTF_8);
                } else {
                    System.out.println("Unexpected message type received: " + message.getClass().getName());
                    return;
                }

                // Extract headers from message properties
                Enumeration<?> propertyNames = message.getPropertyNames();
                while (propertyNames.hasMoreElements()) {
                    String propertyName = (String) propertyNames.nextElement();
                    headers.put(propertyName, message.getObjectProperty(propertyName));
                }

                // Deserialize the message
                JsonNode user = deserializer.deserialize(TOPIC_NAME, payloadBytes, headers);

                System.out.println("Received message with record: " + user);
            }
        }
    }

    /**
     * Gets the configuration for the deserializer.
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
            System.out.println("Usage: JsonSchemaConsumer amqp://<msg_backbone_ip:amqp_port> <username> <password>");
            System.exit(-1);
        }
        new JsonSchemaConsumer().run(args);
    }
}
