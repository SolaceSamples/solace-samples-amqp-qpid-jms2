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
 *  Solace AMQP JMS 2.0 Examples: SimpleReplier
 */

package com.solace.samples;

import org.apache.qpid.jms.JmsConnectionFactory;
import org.apache.qpid.jms.JmsDestination;
import org.apache.qpid.jms.JmsTemporaryQueue;

import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.TextMessage;
import javax.jms.Topic;

/**
 * Receives a request message using JMS 2.0 API over AMQP 1.0 and replies to it. Solace Message Router is used as the
 * message broker.
 * 
 * This is the Replier in the Request/Reply messaging pattern.
 */
public class BasicReplier {

    final String SOLACE_USERNAME = "clientUsername";
    final String SOLACE_PASSWORD = "password";

    final String REQUEST_TOPIC_NAME = "T/GettingStarted/requests";

    private void run(String... args) throws Exception {
        String solaceHost = args[0];
        System.out.printf("BasicReplier is connecting to Solace router %s...%n", solaceHost);

        // Programmatically create the connection factory using default settings
        ConnectionFactory connectionFactory = new JmsConnectionFactory(SOLACE_USERNAME, SOLACE_PASSWORD, solaceHost);

        // establish connection that uses the Solace Message Router as a message broker
        try (JMSContext context = connectionFactory.createContext()) {

            System.out.printf("Connected to the Solace router with client username '%s'.%n", SOLACE_USERNAME);

            // Create the request topic programmatically
            Topic requestTopic = context.createTopic(REQUEST_TOPIC_NAME);

            // Start receiving messages
            System.out.println("Awaiting request...");
            // the main thread blocks at the next statement until a message received
            Message request = context.createConsumer(requestTopic).receive();

            // process received request
            Destination replyDestination = request.getJMSReplyTo();
            if (replyDestination != null) {
                System.out.println("Received request, responding...");

                // workaround as the Apache Qpid JMS API always sets JMSReplyTo as non-temporary
                String replyDestinationName = ((JmsDestination) replyDestination).getName();
                replyDestination = new JmsTemporaryQueue(replyDestinationName);

                TextMessage reply = context.createTextMessage();
                String text = "Sample response";
                reply.setText(text);

                // Copy the correlation ID from the request to the reply
                reply.setJMSCorrelationID(request.getJMSCorrelationID());

                // Sent the reply
                // create producer and send the reply
                context.createProducer().setDeliveryMode(DeliveryMode.NON_PERSISTENT).send(replyDestination, reply);
                System.out.println("Responded successfully. Exiting...");
            } else {
                System.out.println("Received message without reply-to field.");
            }
        }
    }

    public static void main(String... args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: BasicReplier amqp://<msg_backbone_ip:amqp_port>");
            System.exit(-1);
        }
        new BasicReplier().run(args);
    }
}
