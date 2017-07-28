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
 *  Apache Qpid JMS 2.0 Solace AMQP Examples: TopicSubscriber
 */

package com.solace.samples;

import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.Topic;

import org.apache.qpid.jms.JmsConnectionFactory;

/**
 * Subscribes to messages published to a topic using Apache Qpid JMS 2.0 API over AMQP 1.0 Solace Message Router is used as the
 * message broker.
 *
 * This is the Subscriber in the Publish/Subscribe messaging pattern.
 */
public class TopicSubscriber {

    final String SOLACE_USERNAME = "clientUsername";
    final String SOLACE_PASSWORD = "password";

    final String TOPIC_NAME = "T/GettingStarted/pubsub";

    private void run(String... args) throws Exception {
        String solaceHost = args[0];
        System.out.printf("TopicSubscriber is connecting to Solace router %s...%n", solaceHost);

        // Programmatically create the connection factory using default settings
        ConnectionFactory connectionFactory = new JmsConnectionFactory(SOLACE_USERNAME, SOLACE_PASSWORD, solaceHost);

        // Establish connection that uses the Solace Message Router as a message broker
        try (JMSContext context = connectionFactory.createContext()) {

            System.out.printf("Connected to the Solace router with client username '%s'.%n", SOLACE_USERNAME);

            // Create the publishing topic programmatically
            Topic topic = context.createTopic(TOPIC_NAME);

            System.out.println("Awaiting message...");
            // create consumer and wait for a message to arrive.
            // the current thread blocks at the next statement until a message arrives
            String message = context.createConsumer(topic).receiveBody(String.class);

            System.out.printf("Message received: '%s'%n", message);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: TopicSubscriber amqp://<msg_backbone_ip:amqp_port>");
            System.exit(-1);
        }
        new TopicSubscriber().run(args);
    }
}
