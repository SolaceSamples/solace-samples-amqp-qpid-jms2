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

import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.ExceptionListener;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.jms.Queue;
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

    final static Semaphore replyLatch = new Semaphore(0);

    private void run() {
        try {
            // pick up properties from the "jndi.properties" file
            Context initialContext = new InitialContext(); //
            ConnectionFactory factory = (ConnectionFactory) initialContext
                    .lookup(SOLACE_CONNECTION_LOOKUP);

            // establish AMQP Connection that uses the Solace Message Router as a broker
            try (JMSContext context = factory.createContext()) {
                context.setExceptionListener(new QueueConnectionExceptionListener());

                // the AMQP Source for replies: a queue on the broker
                Queue replySource = (Queue) initialContext.lookup("replyQueueLookup");
                context.createConsumer(replySource).setMessageListener(this);

                Message request = context.createTextMessage("AMQP Request with String Data");
                request.setJMSReplyTo(replySource);
                request.setJMSCorrelationID(UUID.randomUUID().toString());

                // the AMQP Target for requests: a queue that already exists on the broker
                Queue target = (Queue) initialContext.lookup(QUEUE_LOOKUP);
                // Create AMQP Producer with an outgoing Link to the broker.
                context.createProducer()
                        .setDeliveryMode(DeliveryMode.NON_PERSISTENT)
                        .send(target, request);
                LOG.info("AMQP Request message sent successfully, waiting for a reply...");
                try {
                    replyLatch.acquire();
                } catch (InterruptedException ex) {
                    LOG.error(ex);
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
