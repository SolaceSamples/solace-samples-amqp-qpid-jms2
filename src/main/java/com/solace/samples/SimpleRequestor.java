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
 *  Solace AMQP JMS 2.0 Examples: SimpleRequestor
 */

package com.solace.samples;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.UUID;
import java.util.concurrent.Semaphore;

import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSRuntimeException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.jms.Queue;
import javax.jms.TemporaryQueue;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Sends a request message using JMS 2.0 API over AMQP 1.0 and receives a reply to it. Solace Message Router is used as
 * the message broker.
 * 
 * The queues used for requests must exist on the message broker.
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
            ConnectionFactory factory = (ConnectionFactory) initialContext.lookup(SOLACE_CONNECTION_LOOKUP);

            // establish connection that uses the Solace Message Router as a message broker
            try (JMSContext context = factory.createContext()) {
                // the target for requests: a queue that already exists on the message broker
                Queue target = (Queue) initialContext.lookup(QUEUE_LOOKUP);
                // the source for replies: a temporary queue
                TemporaryQueue replyQueue = context.createTemporaryQueue();
                // create consumer for reply messages
                context.createConsumer(replyQueue).setMessageListener(this);
                // prepare request
                TextMessage request = context.createTextMessage("Request with String Data");
                request.setJMSReplyTo(replyQueue);
                request.setJMSCorrelationID(UUID.randomUUID().toString());

                // create producer and send request
                context.createProducer().setDeliveryMode(DeliveryMode.NON_PERSISTENT)
                        .send(target, request);
                LOG.info("Request message sent successfully, waiting for a reply...");
                // the current thread blocks at the next statement until the replyLatch is released
                replyLatch.acquire();
            } catch (JMSException ex) {
                LOG.error(ex);
            } catch (JMSRuntimeException ex) {
                LOG.error(ex);
            } catch (InterruptedException ex) {
                LOG.error(ex);
            }

            initialContext.close();
        } catch (NamingException ex) {
            LOG.error(ex);
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
