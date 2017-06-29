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
 *  Solace AMQP JMS 2.0 Samples: TopicSubscriber
 */

package com.solace.samples;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSRuntimeException;
import javax.jms.Topic;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Subscribes to messages published to a topic using JMS 2.0 API over AMQP 1.0 Solace Message Router is used as the
 * message broker.
 *
 * This is the Subscriber in the Publish/Subscribe messaging pattern.
 */
public class TopicSubscriber {

    private static final Logger LOG = LogManager.getLogger(TopicSubscriber.class.getName());

    // connectionfactory.solaceConnectionLookup in file "jndi.properties"
    final String SOLACE_CONNECTION_LOOKUP = "solaceConnectionLookup";
    // topic.topicLookup in file "jndi.properties"
    final String TOPIC_LOOKUP = "topicLookup";

    private void run() {
        try {
            // pick up properties from the "jndi.properties" file
            Context initialContext = new InitialContext();
            ConnectionFactory factory = (ConnectionFactory) initialContext
                    .lookup(SOLACE_CONNECTION_LOOKUP);

            // establish connection that uses the Solace Message Router as a message broker
            try (JMSContext context = factory.createContext()) {

                // the source for messages: a topic on the message broker
                Topic source = (Topic) initialContext.lookup(TOPIC_LOOKUP);

                // create consumer and wait for a message to arrive.
                LOG.info("Waiting for a message...");
                // the current thread blocks at the next statement until a message arrives
                String message = context.createConsumer(source).receiveBody(String.class);
                LOG.info("Received message with string data: \"{}\"", message);
            } catch (JMSRuntimeException ex) {
                LOG.error(ex);
            }
        } catch (NamingException ex) {
            LOG.error(ex);
        }
    }

    public static void main(String[] args) {
        new TopicSubscriber().run();
    }

}
