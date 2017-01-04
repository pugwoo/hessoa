package com.pugwoo.hessoa.utils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetUtils {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(NetUtils.class);
	
	public static boolean isIpLAN(String ip) {
		for(int i = 0; i <= 3; i++) {
			if(isIpInRange(ip, i)) {
				return true;
			}
		}
		return false;
	}
	
	/**判断ipv4是否在某类局域网中，range=0本地，range=1为A类，range=2为B类, range=3为C类*/
	public static boolean isIpInRange(String ip, int range) {
		if(ip == null) {
			return false;
		}
		if(range == 0) {
			return ip.startsWith("127.");
		} else if(range == 1) {
			return ip.startsWith("10.");
		} else if(range == 2) {
			if(ip.startsWith("172.")) {
				String strs[] = ip.split("\\.");
				if(strs.length == 4) {
					int second = Integer.valueOf(strs[1]);
					if(second >= 16 && second <= 31) {
						return true;
					}
				}
			}
			return false;
		} else if (range == 3) {
			return ip.startsWith("192.168.");
		}
		
		return false;
	}
	
	/**
	 * 检查链接是否可以联通, url支持各种不规范的写法，例如http://或https://开头等。
	 * 超时配置取自Configs的超时配置
	 */
	public static boolean checkUrlAlive(String url) {
		if(url == null) {
			return false;
		}
		
		int defaultPort = 80;
		if(url.startsWith("http://")) {
			url = url.substring("http://".length());
			defaultPort = 80;
		} else if (url.startsWith("https://")) {
			url = url.substring("https://".length());
			defaultPort = 443;
		}
		
		int index = url.indexOf("/");
		if(index >= 0) {
			url = url.substring(0, index);
		}
		
		index = url.indexOf(":");
		String ip = null;
		int port = 0;
		if(index > 0) {
			ip = url.substring(0, index);
			port = Integer.valueOf(url.substring(index + 1));
		} else {
			ip = url;
			port = defaultPort;
		}
		
		Socket client = null;
		try {
			client = new Socket();
			client.connect(new InetSocketAddress(ip, port), Configs.getNetworkCheckTimeout());
			return true;
		} catch (Throwable e) {
			return false;
		} finally {
			if(client != null) {
				try {
					client.close();
				} catch (Throwable e) {
					
				}
			}
		}
	}
	
	/**
	 * 获得本机的ipv4的所有ip列表，返回的是网卡别称  到 ip 的map <br>
	 * 排除本机ip 127.开头的
	 * @return
	 */
	public static List<String> getIpv4IPs() {
		List<String> ips = new ArrayList<>();
		String regex = "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
		Pattern pattern = Pattern.compile(regex);
		try {
			for (Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
					ifaces.hasMoreElements();) {
				NetworkInterface iface = ifaces.nextElement();
				for (Enumeration<InetAddress> addresses = iface.getInetAddresses(); addresses.hasMoreElements();) {
					InetAddress address = addresses.nextElement();
					if(pattern.matcher(address.getHostAddress()).find() && !address.getHostAddress().startsWith("127.")) {
						ips.add(address.getHostAddress());
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("getIpv4IPs fail", e);
		}

		return ips;
	}
	
}
