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
 *  Solace AMQP JMS 2.0 Examples: QueueReceiver
 */

package com.solace.samples;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSRuntimeException;
import javax.jms.Message;
import javax.jms.TextMessage;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Receives a persistent message from a queue using JMS 2.0 API over AMQP. Solace Message Router is used as the message
 * broker.
 * 
 * The queue used for messages must exist on the message broker.
 */
public class QueueReceiver {

    private static final Logger LOG = LogManager.getLogger(QueueReceiver.class.getName());

    // connectionfactory.solaceConnectionLookup in file "jndi.properties"
    final String SOLACE_CONNECTION_LOOKUP = "solaceConnectionLookup";
    // queue.queueLookup in file "jndi.properties"
    final String QUEUE_LOOKUP = "queueLookup";

    private void run() {
        try {
            // pick up properties from the "jndi.properties" file
            Context initialContext = new InitialContext(); //
            QueueConnectionFactory factory = (QueueConnectionFactory) initialContext.lookup(SOLACE_CONNECTION_LOOKUP);

            // establish connection that uses the Solace Message Router as a message broker
            try (JMSContext context = factory.createContext()) {
                // the source for messages: a queue that already exists on the broker
                Queue source = (Queue) initialContext.lookup(QUEUE_LOOKUP);
                // create consumer and wait for a message to arrive.
                LOG.info("Waiting for a persistent message...");
                // the current thread blocks at the next statement until a message arrives
                Message message = context.createConsumer(source).receive();
                // process received request
                if (message instanceof TextMessage) {
                    // process received message
                    TextMessage textMessage = (TextMessage) message;
                    LOG.info("Received message with string data: \"{}\"", textMessage.getText());
                } else {
                    LOG.warn("Unexpected data type in message: \"{}\".", message.toString());
                }
            } catch (JMSException ex) {
                LOG.error(ex);
            } catch (JMSRuntimeException ex) {
                LOG.error(ex);
            }

            initialContext.close();
        } catch (NamingException ex) {
            LOG.error(ex);
        }
    }

    public static void main(String[] args) {
        new QueueReceiver().run();
    }

}
