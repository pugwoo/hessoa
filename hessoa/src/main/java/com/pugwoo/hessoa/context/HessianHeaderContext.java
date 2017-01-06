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

	private static final ThreadLocal<Map<String, String>> context =
			new ThreadLocal<Map<String, String>>();
	
	private HessianHeaderContext() {}
	
	public static void add(String key, String value) {
		get().put(key, value);
	}
	
	public static Map<String, String> get() {
		Map<String, String> map = context.get();
		if(map == null) {
			map = new HashMap<String, String>();
			context.set(map);
		}
		return map;
	}
	
	/**
	 * 不存在将返回null
	 * @param key
	 * @return
	 */
	public static String get(String key) {
		return get().get(key);
	}
	
	/**
	 * 清理当前线程的上下文
	 */
	public static void clear() {
		context.set(null);
	}
}
