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
 *  Solace AMQP JMS 1.1 Samples: MessageProducer
 */

package com.solace.samples;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicConnection;
import javax.jms.TopicSession;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * AMQP Container that produces messages using JMS 1.1 API. Solace Message Router is used as the message broker.
 * 
 * This is the Publisher in the Publish/Subscribe messaging pattern.
 */
public class MessageProducer {

    private static final Logger LOG = LogManager.getLogger(MessageProducer.class.getName());

    // connectionfactory.solaceConnectionLookup in file "jndi.properties"
    final String SOLACE_CONNECTION_LOOKUP = "solaceConnectionLookup";
    // topic.topicLookup in file "jndi.properties"
    final String TOPIC_LOOKUP = "topicLookup";

    // AMQP Session parameters
    final int ACK_MODE = Session.AUTO_ACKNOWLEDGE;
    final boolean TRANSACTED = false;

    private void run() {
        try {
            // pick up properties from the "jndi.properties" file
            Context context = new InitialContext();
            TopicConnectionFactory factory = (TopicConnectionFactory) context
                    .lookup(SOLACE_CONNECTION_LOOKUP);

            // establish AMQP Connection that uses the Solace Message Router as a broker
            try (TopicConnection connection = factory.createTopicConnection()) {
                connection.setExceptionListener(new TopicConnectionExceptionListener());
                connection.start();

                // the AMQP Target for publishing messages: a topic on the broker
                Topic target = (Topic) context.lookup(TOPIC_LOOKUP);

                // Create AMQP Session and AMQP Producer with an outgoing Link to the broker.
                // The AMQP Producer is represented by the JMS Publisher
                try (TopicSession session = connection.createTopicSession(TRANSACTED, ACK_MODE);
                        TopicPublisher producer = session.createPublisher(target)) {

                    // publish one message with string data
                    producer.publish(session.createTextMessage("AMQP Message with String Data"));
                    LOG.info("AMQP Message published successfully.");
                }
            }
        } catch (NamingException ex) {
            LOG.error(ex);
        } catch (JMSException ex) {
            LOG.error(ex);
        }
    }

    private static class TopicConnectionExceptionListener implements ExceptionListener {
        @Override
        public void onException(JMSException ex) {
            LOG.error(ex);
        }
    }

    public static void main(String[] args) {
        new MessageProducer().run();
    }

}
