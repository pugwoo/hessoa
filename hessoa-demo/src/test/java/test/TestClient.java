package test;

import com.pugwoo.hessoa.client.SOAClient;

import spring_remote.api.service.IUserService;

public class TestClient {

	public static void main(String[] args) throws Exception {
		for(int i = 0; i < 10; i++) {
			// 手工获得服务的引用
			long start = System.currentTimeMillis();
			IUserService userService = SOAClient.getService(IUserService.class,
					"http://192.168.1.102:8080/hessoa-demo/_remote/userService");
			long end = System.currentTimeMillis();
			System.out.println("获取服务耗时:" + (end - start) + "ms");
			
			start = System.currentTimeMillis();
			String info = userService.getInfo();
			System.out.println(info);
			end = System.currentTimeMillis();
			System.out.println("调用服务耗时:" + (end - start) + "ms");
		}

		// Thread.sleep(300000); // 观察SOAClient
	}
	
}
