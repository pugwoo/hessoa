package spring_remote.api.service;

import spring_remote.api.entity.UserDO;

/**
 * 2012年11月20日 16:55:58
 */
public interface IUserService {
	
	public String getInfo();
	
	public String sayHello(String name);
	
	public UserDO getUser();
	
}
