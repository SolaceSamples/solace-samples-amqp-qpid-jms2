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
public class DurableTopicSubscriber {

    final String TOPIC_NAME = "T/GettingStarted/pubsub";
    final String DTE_NAME = "GettingStarted_DTE";
    final String CLIENT_ID = "GettingStarted";

    private void run(String... args) throws Exception {
        String solaceHost = args[0];
        String solaceUsername = args[1];
        String solacePassword = args[2];
        System.out.printf("DurableTopicSubscriber is connecting to Solace router %s...%n", solaceHost);

        // Programmatically create the connection factory using default settings
        ConnectionFactory connectionFactory = new JmsConnectionFactory(solaceUsername, solacePassword, solaceHost);
        
        // Establish connection that uses the Solace Message Router as a message broker
        try (JMSContext context = connectionFactory.createContext()) {
        	// set client id on jms connection
        	context.setClientID(CLIENT_ID);
        	
            System.out.printf("Connected to the Solace router with client username '%s'.%n", solaceUsername);

            // Create the publishing topic programmatically
            Topic topic = context.createTopic(TOPIC_NAME);

            System.out.println("Awaiting message...");
            // create consumer and wait for a message to arrive.
            // the current thread blocks at the next statement until a message arrives
            String message = context.createDurableConsumer(topic, DTE_NAME).receiveBody(String.class);

            System.out.printf("Message received: '%s'%n", message);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: DurableTopicSubscriber amqp://<msg_backbone_ip:amqp_port> <username> <password>");
            System.exit(-1);
        }
        new DurableTopicSubscriber().run(args);
    }
}
