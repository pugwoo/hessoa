package com.pugwoo.hessoa.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetUtils {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(NetUtils.class);
	
	/**
	 * 获得本机的ipv4的所有ip列表，返回的是网卡别称 -> ip 的map <br>
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
