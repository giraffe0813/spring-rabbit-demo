package me.ele.bpm.skyrim.test.service;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ymy.com.rabbitmq.demo.service.impl.MessageConsumerService;
import ymy.com.rabbitmq.demo.service.impl.MessageProductorService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({
		"classpath:bpm-skyrim-mq-2.xml"
	})
public class MessageManagerTest {

	@Resource
	private MessageProductorService messageProductorService;
	@Resource
	private MessageConsumerService messageConsumer;
	
	@Test
	public void testMessageQueue(){
		messageProductorService.pushToMessageQueue("rabbit_queue_one", "hello giraffe");
//		messageProductorService.popMessage("rabbit_queue_one");
	}
	
}
