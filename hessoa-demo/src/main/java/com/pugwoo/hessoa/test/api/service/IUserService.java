package com.pugwoo.hessoa.test.api.service;

import com.pugwoo.hessoa.test.api.entity.UserDO;

/**
 * 2012年11月20日 16:55:58
 */
public interface IUserService {
	
	public String getInfo();
	
	public String sayHello(String name);
	
	public UserDO getUser();
	
}
