package test;

import java.util.ArrayList;
import java.util.List;

import com.pugwoo.hessoa.client.SOAClient;
import com.pugwoo.hessoa.test.api.service.IUserService;

public class TestBenchmarkMultiThread {

	public static void main(String[] args) throws Exception {
		final int total = 100000;
		int threadNum = 10;
		
		long start = System.currentTimeMillis();
		List<Thread> threads = new ArrayList<>();
		for(int t = 0; t < threadNum; t++) {
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					
					for(int i = 0; i < total; i++) {
						IUserService userService = SOAClient.getService(IUserService.class);
						userService.sayHello("nick");
					}
				}
			});
			threads.add(thread);
			thread.start();
		}
		
		for(Thread thread : threads) {
			thread.join();
		}
		long end = System.currentTimeMillis();
		
		// 2017年主流台式机，10条线程100万次调用，QPS: 7634次每秒，成功率100%，此时CPU跑满
		System.out.println("QPS:" + (total * threadNum * 1.0 / ((end - start) / 1000)));
	}
	
}
