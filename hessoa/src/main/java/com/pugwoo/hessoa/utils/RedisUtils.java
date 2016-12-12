package com.pugwoo.hessoa.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

/**
 * 2016年5月28日 11:10:20
 * @author pugwoo
 *
 * Jedis实例不是线程安全的，所以new Jedis()只能给当前线程用。
 * 但是不断地new Jedis()是不合适的，推荐的做法就是用JedisPool。
 */
public class RedisUtils {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(RedisUtils.class);
	
	private static JedisPool pool = null;
	
	private static String redisKeyPrefix = "";
	
	static {
		String env = System.getProperty("env");
		if(env == null || env.trim().isEmpty()) {
			env = "${env}";
		}
		
		InputStream in = RedisUtils.class.getResourceAsStream
				(Constants.HESSOA_REIDS_PROPERTIES_FILE.replace("${env}", env));
		if(in != null) {
			Properties properties = new Properties();
			try {
				properties.load(in);
			} catch (IOException e) {
				LOGGER.error("properties {} load exception", 
						Constants.HESSOA_REIDS_PROPERTIES_FILE, e);
			}
			
			String host = properties.getProperty("redis.host");
			String port = properties.getProperty("redis.port");
			String passwd = properties.getProperty("redis.password");
			
			redisKeyPrefix = properties.getProperty("redis.key.prefix");
			if(redisKeyPrefix == null) {
				redisKeyPrefix = "";
			}
			
			if(host != null && !host.isEmpty() && port != null && !port.isEmpty()) {
				
				JedisPoolConfig poolConfig = new JedisPoolConfig();
				poolConfig.setMaxTotal(128); // 最大连接数
				poolConfig.setMaxIdle(64);
				poolConfig.setMaxWaitMillis(1000L);
				poolConfig.setTestOnBorrow(false);
				poolConfig.setTestOnReturn(false);
				
				if(passwd == null || passwd.trim().isEmpty()) {
					pool = new JedisPool(poolConfig, host, Integer.valueOf(port),
							Protocol.DEFAULT_TIMEOUT);
				} else {
					pool = new JedisPool(poolConfig, host, Integer.valueOf(port),
							Protocol.DEFAULT_TIMEOUT, passwd);
				}
			} else {
				LOGGER.warn("redis properties {} must have redis.host and redis.port",
						Constants.HESSOA_REIDS_PROPERTIES_FILE);
			}
		} else {
			LOGGER.info("redis conf file {} not found, ignore.", 
					Constants.HESSOA_REIDS_PROPERTIES_FILE);;
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
		if(pool != null && !pool.isClosed()) {
			pool.close();
		}
		super.finalize();
	}
	
	/**
	 * 拿Jedis连接，用完Jedis之后【必须】close jedis，这个非常重要. <br>
	 * 如果没有配置redis或redis服务不可用，返回null
	 */
	private static Jedis getJedisConnection() {
		if(pool == null) {
			return null;
		}
		try {
			return pool.getResource();
		} catch (Exception e) {
			LOGGER.error("getJedisConnection fail", e);
			return null;
		}
	}
	
	public static boolean isConfigRedis() {
		return pool != null;
	}
	
	/**
	 * 将url加入到redis中
	 * @param interfaceName
	 * @param urls
	 * @return
	 */
	public static boolean addUrl(String interfaceName, List<String> urls, int expireSeconds) {
		Jedis jedis = getJedisConnection();
		if(jedis == null) {
			return false;
		}
		
		for (String url : urls) {
			try {
				URL _url = new URL(url);
				String key = Constants.HESSOA_REDIS_PREFIX
						+ (redisKeyPrefix.isEmpty() ? redisKeyPrefix : redisKeyPrefix + "-") 
						+ interfaceName + "-" + _url.getHost() + "-" 
						+ (_url.getPort() > 0 ? _url.getPort() : _url.getDefaultPort());
				// 说明一下，这里之所以加上ip-port，是因为redis没有办法为子元素设置超时时间，所以就抽上来一层
				jedis.setex(key, expireSeconds, url);
			} catch (MalformedURLException e) {
				LOGGER.error("url:{} MalformedURLException", url, e);
			}
		}
		
		jedis.close();
		return true;
	}
	
	/**
	 * 从redis中拿接口的url列表
	 * @param interfaceName
	 * @return 不会返回null
	 */
	public static List<String> getURLs(String interfaceName) {
		Jedis jedis = getJedisConnection();
		if(jedis == null) {
			return new ArrayList<String>();
		}
		
		Set<String> keys = jedis.keys(Constants.HESSOA_REDIS_PREFIX
				+ (redisKeyPrefix.isEmpty() ? redisKeyPrefix : redisKeyPrefix + "-") 
				+ interfaceName + "-*");
		
		List<String> urls = new ArrayList<String>();
		for(String key : keys) {
			String value = jedis.get(key);
			if(value != null && !value.isEmpty()) {
				urls.add(value);
			}
		}
		
		jedis.close();
		return urls;
	}
	
}
