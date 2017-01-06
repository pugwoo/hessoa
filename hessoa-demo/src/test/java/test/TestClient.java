package test;

import com.caucho.services.server.ServiceContext;
import com.pugwoo.hessoa.client.SOAClient;

import com.pugwoo.hessoa.context.HessianHeaderContext;
import org.springframework.util.ObjectUtils;
import spring_remote.api.service.IUserService;

import java.util.UUID;

public class TestClient {

	public static void main(String[] args) throws Exception {

		for(int i = 0; i < 10; i++) {
			// 自动获得服务的引用
			long start = System.currentTimeMillis();
			IUserService userService = SOAClient.getService(IUserService.class);
			
			// 指定绝对地址的方式，用于调试
//			IUserService userService = SOAClient.getService(IUserService.class,
//					"http://127.0.0.1:8080/hessoa-demo/_hessoa/userServiceImpl");
			long end = System.currentTimeMillis();
			System.out.println(userService.toString());
			System.out.println("获取服务耗时:" + (end - start) + "ms");
			start = System.currentTimeMillis();
			HessianHeaderContext context = HessianHeaderContext.getContext();
			context.addHeader("token", UUID.randomUUID().toString());
			String token = userService.getToken();
			System.out.println(token);
			end = System.currentTimeMillis();
			System.out.println("调用服务耗时:" + (end - start) + "ms");
		}
		// Thread.sleep(300000); // 观察SOAClient
	}
	
}
