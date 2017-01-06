package spring_remote.service;

import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pugwoo.hessoa.annotation.HessianService;
import com.pugwoo.hessoa.client.SOAClientContext;

import spring_remote.api.entity.UserDO;
import spring_remote.api.service.IUserService;
import spring_remote.dao.IUserDAO;

/**
 * 2012年11月20日 16:59:09
 */
@Service
@HessianService(IUserService.class)
public class UserServiceImpl implements IUserService{
	
	@Autowired
	private IUserDAO userDAO;

	public String getInfo() {
		
		System.out.println("============从客户端拿到的context数据:============");
		Map<String, String> context = SOAClientContext.get();
		for(Entry<String, String> entry : context.entrySet()) {
			System.out.println(entry.getKey() + ":" + entry.getValue());
		}
		System.out.println("====================END=========================");
		
		return "This is spring remote service";
	}

	public String sayHello(String name) {
		return userDAO.sayHello(name);
	}

	public UserDO getUser() {
		UserDO user = new UserDO();
		user.setName("nick");
		user.setScore(99);

		return user;
	}

}
