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
 *  Solace AMQP JMS 2.0 Examples: QueueSender
 */

package com.solace.samples;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.JMSContext;
import javax.jms.JMSRuntimeException;
import javax.jms.TextMessage;
import javax.jms.Queue;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Sends a persistent message to a queue using JMS 2.0 API over AMQP 1.0. Solace Message Router is used as the message
 * broker.
 * 
 * The queue used for messages must exist on the message broker.
 */
public class QueueSender {

    private static final Logger LOG = LogManager.getLogger(QueueSender.class.getName());

    // connectionfactory.solaceConnectionLookup in file "jndi.properties"
    final String SOLACE_CONNECTION_LOOKUP = "solaceConnectionLookup";
    // queue.queueLookup in file "jndi.properties"
    final String QUEUE_LOOKUP = "queueLookup";

    private void run() {
        try {
            // pick up properties from the "jndi.properties" file
            Context initialContext = new InitialContext(); //
            ConnectionFactory factory = (ConnectionFactory) initialContext.lookup(SOLACE_CONNECTION_LOOKUP);

            // establish connection that uses the Solace Message Router as a message broker
            try (JMSContext context = factory.createContext()) {
                // the target for messages: a queue that already exists on the message broker
                Queue target = (Queue) initialContext.lookup(QUEUE_LOOKUP);
                // send message
                context.createProducer().setDeliveryMode(DeliveryMode.PERSISTENT)
                        .send(target, "Message with String Data");
                LOG.info("Message message sent successfully.");
            } catch (JMSRuntimeException ex) {
                LOG.error(ex);
            }

            initialContext.close();
        } catch (NamingException ex) {
            LOG.error(ex);
        }
    }

    public static void main(String[] args) {
        new QueueSender().run();
    }

}
