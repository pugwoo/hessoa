package com.pugwoo.hessoa.client;

import java.util.Map;

import com.pugwoo.hessoa.context.HessianHeaderContext;
import com.pugwoo.hessoa.utils.Constants;

/**
 * 提供给用户端使用的上下文contxt。
 * 说明：tomcat的http头部最大是8K字节，除了系统会占用一些头部信息，留给用户自定义的头部，建议不要超过4k。
 *      头部字节过大会加大网络传输量，影响调用性能。
 * @author nick
 */
public class SOAClientContext {

	/**
	 * 拿用户上下文数据，注意，对返回值的map的修改不会影响Context，请使用add来写
	 * @return
	 */
	public static Map<String, String> get() {
		return HessianHeaderContext.get(Constants.HESSOA_CONTEXT_HEADER_USER);
	}
	
	public static String get(String key) {
		return HessianHeaderContext.get(Constants.HESSOA_CONTEXT_HEADER_USER, key);
	}
	
	public static void add(String key, String value) {
		HessianHeaderContext.add(Constants.HESSOA_CONTEXT_HEADER_USER, key, value);
	}
	
}
