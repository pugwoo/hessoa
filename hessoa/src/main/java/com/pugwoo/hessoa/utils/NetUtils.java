package com.pugwoo.hessoa.utils;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetUtils {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(NetUtils.class);

	/**
	 * 获得本机的ip，只查询ipv4, 不要127.0.0.1
	 * @return
	 */
	public static List<String> getThisMathineIps() {
		try {
			String hostname = InetAddress.getLocalHost().getHostName();
			InetAddress[] addresses = InetAddress.getAllByName(hostname);

			List<String> ips = new ArrayList<>();
			Pattern ipv4Pattern = Pattern.compile("^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$");
			for (InetAddress addr : addresses) {
				String host = addr.getHostAddress();
				if(host != null && ipv4Pattern.matcher(host).find()) {
					ips.add(host);
				}
			}
			
			return ips;
		} catch (Exception e) {
			LOGGER.error("getThisMathineIps fail", e);
			return new ArrayList<>();
		}
	}
	
}
