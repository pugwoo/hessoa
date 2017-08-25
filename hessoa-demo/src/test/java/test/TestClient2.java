package test;

import java.util.HashMap;
import java.util.Map;

import com.pugwoo.hessoa.client.SOAClient;
import com.pugwoo.hessoa.test.api.entity.UserDO;
import com.pugwoo.hessoa.test.api.service.IUserService;

import test.dto.UserVO;

public class TestClient2 {
	
	public static void main(String[] args) {
		IUserService userService = SOAClient.getService(IUserService.class);
		
		UserVO userVO = new UserVO();
		userVO.setName("nick");
		userVO.setScore(99);
		userVO.setMything("mmm");
		
		Map<Object, UserDO> map = new HashMap<Object, UserDO>();
		map.put("nick", userVO);
		map.put(1, userVO);
		
		String result = userService.insert(map);
		
		System.out.println(result);
	}
	
}
