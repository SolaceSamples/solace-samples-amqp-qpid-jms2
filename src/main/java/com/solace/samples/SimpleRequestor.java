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
 *  Solace AMQP JMS 2.0 Samples: SimpleRequestor
 */

package com.solace.samples;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.UUID;
import java.util.concurrent.Semaphore;

import javax.jms.DeliveryMode;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
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
 * AMQP Container that sends a request message using JMS 2.0 API. Solace Message Router is used as the message broker.
 * 
 * This is the Requestor in the Request/Reply messaging pattern.
 */
public class SimpleRequestor implements MessageListener {

    private static final Logger LOG = LogManager.getLogger(SimpleRequestor.class.getName());

    // connectionfactory.solaceConnectionLookup in file "jndi.properties"
    final String SOLACE_CONNECTION_LOOKUP = "solaceConnectionLookup";
    // queue.queueLookup in file "jndi.properties"
    final String QUEUE_LOOKUP = "queueLookup";

    final int ACK_MODE = Session.AUTO_ACKNOWLEDGE;
    final boolean TRANSACTED = false;

    final static Semaphore replyLatch = new Semaphore(0);

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

                // the AMQP Target for requests: a queue that already exists on the broker
                Queue queue = (Queue) context.lookup(QUEUE_LOOKUP);

                // Create AMQP Session and AMQP Producer with an outgoing Link to the broker.
                // The AMQP Consumer is represented by the JMS QueueSender.
                try (QueueSession session = connection.createQueueSession(TRANSACTED, ACK_MODE);
                        QueueSender requestor = session.createSender(queue)) {
                    requestor.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

                    // FIXME the AMQP Target for replies: a temporary queue
                    // Queue replyQueue = session.createTemporaryQueue();

                    Queue replyQueue = (Queue) context.lookup("replyQueueLookup");
                    try (QueueReceiver replyReceiver = session.createReceiver(replyQueue)) {
                        replyReceiver.setMessageListener(this);

                        TextMessage request = session.createTextMessage("AMQP Request with String Data");
                        request.setJMSReplyTo(replyQueue);
                        request.setJMSCorrelationID(UUID.randomUUID().toString());

                        requestor.send(request);
                        LOG.info("AMQP Request message sent successfully, waiting for a reply...");
                        replyLatch.acquire();
                    } catch (InterruptedException ex) {
                        LOG.error(ex);
                    }
                }
            }
        } catch (NamingException ex) {
            LOG.error(ex);
        } catch (JMSException ex) {
            LOG.error(ex);
        }
    }

    private static class QueueConnectionExceptionListener implements ExceptionListener {
        @Override
        public void onException(JMSException ex) {
            LOG.error(ex);
            replyLatch.release();
        }
    }

    @Override
    public void onMessage(Message message) {
        try {
            LOG.info("Received reply: \"{}\"", ((TextMessage) message).getText());
        } catch (JMSException ex) {
            LOG.error(ex);
        }
        replyLatch.release();
    }

    public static void main(String[] args) {
        new SimpleRequestor().run();
    }

}
