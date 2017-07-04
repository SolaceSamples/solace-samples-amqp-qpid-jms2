---
layout: tutorials
title: Publish/Subscribe
summary: Demonstrates the publish/subscribe message exchange pattern
icon: publish-subscribe-icon.png
---

This tutorial will show you to how to connect a JMS 2.0 API client to a Solace Message Router using AMQP, add a topic subscription and publish a message matching this topic subscription. This is the publish/subscribe message exchange pattern as illustrated here:

![Sample Image Text]({{ site.baseurl }}/images/publish-subscribe-icon.png)

## Assumptions

This tutorial assumes the following:

* You are familiar with Solace messaging [core concepts]({{ site.docs-core-concepts }}){:target="_top"}.
* You have access to a running Solace message router with the following configuration:
    * Enabled “default” message VPN
    * Enabled “default” client username

One simple way to get access to a Solace message router is to start a Solace VMR load [as outlined here]({{ site.docs-vmr-setup }}){:target="_top"}. By default the Solace VMR will run with the “default” message VPN configured and ready for messaging. Going forward, this tutorial assumes that you are using the Solace VMR. If you are using a different Solace message router configuration, adapt the instructions to match your configuration.

## Goals

The goal of this tutorial is to demonstrate how to use JMS 2.0 API over AMQP using the Solace Message Router. This tutorial will show you:

1. How to build and send a message on a topic
2. How to subscribe to a topic and receive a message

## Solace message router properties

In order to send or receive messages to a Solace message router, you need to know a few details about how to connect to the Solace message router. Specifically, you need to know the following:

<table>
<tbody>
<tr>
<th>Resource</th>
<th>Value</th>
<th>Description</th>
</tr>
<tr>
<td>Host</td>
<td>String of the form <code>DNS name</code> or <code>IP:Port</code></td>
<td>This is the address clients use when connecting to the Solace Message Router to send and receive messages. For a Solace VMR this there is only a single interface so the IP is the same as the management IP address. For Solace message router appliances this is the host address of the message-backbone. The port number must match the port number for the plain text AMQP service on the router.</td>
</tr>
<tr>
<td>Message VPN</td>
<td>String</td>
<td>The “default” Solace message router Message VPN that this client will connect to.</td>
</tr>
<tr>
<td>Client Username</td>
<td>String</td>
<td>The “default” client username.</td>
</tr>
</tbody>
</table>

## Obtaining JMS 2.0 API

This tutorial assumes you have downloaded and successfully installed the [Apache Qpid JMS client](https://qpid.apache.org/components/jms/index.html). If your environment differs from the example, then adjust the build instructions appropriately.

The easiest way to install the JMS 2.0 API is through Maven. See the project's *pom.xml* file for details.

## Connecting to the Solace Message Router

In order to send or receive messages, an application must start a JMS connection.

There is only one required parameter for establishing the JMS connection: the Solace Message Router host name with the AMQP service port number. The value of this parameter is loaded in the examples by the `javax.naming.InitialContext.InitialContext()` from the *jndi.properties* project's file, but it could be assigned directly in the application by assigning the corresponding environment variable.

*jndi.properties*
~~~
java.naming.factory.initial = org.apache.qpid.jms.jndi.JmsInitialContextFactory
connectionfactory.solaceConnectionLookup = amqp://192.168.123.45:8555
~~~

Notice how JMS 2.0 API combines `Connection` and `Session` objects into the `JMSContext` object.

*TopicPublisher.java/TopicSubscriber.java*
~~~java
Context initialContext = new InitialContext();
ConnectionFactory factory = (ConnectionFactory) initialContext.lookup("solaceConnectionLookup");

try (JMSContext context = factory.createContext()) {
...
~~~

The session created by the `JMSContext` object by default is non-transacted and uses the acknowledge mode that automatically acknowledges a client's receipt of a message.

At this point the application is connected to the Solace Message Router and ready to publish messages.

## Publishing messages

A JMS producer needs to be created in order to publish a message to a topic. Assign its delivery mode to “non-persistent” for better performance.

The name of the topic is loaded by the `javax.naming.InitialContext.InitialContext()` from the *jndi.properties* project's file.

*jndi.properties*
~~~
topic.topicLookup = amqp.tutorial.topic
~~~

JMS 2.0 API allows the use of *method chaining* to create the producer, set the delivery mode and publish the message.

*TopicPublisher.java*
~~~java
context.createProducer().setDeliveryMode(DeliveryMode.NON_PERSISTENT).send(target, "Message with String Data");
~~~

If you execute the `TopicPublisher.java` program, it will successfully publish a message, but another application is required to receive the message.

## Receiving message

To receive a message from a topic a JMS consumer needs to be created.

The name of the topic is loaded by `javax.naming.InitialContext.InitialContext()` from the *jndi.properties* project's file. Its name is the same as the one to which we publish messages.

*jndi.properties*
~~~
topic.topicLookup = amqp.tutorial.topic
~~~

JMS 2.0 API allows the use of *method chaining* to create the consumer, and receive messages published to the subscribed topic.

*TopicSubscriber.java*
~~~java
Topic source = (Topic) initialContext.lookup("topicLookup");
String message = context.createConsumer(source).receiveBody(String.class);
...
~~~

If you execute the `TopicSubscriber.java` program, it will block at the `receiveBody(String.class)` call until a message is received. Now, if you execute the `TopicPublisher.java` that publishes a message, the `TopicSubscriber.java` program will resume and print out the received message.

## Summarizing

Combining the example source code shown above results in the following source code files:

*   [TopicPublisher.java]({{ site.repository }}/blob/master/src/main/java/com/solace/samples/TopicPublisher.java){:target="_blank"}
*   [TopicSubscriber.java]({{ site.repository }}/blob/master/src/main/java/com/solace/samples/TopicSubscriber.java){:target="_blank"}

## Building

Modify the *jndi.properties* file to reflect your Solace Message Router host and port number for the AMQP service.

You can build and run both example files directly from Eclipse.

To build a jar file that includes all dependencies execute the following:

~~~sh
mvn assembly:single
~~~

Then the examples can be executed as:

~~~sh
java -cp ./target/solace-samples-amqp-jms2-1.0.1-SNAPSHOT-jar-with-dependencies.jar  com.solace.samples.TopicSubscriber
java -cp ./target/solace-samples-amqp-jms2-1.0.1-SNAPSHOT-jar-with-dependencies.jar  com.solace.samples.TopicPublisher
~~~

## Sample Output

First, start `TopicSubscriber` so that it's up and waiting for published messages. You can start multiple instances of this application, and all of them will receive published messages.

~~~sh
$ java -cp ./target/solace-samples-amqp-jms2-1.0.1-SNAPSHOT-jar-with-dependencies.jar  com.solace.samples.TopicSubscriber
2017-06-29T15:20:10,706 INFO sasl.SaslMechanismFinder - Best match for SASL auth was: SASL-ANONYMOUS
2017-06-29T15:20:10,717 INFO samples.TopicSubscriber - Waiting for a message...
2017-06-29T15:20:10,743 INFO jms.JmsConnection - Connection ID:8b0451d8-13ac-4d88-a900-d9f969cb722e:1 connected to remote Broker: amqp://192.168.123.45:8555
~~~

Then you can start `TopicPublisher` to publish a message.
~~~sh
$ java -cp ./target/solace-samples-amqp-jms2-1.0.1-SNAPSHOT-jar-with-dependencies.jar com.solace.samples.TopicPublisher
2017-06-29T15:21:15,790 INFO sasl.SaslMechanismFinder - Best match for SASL auth was: SASL-ANONYMOUS
2017-06-29T15:21:15,818 INFO jms.JmsConnection - Connection ID:f28e5ffb-1310-4ad5-87d9-a819fb4d8116:1 connected to remote Broker: amqp://192.168.123.45:8555
2017-06-29T15:21:15,859 INFO samples.TopicPublisher - Message published successfully.
2017-06-29T15:21:15,863 INFO jms.JmsSession - A JMS MessageProducer has been closed: JmsProducerInfo { ID:f28e5ffb-1310-4ad5-87d9-a819fb4d8116:1:1:1, destination = null }
~~~

Notice how the published message is received by the the `TopicSubscriber`.

~~~sh
...
2017-06-29T15:20:03,794 INFO samples.TopicSubscriber - Waiting for a message...
2017-06-29T15:20:03,813 INFO jms.JmsConnection - Connection ID:a09e9b79-9c34-422a-ac14-9ec928b5883a:1 connected to remote Broker: amqp://192.168.123.45:8555
2017-06-29T15:21:15,877 INFO samples.TopicSubscriber - Received message with string data: "Message with String Data"
2017-06-29T15:21:15,885 INFO jms.JmsSession - A JMS MessageConsumer has been closed: JmsConsumerInfo: { ID:a09e9b79-9c34-422a-ac14-9ec928b5883a:1:1:1, destination = amqp.tutorial.topic }
~~~

You now know how to use JMS 2.0 API over AMQP using the Solace Message Router to implement the publish/subscribe message exchange pattern.

If you have any issues publishing and receiving a message, check the [Solace community]({{ site.links-community }}){:target="_top"} for answers to common issues seen.
