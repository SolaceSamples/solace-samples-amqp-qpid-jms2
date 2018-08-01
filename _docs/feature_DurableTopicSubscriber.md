---
layout: features
title: Durable JMS Subscription
summary: Demonstrates Durable JMS Subscription published to a topic.
---

 Creates a Durable JMS Subscription published to a topic using Apache Qpid JMS 2.0 API over AMQP 1.0 Solace Message Router is used as the
 message broker. Durable JMS Subscriptions are achieve by the Solace Message Broker by Durable Topic Endpoints (DTEs).

 This is the Subscriber in the Publish/Subscribe messaging pattern.



## Code

This is how to create or activate Durable JMS Subscription

~~~java
String message = context.createDurableConsumer(topic, SUBSCRIPTION_NAME).receiveBody(String.class);
~~~

## Learn More

For more information [click here](https://docs.solace.com/Solace-JMS-API/Creating-Durable-Topic-S.htm)
