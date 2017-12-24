package test;

import com.pugwoo.hessoa.utils.NetUtils;

public class TestCheckUrl {

	public static void main(String[] args) {
		String url = "http://192.168.0.99:8080/hessoa-demo/_hessoa/com.pugwoo.hessoa.test.service.UserServiceImpl";
		
		System.out.println(NetUtils.checkUrlAlive(NetUtils.parseIpPort(url)));
		
		long start = System.currentTimeMillis();
		for(int i = 0; i < 10000; i++) {
			NetUtils.checkUrlAlive(NetUtils.parseIpPort(url)); // 每次检测0.6ms，有点慢
		}
		long end = System.currentTimeMillis();
		
		System.out.println(end - start);
	}
}
