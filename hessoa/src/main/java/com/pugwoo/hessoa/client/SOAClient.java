package com.pugwoo.hessoa.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.caucho.hessian.client.HessianProxyFactory;

/**
 * 拿到soa的具体url，这是一种手工的方式。
 * 
 * class到ip:port的映射关系，写在机器的文件上，格式为：
 * com.trueme.xxx  127.0.0.1:8080/order/_remote
 * com.trueme.xxx  127.1.2.3:8081/order/_remote
 * 
 * 配置文件每1秒读取更新一次。
 * 
 * 匹配package如果有多个时，以最细的package更优先为原则。例如认为com.abc.order.list比com.abc.order优先级高。
 * 
 * 机器查找路径为：confPaths写定的
 * 
 * @author pugwoo
 */
public class SOAClient {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SOAClient.class);
	
	/**需要扫描的配置文件，按顺序直到找到第一个为止*/
	private static final String[] confPaths = new String[] {
			"/etc/soa_host",
			"/root/soa_host",
			"C:/soa_host.txt",
			"D:/soa_host.txt"};
		
	private static Map<String, List<String>> pkgToHosts = new HashMap<String, List<String>>();
	private static Map<String, List<String>> livePkgToHosts = new HashMap<String, List<String>>();
	
	private static long fileLastModified = 0;
	private static File hostFile = null;
	
	private static synchronized void updatePkgToHosts() {
		if(hostFile == null) {
			hostFile = getExistFile();
			if(hostFile == null) {return;}
		}
		if(!hostFile.exists()) {
			hostFile = null;
			return;
		}
		if(hostFile.lastModified() == fileLastModified) {
			return;
		}
		
		try {
			Map<String, List<String>> map = new HashMap<String, List<String>>();
			BufferedReader br = new BufferedReader(new FileReader(hostFile));
			String line = null;
			while((line = br.readLine()) != null) {
				// 去掉注释，以#开头，不支持转义#
				int index = line.indexOf("#");
				if(index >= 0) {
					line = line.substring(0, index);
				}
				
				String strs[] = line.split("\\s+");
				if(strs.length < 2) {
					continue;
				}
				String pkg = strs[0].trim();
				String host = strs[1].trim();
				if(!pkg.isEmpty() && !host.isEmpty()) {
					if(!map.containsKey(pkg)) {
						map.put(pkg, new ArrayList<String>());
					}
					if(!map.get(pkg).contains(host)) {
						map.get(pkg).add(host);
					}
				}
			}
			br.close();
			pkgToHosts = map;
			fileLastModified = hostFile.lastModified();
		} catch (IOException e) {
		}
	}
	
	static {
		updatePkgToHosts();
		livePkgToHosts = pkgToHosts;
		
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						updatePkgToHosts();
						Thread.sleep(1000);
					} catch (Exception e) {
						LOGGER.error("updatePkgToHosts fail", e);
					}				
				}
			}
		});
		thread.setName("SOA-updatePkgToHosts-thread");
		thread.setDaemon(true);
		thread.start();
		
		Thread thread2 = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						checkHostsLive();
						Thread.sleep(1000);
					} catch (Exception e) {
						LOGGER.error("checkHostsLive fail", e);
					}				
				}
			}
		});
		thread2.setName("SOA-checkHostsLive-thread");
		thread2.setDaemon(true);
		thread2.start();
	}
	
	/**
	 * 检查服务器是否存活
	 */
	private static synchronized void checkHostsLive() {
		Map<String, List<String>> _livePkgToHosts = new HashMap<String, List<String>>();
		
		for(Entry<String, List<String>> entry : pkgToHosts.entrySet()) {
			List<String> lives = new ArrayList<String>();
			for(String str : entry.getValue()) {
				String current = str;
				if(str.startsWith("http://")) {
					str = str.substring("http://".length());
				} else if (str.startsWith("https://")) {
					str = str.substring("https://".length());
				}
				int index = str.indexOf("/");
				if(index >= 0) {
					str = str.substring(0, index);
				}
				index = str.indexOf(":");
				String ip = null, port = null;
				if(index > 0) {
					ip = str.substring(0, index);
					port = str.substring(index + 1);
				} else {
					ip = str;
					port = "80";
				}
				boolean isLive = checkPort(ip, port);
				
				if(isLive) {
					lives.add(current);
				} else {
					LOGGER.warn("host {} is inavaliable", current);
				}
			}
			
			if(lives.isEmpty()) {
				LOGGER.error("package {} has no avaliable hosts", entry.getKey());
			}
			_livePkgToHosts.put(entry.getKey(), lives);
		}
		
		livePkgToHosts = _livePkgToHosts;
	}
	
	/**
	 * 检查端口是否可用，超时是1s
	 * @param ip
	 * @param port
	 * @return
	 */
	private static boolean checkPort(String ip, String port) {
		Socket client = null;
		try {
			client = new Socket(ip, Integer.valueOf(port)); // 默认超时是1s，可以改的，但1s合理
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
		
	private static File getExistFile() {
		if(confPaths == null) {
			return null;
		}
		for(String filePath : confPaths) {
			File file = new File(filePath);
			if(file.exists() && file.isFile()) {
				return file;
			}
		}
		return null;
	}

	/**
	 * 获得类名对应的服务器列表
	 * @param clazz
	 * @return 查找不存在时返回null
	 */
	private static List<String> getHostByClass(Class<?> clazz) {
		String matchedKey = null;
		for(Entry<String, List<String>> entry : livePkgToHosts.entrySet()) {
			if(clazz.getName().startsWith(entry.getKey())) {
				if(matchedKey == null || matchedKey.length() < entry.getKey().length()) {
					matchedKey = entry.getKey();
				}
			}
		}
		return matchedKey == null ? null : livePkgToHosts.get(matchedKey);
	}

	/**
	 * 通过手工指定url获得服务引用。url支持绝对地址或相对地址。
	 * 相对地址会根据本机的url配置去获取。
	 * 
	 * @param clazz 传入接口className
	 * @param url 传入根目录后的url地址，相对地址例如/content/helloService，
	 *        相对地址会根据本机文件配置+url构成完成地址。绝对地址必须以http://开头
	 * @return null 如果拿不到服务
	 */
	public static <T> T getService(Class<T> clazz, String url) {
		if(url == null) {
			return null;
		}
		
		if(!url.startsWith("http://")) {
			List<String> hostList = getHostByClass(clazz);
			if(hostList == null || hostList.isEmpty()) {
				LOGGER.error("class {} has no soa_host configured.", clazz.getName());
				return null;
			}
			
			// 负载均衡和hosts自动摘除已经实现
			int index = new Random().nextInt(hostList.size());
			url = "http://" + hostList.get(index) + "/" + url;
		}
		
		HessianProxyFactory factory = new HessianProxyFactory();
		factory.setOverloadEnabled(true); 
		try {
			@SuppressWarnings("unchecked")
			T t = (T) factory.create(clazz, url);
			return t;
		} catch (MalformedURLException e) {
			LOGGER.error("getService {} fail", clazz.getName(), e);
			return null;
		}
	}
	
}
