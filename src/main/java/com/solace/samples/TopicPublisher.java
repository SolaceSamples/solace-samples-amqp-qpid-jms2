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
 *  Apache Qpid JMS 2.0 Solace AMQP Examples: TopicPublisher
 */

package com.solace.samples;

import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.JMSContext;
import javax.jms.TextMessage;
import javax.jms.Topic;

import org.apache.qpid.jms.JmsConnectionFactory;

/**
 * Publishes a messages to a topic using Apache Qpid JMS 2.0 API over AMQP 1.0. Solace Message Router is used as the message broker.
 *
 * This is the Publisher in the Publish/Subscribe messaging pattern.
 */
public class TopicPublisher {

    final String TOPIC_NAME = "T/GettingStarted/pubsub";

    private void run(String... args) throws Exception {
        String solaceHost = args[0];
        String solaceUsername = args[1];
        String solacePassword = args[2];
        System.out.printf("TopicPublisher is connecting to Solace router %s...%n", solaceHost);

        // Programmatically create the connection factory using default settings
        ConnectionFactory connectionFactory = new JmsConnectionFactory(solaceUsername, solacePassword, solaceHost);

        // Establish connection that uses the Solace Message Router as a message broker
        try (JMSContext context = connectionFactory.createContext()) {

            System.out.printf("Connected to the Solace router with client username '%s'.%n", solaceUsername);

            // Create the publishing topic programmatically
            Topic topic = context.createTopic(TOPIC_NAME);

            // Create the message
            TextMessage message = context.createTextMessage("Hello world!");

            System.out.printf("Sending message '%s' to topic '%s'...%n", message.getText(), topic.toString());

            // Create producer and publish the message
            context.createProducer().setDeliveryMode(DeliveryMode.NON_PERSISTENT).send(topic, message);

            System.out.println("Sent successfully. Exiting...");
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: TopicPublisher amqp://<msg_backbone_ip:amqp_port> <username> <password>");
            System.exit(-1);
        }
        new TopicPublisher().run(args);
    }

}
