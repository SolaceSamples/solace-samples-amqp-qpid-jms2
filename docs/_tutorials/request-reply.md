---
layout: tutorials
title: Request/Reply
summary: Demonstrates the request/reply message exchange pattern
icon: request-reply-icon.png
---

This tutorial builds on the basic concepts introduced in the [publish/subscribe tutorial]({{ site.baseurl }}/publish-subscribe){:target="_blank"}, and will show you how to send a request, reply to it, and receive the reply with Apache Qpid JMS 1.1 client using AMQP and the Solace Message Router. This the request/reply message exchange pattern as illustrated here:

![Sample Image Text]({{ site.baseurl }}/images/request-reply-icon.png)

This tutorial is available in [GitHub]({{ site.repository }}){:target="_blank"} along with the other [Solace Getting Started AMQP Tutorials]({{ site.links-get-started-amqp }}){:target="_top"}.

At the end, this tutorial walks through downloading and running the sample from source.

This tutorial focuses on using a non-Solace JMS API implementation. For using the Solace JMS API see [Solace Getting Started JMS Tutorials]({{ site.links-get-started-jms }}){:target="_blank"}.

## Assumptions

This tutorial assumes the following:

* You are familiar with Solace [core concepts]({{ site.docs-core-concepts }}){:target="_top"}.
* You have access to a running Solace message router with the following configuration:
    * Enabled `default` message VPN
    * Enabled `default` client username
    * Enabled `default` client profile with guaranteed messaging permissions.

One simple way to get access to a Solace message router is to start a Solace VMR load [as outlined here]({{ site.docs-vmr-setup }}){:target="_top"}. By default the Solace VMR will run with the “default” message VPN configured and ready for messaging. Going forward, this tutorial assumes that you are using the Solace VMR. If you are using a different Solace message router configuration, adapt the instructions to match your configuration.

## Goals

The goal of this tutorial is to demonstrate how to use Apache Qpid JMS 2.0 API over AMQP using the Solace Message Router. This tutorial will show you:

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

## Java Messaging Service (JMS) Introduction

JMS is a standard API for sending and receiving messages. As such, in addition to information provided on the Solace developer portal, you may also look at some external sources for more details about JMS. The following are good places to start

1. [http://java.sun.com/products/jms/docs.html](http://java.sun.com/products/jms/docs.html){:target="_blank"}.
2. [https://en.wikipedia.org/wiki/Java_Message_Service](https://en.wikipedia.org/wiki/Java_Message_Service){:target="_blank"}
3. [https://docs.oracle.com/javaee/7/tutorial/partmessaging.htm#GFIRP3](https://docs.oracle.com/javaee/7/tutorial/partmessaging.htm#GFIRP3){:target="_blank"}

The last (Oracle docs) link points you to the JEE official tutorials which provide a good introduction to JMS.

This tutorial focuses on using [JMS 2.0 (May 21, 2013)]({{ site.links-jms2-specification }}){:target="_blank"}, for [JMS 1.1 (April 12, 2002)]({{ site.links-jms1-specification }}){:target="_blank"} see [Solace Getting Started AMQP JMS 1.1 Tutorials]({{ site.links-get-started-amqp-jms1 }}){:target="_blank"}.

## Obtaining Apache Qpid JMS 2.0 API

This tutorial assumes you have downloaded and successfully installed the [Apache Qpid JMS client](https://qpid.apache.org/components/jms/index.html). If your environment differs from the example, then adjust the build instructions appropriately.

The easiest way to install it is through Gradle or Maven.

### Get the API: Using Gradle

```
dependencies {
    compile("org.apache.qpid:qpid-jms-client:0.23.+")
}
```

### Get the API: Using Maven

```
<dependency>
    <groupId>org.apache.qpid</groupId>
    <artifactId>qpid-jms-client</artifactId>
    <version>[0.23,)</version>
</dependency>
```s.

## Connecting to the Solace Message Router

In order to send or receive messages, an application must start a JMS connection and a session.

There are three parameters for establishing the JMS connection: the Solace Message Router host name with the AMQP service port number, the client username and the optional password.

*BasicRequestor.java/BasicReplier.java*
```java
final String SOLACE_USERNAME = "clientUsername";
final String SOLACE_PASSWORD = "password";

String solaceHost = args[0];
ConnectionFactory connectionFactory = new JmsConnectionFactory(SOLACE_USERNAME, SOLACE_PASSWORD, solaceHost);
```

Notice how JMS 2.0 API combines `Connection` and `Session` objects into the `JMSContext` object.

*BasicRequestor.java/BasicReplier.java*
```java
JMSContext context = connectionFactory.createContext()
```

The session created by the `JMSContext` object by default is non-transacted and uses the acknowledge mode that automatically acknowledges a client's receipt of a message.

Notice how JMS 2.0 API combines `Connection` and `Session` objects into the `JMSContext` object.

At this point the application is connected to the Solace Message Router and ready to send and receive request and reply messages.

## Sending a request

In order to send a request a JMS *Producer* needs to be created.

![sending-message-to-queue]({{ site.baseurl }}/images/request-reply-details-2.png)

Also, it is necessary to allocate a temporary queue for receiving the reply.

*BasicRequestor.java*
```java
TemporaryQueue replyToQueue = context.createTemporaryQueue();
```

The request must have two properties assigned: `JMSReplyTo` and `JMSCorrelationID`.

The `JMSReplyTo` property needs to have the value of the temporary queue for receiving the reply that was already created.

The `JMSCorrelationID` property needs to have an unique value so the requestor to correlate the request with the subsequent reply.

The figure below outlines the exchange of messages and the role of both properties.

![]({{ site.baseurl }}/images/request-reply-details-1.png)


*BasicRequestor.java*
```java
TextMessage request = context.createTextMessage("Sample Request");
request.setJMSReplyTo(replyToQueue);
String correlationId = UUID.randomUUID().toString();
request.setJMSCorrelationID(correlationId);
```

Now we create a JMS producer and send the request. We assign the delivery mode to `non-persistent` for better performance.

The JMS 2.0 API allows the use of *method chaining* to create the producer, set the delivery mode and send the message.

*BasicRequestor.java*
```java
final String REQUEST_TOPIC_NAME = "T/GettingStarted/requests";

Topic requestTopic = context.createTopic(REQUEST_TOPIC_NAME);
context.createProducer().setDeliveryMode(DeliveryMode.NON_PERSISTENT).send(requestTopic, request);
```

## Receiving a request

In order to receive a request from a queue a JMS *Consumer* needs to be created.

We create a JMS consumer and receive the request in the same, main thread.

The JMS 2.0 API allows the use of *method chaining* to create the consumer and receive a message from the queue.

*BasicReplier.java*
```java
Topic requestTopic = context.createTopic(REQUEST_TOPIC_NAME);
Message request = context.createConsumer(requestTopic).receive();
```

## Replying to a request

To reply to a received request a JMS *Producer* needs to be created.

![Request-Reply_diagram-3]({{ site.baseurl }}/images/request-reply-details-3.png)

The reply message must have the `JMSCorrelationID` property value assigned from the received request. Create the reply message using the current `JMSContext` and assign its `JMSCorrelationID` property from the request value:

*BasicReplier.java*
```java
Message request = context.createConsumer(requestTopic).receive();

TextMessage reply = context.createTextMessage();
String text = "Sample response";
reply.setText(text);
reply.setJMSCorrelationID(request.getJMSCorrelationID());
```

Now we can send the reply message.

We must send it to the temporary queue that was created by the requestor. Create an instance of the `org.apache.qpid.jms.JmsDestination` class for the reply destination and assign it a name from the request `JMSReplyTo` property because of the way the Apache Qpid JMS client is implementated.

*BasicReplier.java*
```java
Destination replyDestination = request.getJMSReplyTo();
String replyDestinationName = ((JmsDestination) replyDestination).getName();
replyDestination = new JmsTemporaryQueue(replyDestinationName);
```

A JMS producer needs to be created to send the reply message. Assign its delivery mode to `non-persistent` for better performance.

The JMS 2.0 API allows the use of *method chaining* to create the producer, set the delivery mode and send the reply message.

*BasicReplier.java*
```java
context.createProducer().setDeliveryMode(DeliveryMode.NON_PERSISTENT).send(replyDestination, reply);
```

The reply will be received in the main thread by the `BasicRequestor`.

*BasicRequestor.java*
```java
final int REPLY_TIMEOUT_MS = 10000;

Message reply = context.createConsumer(replyToQueue).receive(REPLY_TIMEOUT_MS);
```

If you execute the `BasicReplier.java` program, it will block at the `context.createConsumer(requestTopic).receive()` call until a request is received. Now, if you execute the `BasicRequestor.java` that sends the request, the `BasicReplier.java` program will resume and reply to the request. That will unblock the `BasicRequestor.java` program that was blocked on the `context.createConsumer(replyToQueue).receive(REPLY_TIMEOUT_MS)` call waiting for the reply to its request.


## Summarizing

Combining the example source code shown above results in the following source code files:

*   [BasicRequestor.java]({{ site.repository }}/blob/master/src/main/java/com/solace/samples/BasicRequestor.java){:target="_blank"}
*   [BasicReplier.java]({{ site.repository }}/blob/master/src/main/java/com/solace/samples/BasicReplier.java){:target="_blank"}

### Getting the Source

Clone the GitHub repository containing the Solace samples.

```
git clone {{ site.repository }}
cd {{ site.baseurl | remove: '/'}}
```

### Building

You can build and run both example files directly from Eclipse or with Gradle.

```sh
./gradlew assemble
```

The examples can be run as:

```sh
cd build/staged/bin
./basicReplier amqp://SOLACE_HOST:AMQP_PORT
./basicRequestor amqp://SOLACE_HOST:AMQP_PORT
```


### Sample Output

First start the `BasicReplier` so that it is up and waiting for requests.

```sh
$ basicReplier amqp://SOLACE_HOST:AMQP_PORT
BasicReplier is connecting to Solace router amqp://SOLACE_HOST:AMQP_PORT...
Connected to the Solace router with client username 'clientUsername'.
Awaiting request...
```

Then you can start the `BasicRequestor` to send the request and receive the reply.
```sh
$ basicRequestor amqp://SOLACE_HOST:AMQP_PORT
BasicRequestor is connecting to Solace router amqp://SOLACE_HOST:AMQP_PORT...
Connected to the Solace router with client username 'clientUsername'.
Sending request 'Sample Request' to topic 'T/GettingStarted/requests'...
Sent successfully. Waiting for reply...
TextMessage response received: 'Sample response'
Message Content:
JmsTextMessage { org.apache.qpid.jms.provider.amqp.message.AmqpJmsTextMessageFacade@527740a2 }
```

Notice how the request is received by the `BasicReplier` and replied to.

```sh
Awaiting request...
Received request, responding...
Responded successfully. Exiting...
```

Now you know how to use Apache Qpid JMS 2.0 API over AMQP using the Solace Message Router to implement the request/reply message exchange pattern.

If you have any issues sending and receiving request or reply, check the [Solace community]({{ site.links-community }}){:target="_top"} for answers to common issues seen.
