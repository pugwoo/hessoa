package com.pugwoo.hessoa.context;

import java.util.HashMap;
import java.util.Map;

/**
 * 读写hessian http header，凡是由HessianHeaderContext放的头部一律加上_hessoa前缀。
 * HessianHeaderContext会一直带上上一个调用的实时Context。
 * 
 * 这是一个底层实现，业务方请使用SOACallContext来传递上下文。
 * 
 * @author markflyfly pugwoo 2017年1月6日
 */
public class HessianHeaderContext {

	/**
	 * 2017年5月19日 15:38:04
	 * 为解决hessian将头部都变成小写的问题，这里将头部数据格式修改成Map<String, Map<String, String>>
	 * 
	 * 【特别注意】namespace是hessoa自行管理的，只能用小写!
	 */
	private static final ThreadLocal<Map<String, Map<String, String>>> context =
			new ThreadLocal<Map<String, Map<String, String>>>();
	
	private HessianHeaderContext() {}
	
	public static void add(String namespace, String key, String value) {
		get(namespace).put(key, value);
	}
	
	public static void set(String namespace, Map<String, String> map) {
		get().put(namespace, map);
	}
	
	public static Map<String, Map<String, String>> get() {
		Map<String, Map<String, String>> map = context.get();
		if(map == null) {
			map = new HashMap<String, Map<String, String>>();
			context.set(map);
		}
		return map;
	}
	
	public static Map<String, String> get(String namespace) {
		Map<String, Map<String, String>> map = get();
		Map<String, String> m = map.get(namespace);
		if(m == null) {
			m = new HashMap<String, String>();
			map.put(namespace, m);
		}
		return m;
	}
	
	/**
	 * 不存在将返回null
	 * @param key
	 * @return
	 */
	public static String get(String namespace, String key) {
		return get(namespace).get(key);
	}
	
	/**
	 * 清理当前线程的上下文
	 */
	public static void clear() {
		context.set(null);
	}
}
