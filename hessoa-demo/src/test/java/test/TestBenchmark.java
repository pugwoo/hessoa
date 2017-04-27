package test;

import com.pugwoo.hessoa.client.SOAClient;
import com.pugwoo.hessoa.test.api.service.IUserService;

public class TestBenchmark {

	public static void main(String[] args) throws Exception {
		
		int total = 100000;
		
		// 自动获得服务的引用
		long start = System.currentTimeMillis();
		
		for(int i = 0; i < total; i++) {
			IUserService userService = SOAClient.getService(IUserService.class);
			userService.sayHello("nick");
		}
		
		long end = System.currentTimeMillis();
		
		// 2017年主流台式机，单线程QPS: 2326次每秒
		System.out.println("QPS:" + (total * 1.0 / ((end - start) / 1000)));
	}
	
}
