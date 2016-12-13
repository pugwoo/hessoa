package com.pugwoo.hessoa.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 拿base-$env.properties定义的参数值
 * @author nick
 */
public class Configs {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Configs.class);

	private static Properties prop = new Properties();
	
	// 常用的配置项
	
	/**拿redis的host，没有配置返回null*/
	public static String getRedisHost() {
		return prop.getProperty("redis.host");
	}
	
	/**拿到redis的port，没有配置返回null*/
	public static String getRedisPort() {
		return prop.getProperty("redis.port");
	}
	
	/**拿到redis的密码，拿不到返回null*/
	public static String getRedisPassword() {
		return prop.getProperty("redis.password");
	}
	
	/**拿到配置的key的前缀，拿不到返回空字符串*/
	public static String getRedisKeyPrefix() {
		String redisKeyPrefix = prop.getProperty("redis.key.prefix");
		if(redisKeyPrefix == null) {
			return null;
		}
		return "";
	}
	
	/**获得是否优先从本地拿服务，默认false，本地调试可以设置为true。当它为true时，它会一直从本机拿，100%概率*/
	public static boolean getNetworkUseLocal() {
		String networkLocal = prop.getProperty("network.uselocal");
		if(networkLocal == null || networkLocal.trim().isEmpty()) {
			return false;
		}
		return Boolean.valueOf(networkLocal);
	}
	
	/**拿到是否优先访问外网outer或内网inner的配置，默认没有这样的倾向，返回空字符串*/
	public static String getNetworkPrefer() {
		String networkPrefer = prop.getProperty("network.prefer");
		if(networkPrefer == null) {
			networkPrefer = "";
		}
		return networkPrefer;
	}
	
	/**拿测试网络是否通的超时时间，默认1000毫秒*/
	public static int getNetworkCheckTimeout() {
		String networkCheckTimeout = prop.getProperty("network.check.timeout");
		if(networkCheckTimeout == null || networkCheckTimeout.trim().isEmpty()) {
			return 1000;
		}
		return Integer.valueOf(networkCheckTimeout);
	}
	
	static {
		String env = System.getProperty("env");
		if(env == null || env.trim().isEmpty()) {
			env = "${env}";
		}
		String fileName = Constants.HESSOA_REIDS_PROPERTIES_FILE.replace("${env}", env);
		
		LOGGER.info("load base config file:{}", fileName);
		
		InputStream in = RedisUtils.class.getResourceAsStream(fileName);
		
		if(in != null) {
			try {
				prop.load(in);
			} catch (IOException e) {
				LOGGER.error("prop load exception", e);
			}
		} else {
			LOGGER.info("redis conf file {} not found, ignore.", 
					Constants.HESSOA_REIDS_PROPERTIES_FILE);
		}
	}
	
	/**
	 * 拿系统配置，会自动根据环境来拿
	 * @param key
	 * @return null if not found
	 */
	public static String getConfig(String key) {
		return prop.getProperty(key);
	}

}
