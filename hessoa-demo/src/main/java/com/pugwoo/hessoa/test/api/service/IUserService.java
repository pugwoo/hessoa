package com.pugwoo.hessoa.test.api.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.pugwoo.hessoa.test.api.entity.UserDO;

/**
 * 2012年11月20日 16:55:58
 */
public interface IUserService {
	
	String getInfo();
	
	String sayHello(String name);
	
	UserDO getUser();
	
	String insert(UserDO userDO);
	
	String insert(List<UserDO> userDOs);
	
	String insert(Map<Object, UserDO> userMap);
	
	String insert(Set<UserDO> userDOs);
	
}
