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
 *  Apache Qpid JMS 2.0 Solace AMQP Examples: BasicRequestor
 */

package com.solace.samples;

import org.apache.qpid.jms.JmsConnectionFactory;

import java.util.UUID;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TemporaryQueue;

/**
 * Sends a request message using Apache Qpid JMS 2.0 API over AMQP 1.0 and receives a reply to it. Solace Message Router is used as
 * the message broker.
 * 
 * This is the Requestor in the Request/Reply messaging pattern.
 */
public class BasicRequestor {

    final String SOLACE_USERNAME = "clientUsername";
    final String SOLACE_PASSWORD = "password";

    final String REQUEST_TOPIC_NAME = "T/GettingStarted/requests";

    final int REPLY_TIMEOUT_MS = 10000; // 10 seconds

    private void run(String... args) throws Exception {
        String solaceHost = args[0];
        System.out.printf("BasicRequestor is connecting to Solace router %s...%n", solaceHost);

        // Programmatically create the connection factory using default settings
        ConnectionFactory connectionFactory = new JmsConnectionFactory(SOLACE_USERNAME, SOLACE_PASSWORD, solaceHost);

        // establish connection that uses the Solace Message Router as a message broker
        try (JMSContext context = connectionFactory.createContext()) {

            System.out.printf("Connected to the Solace router with client username '%s'.%n", SOLACE_USERNAME);

            // Create the request topic programmatically
            Topic requestTopic = context.createTopic(REQUEST_TOPIC_NAME);

            // The response will be received on this temporary queue.
            TemporaryQueue replyToQueue = context.createTemporaryQueue();

            // Create a request.
            TextMessage request = context.createTextMessage("Sample Request");
            // The application must put the destination of the reply in the replyTo field of the request
            request.setJMSReplyTo(replyToQueue);
            // The application must put a correlation ID in the request
            String correlationId = UUID.randomUUID().toString();
            request.setJMSCorrelationID(correlationId);

            System.out.printf("Sending request '%s' to topic '%s'...%n", request.getText(), requestTopic.toString());

            // create producer and send request
            context.createProducer().setDeliveryMode(DeliveryMode.NON_PERSISTENT).send(requestTopic, request);

            System.out.println("Sent successfully. Waiting for reply...");

            // create consumer and wait for a message to arrive.
            // the current thread blocks at the next statement until a message arrives
            Message reply = context.createConsumer(replyToQueue).receive(REPLY_TIMEOUT_MS);

            if (reply == null) {
                throw new Exception("Failed to receive a reply in " + REPLY_TIMEOUT_MS + " msecs");
            }

            // Process the reply
            if (reply.getJMSCorrelationID() == null) {
                throw new Exception(
                        "Received a reply message with no correlationID. This field is needed for a direct request.");
            }

            // Apache Qpid JMS prefixes correlation ID with string "ID:" so remove such prefix for interoperability
            if (!reply.getJMSCorrelationID().replaceAll("ID:", "").equals(correlationId)) {
                throw new Exception("Received invalid correlationID in reply message.");
            }

            if (reply instanceof TextMessage) {
                System.out.printf("TextMessage response received: '%s'%n", ((TextMessage) reply).getText());
            } else {
                System.out.println("Message response received.");
            }

            System.out.printf("Message Content:%n%s%n", reply.toString());
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: BasicRequestor amqp://<msg_backbone_ip:amqp_port>");
            System.exit(-1);
        }
        new BasicRequestor().run(args);
    }

}
