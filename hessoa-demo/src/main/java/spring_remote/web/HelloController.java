package spring_remote.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.pugwoo.hessoa.client.SOAClient;

import spring_remote.api.service.IUserService;

@Controller
public class HelloController {

	@ResponseBody
	@RequestMapping("/hello")
	public String hello() {
		return "hello";
	}
	
	@ResponseBody
	@RequestMapping("/remote")
	public String getRemote() {
		StringBuilder sb = new StringBuilder();
		
		long start = System.currentTimeMillis();
		IUserService userService = SOAClient.getService(IUserService.class);
		long end = System.currentTimeMillis();
		
		sb.append("getService cost:" + (end - start) + "ms,");
		
		start = System.currentTimeMillis();
		String info = userService.getInfo();
		end = System.currentTimeMillis();
		sb.append("call service cost:" + (end - start) + "ms, info:" + info);
		
		return sb.toString();
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
