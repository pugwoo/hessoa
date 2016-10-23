package spring_remote.dao;

import org.springframework.stereotype.Component;

@Component
public class UserDAOImpl implements IUserDAO {

	@Override
	public String sayHello(String name) {
		return "hello: " + name;
	}

}
