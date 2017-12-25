package test;

import com.pugwoo.hessoa.context.HessianProxyFactory;
import com.pugwoo.hessoa.test.api.service.IUserService;

/**
 * 2017年12月25日 17:30:31
 * 这个是直接拿到url进行测试性能，理论上测出来的就是hessian本身的性能
 */
public class TestBenchmarkDirect {

	public static void main(String[] args) throws Exception {
		
		int total = 100000;
		
		// 自动获得服务的引用
		long start = System.currentTimeMillis();
		
		HessianProxyFactory factory = new HessianProxyFactory();
		factory.setOverloadEnabled(true); 
		
		IUserService userService = (IUserService) factory.create(IUserService.class, 
				"http://192.168.0.99:8080/hessoa-demo/_hessoa/com.pugwoo.hessoa.test.service.UserServiceImpl");
		
		for(int i = 0; i < total; i++) {
			userService.sayHello("nick");
		}
		
		long end = System.currentTimeMillis();
		
		System.out.println("QPS:" + (total * 1.0 / ((end - start) / 1000)));
	}
	
}
