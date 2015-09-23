package me.ele.bpm.skyrim.test.base;


import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({
		"classpath:bpm-skyrim-mq-2.xml"
	})
public class TestBase {
	
}
