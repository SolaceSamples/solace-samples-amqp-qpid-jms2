---
layout: tutorials
title: Request/Reply
summary: Demonstrates the request/reply message exchange pattern
icon: request-reply-icon.png
---

This tutorial will demonstrate to you to how to connect a JMS 2.0 API client to a Solace Message Router using AMQP, send a request, reply to it, and receive the reply. This the request/reply message exchange pattern as illustrated here:

![Sample Image Text]({{ site.baseurl }}/images/request-reply-icon.png)

## Assumptions

This tutorial assumes the following:

* You are familiar with Solace [core concepts]({{ site.docs-core-concepts }}){:target="_top"}.
* You have access to a running Solace message router with the following configuration:
    * Enabled “default” message VPN
    * Enabled “default” client username

One simple way to get access to a Solace message router is to start a Solace VMR load [as outlined here]({{ site.docs-vmr-setup }}){:target="_top"}. By default the Solace VMR will run with the “default” message VPN configured and ready for messaging. Going forward, this tutorial assumes that you are using the Solace VMR. If you are using a different Solace message router configuration, adapt the instructions to match your configuration.

## Goals

The goal of this tutorial is to demonstrate how to use JMS 2.0 API over AMQP using the Solace Message Router. This tutorial will show you:

1. How to build and send a request message
2. How to receive a request message and respond to it

## Solace message router properties

In order to send or receive messages to a Solace message router, you need to know a few details of how to connect to the Solace message router. Specifically you need to know the following:

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
<td>This is the address client’s use when connecting to the Solace Message Router to send and receive messages. For a Solace VMR this there is only a single interface so the IP is the same as the management IP address. For Solace message router appliances this is the host address of the message-backbone. The port number must match the port number for the plain text AMQP service on the router.</td>
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

This tutorial depends on you having the [Apache Qpid JMS client](https://qpid.apache.org/components/jms/index.html) downloaded and installed for your project, and the instructions in this tutorial assume you successfully done it. If your environment differs then adjust the build instructions appropriately.

The easiest way to do it through Maven. See the project's *pom.xml* file for details.

## Connecting to the Solace Message Router

In order to send or receive messages, an application must start a JMS connection.

There is only one required parameter for establishing the JMS connection: the Solace Message Router host name with the AMQP service port number. The value of this parameter is loaded in the examples by the `javax.naming.InitialContext.InitialContext()` from the *jndi.properties* project's file, but of course it could be assigned directly in the application by assigning the corresponding environment variable.

*jndi.properties*
~~~
java.naming.factory.initial = org.apache.qpid.jms.jndi.JmsInitialContextFactory
connectionfactory.solaceConnectionLookup = amqp://192.168.123.45:8555
~~~

Notice how JMS 2.0 API combines `Connection` and `Session` objects into the `JMSContext` object.

*SimpleRequestor.java/SimpleReplier.java*
~~~java
Context initialContext = new InitialContext();
ConnectionFactory factory = (ConnectionFactory) initialContext.lookup("solaceConnectionLookup");

try (JMSContext context = factory.createContext()) {
...
~~~

The session created by the `JMSContext` object by default is non-transacted and uses the acknowledge mode that automatically acknowledges a client's receipt of a message.

At this point the application is connected to the Solace Message Router and ready to send and receive request and reply messages.

## Sending a request

The name of the queue for sending requests is loaded by `javax.naming.InitialContext.InitialContext()` from the *jndi.properties* project's file. It must exist on the Solace Message Router as a `durable queue`.

*jndi.properties*
~~~
queue.queueLookup = amqp/tutorial/queue
~~~

*SimpleRequestor.java*
~~~java
Queue target = (Queue) initialContext.lookup("queueLookup");
...
~~~

Also, it is necessary to allocate a temporary queue for receiving the reply.

*SimpleRequestor.java*
~~~java
TemporaryQueue replyQueue = session.createTemporaryQueue();
context.createConsumer(replyQueue).setMessageListener(this);
~~~

Because the `SimpleRequestor` class will be receiving replies, it needs to implement `javax.jms.MessageListener`:

*SimpleRequestor.java*
~~~java
public class SimpleRequestor implements MessageListener {
<...>
    @Override
    public void onMessage(Message message) {
        try {
            LOG.info("Received reply: \"{}\"", ((TextMessage) message).getText());
<...>
~~~

The request must have two properties assigned: `JMSReplyTo` and `JMSCorrelationID`.

The `JMSReplyTo` property needs to have the value of the temporary queue for receiving the reply that was already created.

The `JMSCorrelationID` property needs to have an unique value.

*SimpleRequestor.java*
~~~java
TextMessage request = session.createTextMessage("Request with String Data");
request.setJMSReplyTo(replyQueue);
request.setJMSCorrelationID(UUID.randomUUID().toString());
~~~

Create a JMS producer and send the request. Assign the delivery mode to “non-persistent” for better performance. The JMS 2.0 API allows the use of *method chaining* to create the producer, set the delivery mode and send the message.

*SimpleRequestor.java*
~~~java
context.createProducer().setDeliveryMode(DeliveryMode.NON_PERSISTENT).send(target, request);
~~~

## Receiving a request

The name of the queue for receiving requests is loaded by `javax.naming.InitialContext.InitialContext()` from the *jndi.properties* project's file, and it's the same as the one to which we send requests.

*jndi.properties*
~~~
queue.queueLookup = amqp/tutorial/queue
~~~

*SimpleReplier.java*
~~~java
Queue source = (Queue) initialContext.lookup("queueLookup");
...
~~~

Create a JMS consumer and receive the request. The JMS 2.0 API allows the use of *method chaining* to create the consumer and receive a message from the queue.

*SimpleReplier.java*
~~~java
Message request = context.createConsumer(source).receive();
~~~

## Replying to a request

The reply message must have the `JMSCorrelationID` property value assigned from the received request. Create the reply message using the current `JMSContext` and assign its `JMSCorrelationID` property from the request value:

*SimpleReplier.java*
~~~java
Message request = context.createConsumer(source).receive();
if (request instanceof TextMessage) {
    TextMessage requestTextMessage = (TextMessage) request;
    Message replyMessage = context.createTextMessage(String.format("Reply to \"%s\"", requestTextMessage.getText()));
    replyMessage.setJMSCorrelationID(request.getJMSCorrelationID());
...
~~~

Now we can send the reply message.

We must send it to the temporary queue that was created by the requestor. Create an instance of the `org.apache.qpid.jms.JmsTemporaryQueue` class for the reply destination and assign it a name from the request `JMSReplyTo` property because of the Apache Qpid JMS implementation.

*SimpleReplier.java*
~~~java
Destination replyDestination = new JmsTemporaryQueue(((Queue) request.getJMSReplyTo()).getQueueName());
~~~

A JMS producer needs to be created to send the reply message. Assign its delivery mode to “non-persistent” for better performance. The JMS 2.0 API allows the use of *method chaining* to create the producer, set the delivery mode and send the reply message.

*SimpleReplier.java*
~~~java
context.createProducer().setDeliveryMode(DeliveryMode.NON_PERSISTENT).send(replyDestination, replyMessage);
...
~~~

The reply will be received in a separate thread by the `SimpleRequestor.onMessage` routine.

## Summarizing

Combining the example source code shown above results in the following source code files:

*   [SimpleRequestor.java]({{ site.repository }}/blob/master/src/main/java/com/solace/samples/SimpleRequestor.java){:target="_blank"}
*   [SimpleReplier.java]({{ site.repository }}/blob/master/src/main/java/com/solace/samples/SimpleReplier.java){:target="_blank"}

## Building

Modify the *jndi.properties* file to reflect your Solace Message Router host and port number for the AMQP service.

You can build and run both example files directly from Eclipse.

To build a jar file that includes all dependencies execute the following:

~~~sh
mvn assembly:single
~~~

The examples can be executed as:

~~~sh
java -cp ./target/solace-samples-amqp-jms2-1.0.1-SNAPSHOT-jar-with-dependencies.jar  com.solace.samples.SimpleReplier
java -cp ./target/solace-samples-amqp-jms2-1.0.1-SNAPSHOT-jar-with-dependencies.jar  com.solace.samples.SimpleRequestor
~~~

## Sample Output

First, start the `SimpleReplier` so that it's up and waiting for requests.

~~~sh
$ java -cp ./target/solace-samples-amqp-jms2-1.0.1-SNAPSHOT-jar-with-dependencies.jar com.solace.samples.SimpleReplier
2017-06-29T17:23:00,880 INFO sasl.SaslMechanismFinder - Best match for SASL auth was: SASL-ANONYMOUS
2017-06-29T17:23:00,893 INFO samples.SimpleReplier - Waiting for a request...
2017-06-29T17:23:00,913 INFO jms.JmsConnection - Connection ID:c512a0a2-3f83-42b4-a80f-f9f23f644d88:1 connected to remote Broker: amqp://192.168.123.45:8555
~~~

Then you can start the `SimpleRequestor` to send the request and receive the reply.
~~~sh
$ java -cp ./target/solace-samples-amqp-jms2-1.0.1-SNAPSHOT-jar-with-dependencies.jar com.solace.samples.SimpleRequestor
2017-06-29T17:23:32,613 INFO sasl.SaslMechanismFinder - Best match for SASL auth was: SASL-ANONYMOUS
2017-06-29T17:23:32,642 INFO jms.JmsConnection - Connection ID:655fc087-7c2f-4e40-8fc2-cf40a4a05ba8:1 connected to remote Broker: amqp://192.168.123.45:8555
2017-06-29T17:23:32,712 INFO samples.SimpleRequestor - Request message sent successfully, waiting for a reply...
2017-06-29T17:23:32,752 INFO samples.SimpleRequestor - Received reply: "Reply to "Request with String Data""
2017-06-29T17:23:32,757 INFO jms.JmsSession - A JMS MessageConsumer has been closed: JmsConsumerInfo: { ID:655fc087-7c2f-4e40-8fc2-cf40a4a05ba8:1:1:1, destination = #P2P/QTMP/v:vmr-133-16/qpid-jms:temp-queue-creator:ID:655fc087-7c2f-4e40-8fc2-cf40a4a05ba8:1:1 }
~~~

Notice how the request is received by the `SimpleReplier` and replied to.

~~~sh
...
2017-06-29T17:23:00,893 INFO samples.SimpleReplier - Waiting for a request...
2017-06-29T17:23:00,913 INFO jms.JmsConnection - Connection ID:c512a0a2-3f83-42b4-a80f-f9f23f644d88:1 connected to remote Broker: amqp://192.168.123.45:8555
2017-06-29T17:23:32,727 INFO samples.SimpleReplier - Received request with string data: "Request with String Data"
2017-06-29T17:23:32,742 INFO samples.SimpleReplier - Request Message replied successfully.
2017-06-29T17:23:35,746 INFO jms.JmsSession - A JMS MessageConsumer has been closed: JmsConsumerInfo: { ID:c512a0a2-3f83-42b4-a80f-f9f23f644d88:1:1:1, destination = amqp/tutorial/queue }
~~~

Now you know how to use JMS 2.0 API over AMQP using the Solace Message Router to implement the request/reply message exchange pattern.

If you have any issues sending and receiving request or reply, check the [Solace community]({{ site.links-community }}){:target="_top"} for answers to common issues seen.
