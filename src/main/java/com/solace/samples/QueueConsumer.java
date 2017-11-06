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
 *  Apache Qpid JMS 2.0 Solace AMQP Examples: QueueReceiver
 */

package com.solace.samples;

import org.apache.qpid.jms.JmsConnectionFactory;
import java.util.concurrent.CountDownLatch;

import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.TextMessage;
import javax.jms.Queue;

/**
 * Receives a persistent message from a queue using Apache Qpid JMS 2.0 API over AMQP. Solace Message Router is used as the message
 * broker.
 * 
 * The queue used for messages is created on the message broker.
 */
public class QueueConsumer {

    final String QUEUE_NAME = "Q/tutorial";

    private void run(String... args) throws Exception {
        String solaceHost = args[0];
        String solaceUsername = args[1];
        String solacePassword = args[2];
        System.out.printf("QueueConsumer is connecting to Solace router %s...%n", solaceHost);

        // Programmatically create the connection factory using default settings
        ConnectionFactory connectionFactory = new JmsConnectionFactory(solaceUsername, solacePassword, solaceHost);

        // establish connection that uses the Solace Message Router as a message broker
        try (JMSContext context = connectionFactory.createContext()) {
            // the source for messages: a queue that already exists on the broker
            System.out.printf("Connected with username '%s'.%n", solaceUsername);

            // Create the queue programmatically and the corresponding router resource
            Queue queue = context.createQueue(QUEUE_NAME);

            System.out.println("Awaiting message...");
            // create consumer and wait for a message to arrive.
            // the current thread blocks at the next statement until a message arrives
            Message message = context.createConsumer(queue).receive();

            // process received message
            if (message instanceof TextMessage) {
                System.out.printf("TextMessage received: '%s'%n", ((TextMessage) message).getText());
            } else {
                System.out.println("Message received.");
            }
            System.out.printf("Message Content:%n%s%n", message.toString());
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: QueueConsumer amqp://<msg_backbone_ip:amqp_port> <username> <password>");
            System.exit(-1);
        }
        new QueueConsumer().run(args);
    }
}
