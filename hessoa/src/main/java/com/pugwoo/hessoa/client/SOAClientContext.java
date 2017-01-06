package com.pugwoo.hessoa.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.pugwoo.hessoa.context.HessianHeaderContext;
import com.pugwoo.hessoa.utils.Constants;

/**
 * 提供给用户端使用的上下文contxt
 * @author nick
 */
public class SOAClientContext {

	/**
	 * 拿用户上下文数据，注意，对返回值的map的修改不会影响Context，请使用add来写
	 * @return
	 */
	public static Map<String, String> get() {
		Map<String, String> result = new HashMap<String, String>();
		Map<String, String> map = HessianHeaderContext.get();
		for(Entry<String, String> entry : map.entrySet()) {
			String key = entry.getKey();
			if(key != null && key.startsWith(Constants.HESSOA_CONTEXT_USER_PREFIX)) {
				result.put(key.substring(Constants.HESSOA_CONTEXT_USER_PREFIX.length()),
						entry.getValue());
			}
		}
		return result;
	}
	
	public static String get(String key) {
		return HessianHeaderContext.get(Constants.HESSOA_CONTEXT_USER_PREFIX + key);
	}
	
	public static void add(String key, String value) {
		HessianHeaderContext.add(Constants.HESSOA_CONTEXT_USER_PREFIX + key, value);
	}
	
}
