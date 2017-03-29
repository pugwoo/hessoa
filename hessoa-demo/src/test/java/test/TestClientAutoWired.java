package test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import spring_remote.api.service.IUserService;

@ContextConfiguration(locations = "classpath:service-client-spring-context.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class TestClientAutoWired {

	@Autowired
	private IUserService userService;
	
	@Test
	public void test() {
		System.out.println(userService);
		
		String result = userService.sayHello("nick");
		System.out.println(result);
	}
	
}
