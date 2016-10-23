package spring_remote.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HelloController {

	@ResponseBody
	@RequestMapping("/hello")
	public String hello() {
		return "hello";
	}
	
	/**
	 * 测试虽然名称和soa相同，但是是对应到不同的链接中
	 * @return
	 */
	@ResponseBody
	@RequestMapping("/userService")
	public String testSameNameToSOA() {
		return "userService";
	}
	
}
