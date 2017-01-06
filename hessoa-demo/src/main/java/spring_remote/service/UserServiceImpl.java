package spring_remote.service;

import com.pugwoo.hessoa.annotation.HessianService;
import com.pugwoo.hessoa.client.SOAClient;
import com.pugwoo.hessoa.context.HessianHeaderContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import spring_remote.api.entity.UserDO;
import spring_remote.api.service.IUserService;
import spring_remote.dao.IUserDAO;

/**
 * 2012年11月20日 16:59:09
 */
@Service
@HessianService
public class UserServiceImpl implements IUserService{
	
	@Autowired
	private IUserDAO userDAO;



	public String getInfo() {
		return "username";
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

	@Override
	public String getToken() {
		HessianHeaderContext context = HessianHeaderContext.getContext();
		String token = context.getHeader("token");
		return token;
	}

}
