package com.pugwoo.hessoa.test.api.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.pugwoo.hessoa.test.api.entity.UserDO;
import com.pugwoo.hessoa.test.api.entity.UserTypeEnum;

/**
 * 2012年11月20日 16:55:58
 */
public interface IUserService {
	
	String getInfo();
	
	String sayHello(String name);
	
	int sayInt(int n);
	
	// 重要：不推荐在SOA接口中使用枚举，当客户端和服务端枚举值对应不上时，会抛序列化异常
	UserTypeEnum sayEnum(UserTypeEnum type);
	
	UserDO getUser();
	
	String insert(UserDO userDO);
	
	String insert(List<UserDO> userDOs);
	
	String insert(Map<Object, UserDO> userMap);
	
	String insert(Set<UserDO> userDOs);
	
}
