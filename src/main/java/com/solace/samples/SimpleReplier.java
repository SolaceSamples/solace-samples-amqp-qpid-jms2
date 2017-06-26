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
 *  Solace AMQP JMS 1.1 Samples: SimpleReplier
 */

package com.solace.samples;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.concurrent.TimeUnit;

import javax.jms.DeliveryMode;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueConnection;
import javax.jms.QueueSession;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * AMQP Container that uses JMS 1.1 API to receive a request message and replies to it. Solace Message Router is used as
 * the message broker.
 * 
 * This is the Replier in the Request/Reply messaging pattern.
 */
public class SimpleReplier {

    private static final Logger LOG = LogManager.getLogger(SimpleReplier.class.getName());

    // connectionfactory.solaceConnectionLookup in file "jndi.properties"
    final String SOLACE_CONNECTION_LOOKUP = "solaceConnectionLookup";
    // queue.queueLookup in file "jndi.properties"
    final String QUEUE_LOOKUP = "queueLookup";

    // AMQP Session parameters
    final int ACK_MODE = Session.AUTO_ACKNOWLEDGE;
    final boolean TRANSACTED = false;

    private void run() {
        try {
            // pick up properties from the "jndi.properties" file
            Context context = new InitialContext(); //
            QueueConnectionFactory factory = (QueueConnectionFactory) context
                    .lookup(SOLACE_CONNECTION_LOOKUP);

            // establish AMQP Connection that uses the Solace Message Router as a broker
            try (QueueConnection connection = factory.createQueueConnection()) {
                connection.setExceptionListener(new QueueConnectionExceptionListener());
                connection.start();

                // the AMQP Source for messages: a queue that already exists on the broker
                Queue queue = (Queue) context.lookup(QUEUE_LOOKUP);

                // Create AMQP Session and AMQP Consumer with an incoming Link to the broker.
                // Subscribe to messages from the Source and wait for a request to arrive.
                // The AMQP Consumer is represented by the JMS QueueReceiver.
                // Also create AMQP Producer (represented by JMS QueueSender) with an outgoing Link
                // to the broker. This producer will be used to reply to the received requests.
                try (QueueSession session = connection.createQueueSession(TRANSACTED, ACK_MODE);
                        QueueReceiver requestConsumer = session.createReceiver(queue);
                        QueueSender replyProducer = session.createSender(null)) {
                    replyProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

                    LOG.info("Waiting for a request...");
                    // the current thread blocks here until a request arrives
                    Message request = requestConsumer.receive();
                    if (request instanceof TextMessage) {
                        LOG.info("Received AMQP request with string data: \"{}\"",
                                ((TextMessage) request).getText());
                    } else {
                        LOG.warn("Unexpected data type in request: \"{}\"", request.toString());
                    }

                    // create response with string data
                    TextMessage response = session.createTextMessage(
                            String.format("Reply to \"%s\"", ((TextMessage) request).getText()));
                    response.setJMSCorrelationID(request.getJMSCorrelationID());
                    replyProducer.send(request.getJMSReplyTo(), response);
                    LOG.info("AMQP Request Message replied successfully.");
                    TimeUnit.SECONDS.sleep(10);
                } catch (InterruptedException ex) {
                    LOG.error(ex);
                }
            }
        } catch (

        NamingException ex) {
            LOG.error(ex);
        } catch (JMSException ex) {
            LOG.error(ex);
        }
    }

    private static class QueueConnectionExceptionListener implements ExceptionListener {
        @Override
        public void onException(JMSException ex) {
            LOG.error(ex);
        }

    }

    public static void main(String[] args) {
        new SimpleReplier().run();
    }

}
