package com.pugwoo.hessoa.utils;

public interface Constants {

	/**服务器和SOAClient客户端，都会自动加上前缀url，以方便运维上隔离内网外网*/
	String HESSOA_URL_PREFIX = "_hessoa";

	/**存儲在redis服务中心的key值前缀*/
	String HESSOA_REDIS_PREFIX = "HESSOA-";

	/**redis配置文件名称*/
	String HESSOA_REIDS_PROPERTIES_FILE = "/hessoa-redis-${env}.properties";
	
	String HESSOA_CONTEXT_HEADER_PREFIX = "_hsoa_";
	
	String HESSOA_CONTEXT_USER_PREFIX = "user_";
}
