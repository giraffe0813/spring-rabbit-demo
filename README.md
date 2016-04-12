> 在做搜索服务时，当业务方数据源改变时，需要改变搜索引擎中索引的数据。可以`定时拉取`也可以`实时推送`。为了实现同步更新，选择了实时推送。实时推送也有两种方式，一种是提供索引更新接口供业务方调用，在接口中将变化的数据更新到搜索引擎中；另一种是使用消息队列，业务方在数据改变时，将改变的数据插入队列，服务端的消费者实时监听队列，并进行索引更新。考虑到使用消息队列有三点好处，选择了第二种方式。`使用消息队列的好处`: 一，使用消息队列可以异步处理更新操作降低接口响应时间；二，对于消费端由于不可预测原因导致消息无法处理时，数据可以暂存在队列中，等消费者服务恢复后可以继续处理历史数据,提高可用性；三，将业务方的服务和索引更新服务解耦。下面简单记录一下一个简单的spring+rabbitmq的demo实现，只实现了简单的消息生产和消费。

博客地址:http://yemengying.com

参考了下面几篇博客：   
http://syntx.io/getting-started-with-rabbitmq-using-the-spring-framework/   
https://blog.codecentric.de/en/2011/04/amqp-messaging-with-rabbitmq/    
http://wb284551926.iteye.com/blog/2212869

<!-- more -->
## 本地安装rabbitmq
只介绍mac系统下如何安装
### 安装brew

```
ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"
```
### 利用brew安装rabbitmq
1.输入命令

```
brew install rabbitmq
```
2.添加配置

```
export PATH=$PATH:$(brew --prefix)/sbin
```
3.可以禁用用不着的插件

```
 rabbitmq-plugins disable --offline rabbitmq_stomp
 rabbitmq-plugins disable --offline rabbitmq_mqtt
```

4.启动rabbitmq server 直接输入下面的命令

```
rabbitmq-server
```
5.如果打印出下面的东东就是启动成功了，默认端口是5672

![spring-rabbitMq](/images/spring-rabbit.jpg)

## 实现demo
首先要有个可以运行的spring + maven的项目
### 添加maven依赖
在pom文件中添加rabbitmq相关的依赖

```
<dependency>
	<groupId>com.rabbitmq</groupId>
	<artifactId>amqp-client</artifactId>
	<version>3.5.4</version>
</dependency>
<dependency>
	<groupId>org.springframework.amqp</groupId>
	<artifactId>spring-rabbit</artifactId>
	<version>1.5.0.RELEASE</version>
</dependency>
```
### 添加配置文件

```
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:context="http://www.springframework.org/schema/context"
	xmlns:rabbit="http://www.springframework.org/schema/rabbit" xmlns:p="http://www.springframework.org/schema/p"
	xsi:schemaLocation="http://www.springframework.org/schema/beans
http://www.springframework.org/schema/beans/spring-beans-4.0.xsd http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context-4.0.xsd http://www.springframework.org/schema/rabbit http://www.springframework.org/schema/rabbit/spring-rabbit.xsd">

	<!-- 扫描包 -->
	<context:component-scan base-package="ymy.com.rabbitmq.demo.service.impl.*" />

	<context:annotation-config />

	<!-- 连接本地rabbitmq -->
	<rabbit:connection-factory id="connectionFactory"
		host="localhost" port="5672" />

	<rabbit:admin connection-factory="connectionFactory" id="amqpAdmin" />

	<!-- queue 队列声明 -->
	<rabbit:queue id="rabbit_queue_one" durable="true"
		auto-delete="false" exclusive="false" name="rabbit_queue_one" />

	<!-- exchange queue binging key 绑定 -->
	<rabbit:direct-exchange name="mq-exchange"
		durable="true" auto-delete="false" id="mq-exchange">
		<rabbit:bindings>
			<rabbit:binding queue="rabbit_queue_one" key="rabbit_queue_one" />
		</rabbit:bindings>
	</rabbit:direct-exchange>

	<!-- spring template声明 -->
	<rabbit:template exchange="mq-exchange" id="amqpTemplate"
		connection-factory="connectionFactory" />
</beans>
```
### 生产者开发（插入消息）

```
package ymy.com.rabbitmq.demo.service.impl;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service("messageProductorService")
public class MessageProductorService {

	@Autowired
	private AmqpAdmin admin;
	@Autowired
	private AmqpTemplate amqpTemplate;
	@Autowired
	private ConnectionFactory connectionFactory;

	public void pushToMessageQueue(String routingKey, String message) {
		amqpTemplate.convertAndSend(routingKey, message);
	}
}

```
### 消费者开发
有两种方式从消息队列中获取消息，一种是自己调用receive方法获得，一种是为队列配置监听类，每当监听的队列中有消息产生，就会被监听的类去除。
第一种方式：

```
package ymy.com.rabbitmq.demo.service.impl;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
@Service("messageConsumerService")
public class MessageConsumerService {
	@Autowired
	private AmqpTemplate amqpTemplate;
	
	public void popMessage(String destinationQueueName) {
		Message message = amqpTemplate.receive(destinationQueueName);
		System.out.println(new String(message.getBody()));
	}

}
```
junit test

```
	@Test
	public void testMessageQueueManager(){
        messageProductor.pushToMessageQueue("rabbit_queue_one", "hello giraffe");
        messageConsumer.popMessage("rabbit_queue_one");
	}
```

第二种方式
配置中添加监听

```
	<!-- queue litener  观察 监听模式 当有消息到达时会通知监听在对应的队列上的监听对象 taskExecutor这个需要自己实现一个连接池 按照官方说法 除非特别大的数据量 一般不需要连接池-->
    <rabbit:listener-container connection-factory="connectionFactory" acknowledge="auto" >
        <rabbit:listener queues="rabbit_queue_one" ref="messageConsumerService"/>
    </rabbit:listener-container> 
```
消费者类需要实现MessageListener 并实现onMessage方法，当监听的队列中有消息进入时，onMessage方法会被调用

```
package ymy.com.rabbitmq.demo.service.impl;

import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Service;
import org.springframework.amqp.core.MessageListener;

@Service("messageConsumerService")
public class MessageConsumerService implements MessageListener{

	@Override
	public void onMessage(Message message) {
		System.out.println("成功取出消息" + new String(message.getBody()));
	}
}
```
