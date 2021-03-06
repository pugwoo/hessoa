package test;

import com.pugwoo.hessoa.client.SOAClient;
import com.pugwoo.hessoa.client.SOAClientContext;
import com.pugwoo.hessoa.test.api.entity.UserTypeEnum;
import com.pugwoo.hessoa.test.api.service.IUserService;

public class TestClient {

	public static void main(String[] args) throws Exception {
		for(int i = 0; i < 10; i++) {
			
			// 自动获得服务的引用
			long start = System.currentTimeMillis();
			IUserService userService = SOAClient.getService(IUserService.class);
			
			// 放些上下文
			SOAClientContext.add("loginUserId", "3");
			SOAClientContext.add("loginUserName名称", "nick你好");
			
			// 指定绝对地址的方式，方便用于调试指向到哪台机器上
//			IUserService userService = SOAClient.getService(IUserService.class,
//					"http://127.0.0.1:8080/hessoa-demo/_hessoa/userServiceImpl"); 
			long end = System.currentTimeMillis();
			System.out.println(userService.toString());
			System.out.println("获取服务耗时:" + (end - start) + "ms");
			
			start = System.currentTimeMillis();
			String info = userService.getInfo();
			System.out.println(info);
			end = System.currentTimeMillis();
			System.out.println("调用服务耗时:" + (end - start) + "ms");
		}

		IUserService userService = SOAClient.getService(IUserService.class);
		
		Integer a  = 3;
		int b = 4;
		System.out.println(userService.sayInt(a));
		System.out.println(userService.sayInt(b));
		System.out.println(userService.sayInt(5));
		
		
		System.out.println(userService.sayEnum(UserTypeEnum.VIP));
		// Thread.sleep(300000); // 观察SOAClient
	}
	
}
