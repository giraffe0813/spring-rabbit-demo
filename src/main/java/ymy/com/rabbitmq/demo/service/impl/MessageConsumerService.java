package ymy.com.rabbitmq.demo.service.impl;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MessageConsumerService implements MessageListener{
	
	@Autowired
	private AmqpTemplate amqpTemplate;


	public void onMessage(Message message) {
		System.out.println("接收到消息：" + new String(message.getBody()));

	}
}
