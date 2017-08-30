package com.pugwoo.hessoa.test.service;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pugwoo.hessoa.annotation.HessianService;
import com.pugwoo.hessoa.client.SOAClientContext;
import com.pugwoo.hessoa.test.api.entity.UserDO;
import com.pugwoo.hessoa.test.api.service.IUserService;
import com.pugwoo.hessoa.test.dao.IUserDAO;

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
	
	@Override
	public int sayInt(int n) {
		System.out.println("say int:" + n);
		return n;
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
	public String insert(UserDO userDO) {
		System.out.println(userDO);
		return "ok";
	}

	@Override
	public String insert(List<UserDO> userDOs) {
		System.out.println(userDOs);
		return "ok";
	}

	@Override
	public String insert(Map<Object, UserDO> userMap) {
		System.out.println(userMap);
		return "ok";
	}

	@Override
	public String insert(Set<UserDO> userDOs) {
		System.out.println(userDOs);
		return "ok";
	}
	
}
