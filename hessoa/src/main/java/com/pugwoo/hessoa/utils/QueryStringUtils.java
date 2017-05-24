package com.pugwoo.hessoa.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * parse and format map to querystring
 * 
 * 不支持查询参数中有相同name的情况，如果有，后面的会覆盖前面的
 * @author pugwoo
 */
public class QueryStringUtils {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(QueryStringUtils.class);

	/**
	 * 将object转换成 query String
	 * @param object
	 * @return will not return null
	 */
	public static String format(Map<String, String> object) {
		if(object == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		boolean isFirst = true;
		for(Entry<String, String> entry : object.entrySet()) {
			if(!isFirst) {
				sb.append("&");
			} else {
				isFirst = false;
			}
			String key = entry.getKey() == null ? "" : entry.getKey();
			String value = entry.getValue() == null ? "" : entry.getValue();
			try {
				sb.append(URLEncoder.encode(key, "UTF-8"));
				sb.append("=");
				sb.append(URLEncoder.encode(value, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				LOGGER.error("encode key:{} value:{} error", key, value, e);
			}
		}
		return sb.toString();
	}
	
	/**
	 * queryString转换成map
	 * @param queryString
	 * @return
	 */
	public static Map<String, String> parse(String queryString) {
		Map<String, String> map = new HashMap<String, String>();
		if(queryString == null || queryString.trim().isEmpty()) {
			return map;
		}
		String[] pairs = queryString.split("&");
		for(String pair : pairs) {
			if(pair == null || pair.trim().isEmpty()) {
				continue;
			}
			int index = pair.indexOf("=");
			try {
				if(index < 0) {
					map.put(URLDecoder.decode(pair, "UTF-8"), "");
				} else {
					map.put(URLDecoder.decode(pair.substring(0, index), "UTF-8"),
							URLDecoder.decode(pair.substring(index + 1), "UTF-8"));
				}
			} catch (UnsupportedEncodingException e) {
				LOGGER.error("encode string:{} error", pair, e);
			}
		}
		
		return map;
	}
	
}
