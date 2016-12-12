package com.pugwoo.hessoa.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.caucho.hessian.client.HessianProxyFactory;
import com.pugwoo.hessoa.utils.NetUtils;
import com.pugwoo.hessoa.utils.RedisUtils;

/**
 * 拿到soa的具体url，这是一种手工的方式。
 * 
 * class到ip:port的映射关系，写在机器的文件上，格式为：
 * com.abc.xxx  127.0.0.1:8080
 * com.abc.xxx  127.1.2.3:8081
 * 
 * 配置文件每1秒读取更新一次。
 * 
 * 匹配package如果有多个时，以最细的package更优先为原则。例如认为com.abc.order.list比com.abc.order优先级高。
 * 
 * 机器查找路径为：confPaths写定的。
 * 
 * ==================2016年12月12日 14:50:48 =======================
 * 现在支持redis作为配置中心服务器，但是本地文件依然有效。而且本地文件优先生效。
 * 
 * 默认的，当redis拿到多个服务地址时，会优先使用本机的服务。除非本地文件特意配置。
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
	
	// redis上存放的从接口到url的映射,当本地拿过一次之后，服务就会自动来更新这个列表并检查服务是否存活
	private static Map<String, List<String>> redisInterfToUrls = new ConcurrentHashMap<String, List<String>>();
	
	// 本机的ipv4 ip
	private static List<String> thisMathineIps = new ArrayList<>();
	
	/**处理本地文件*/
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
		thisMathineIps = NetUtils.getThisMathineIps();
		
		// 更新本地配置文件
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						updatePkgToHosts();
						Thread.sleep(3000);
					} catch (Exception e) {
						LOGGER.error("updatePkgToHosts fail", e);
					}				
				}
			}
		});
		thread.setName("SOA-updatePkgToHosts-thread");
		thread.setDaemon(true);
		thread.start();
		
		// 检查服务器存活情况
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
		
		// 更新redis的urls，10秒更新一次，更新不到不清空
		Thread thread3 = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						updateRedisUrl();
						Thread.sleep(10000);
					} catch (Exception e) {
						LOGGER.error("updateRedisUrl fail", e);
					}				
				}
			}
		});
		thread3.setName("SOA-sync-Redis-urls");
		thread3.setDaemon(true);
		thread3.start();
	}
	
	private static void updateRedisUrl() {
		for(Entry<String, List<String>> entry : redisInterfToUrls.entrySet()) {
			List<String> urls = RedisUtils.getURLs(entry.getKey());
			// 只有当urls不为空，才会更新，避免redis挂掉的情况。因为urls宜多不宜少
			entry.setValue(urls);
		}
	}
	
	/**
	 * 检查服务器是否存活
	 */
	private static synchronized void checkHostsLive() {
		// 检查本地的，因为livePkgToHosts可能删除key，所以使用全量替换
		Map<String, List<String>> _livePkgToHosts = new HashMap<String, List<String>>();
		
		for(Entry<String, List<String>> entry : pkgToHosts.entrySet()) {
			List<String> lives = new ArrayList<String>();
			for(String str : entry.getValue()) {
				boolean isLive = isUrlHostAlive(str);
				if(isLive) {
					lives.add(str);
				} else {
					LOGGER.warn("host {} is inavaliable", str);
				}
			}
			
			if(lives.isEmpty()) {
				LOGGER.error("package {} has no avaliable hosts", entry.getKey());
			}
			_livePkgToHosts.put(entry.getKey(), lives);
		}
		livePkgToHosts = _livePkgToHosts;
		
		// 检查redis的，局部替换即可,不会减少key
		for(Entry<String, List<String>> entry : redisInterfToUrls.entrySet()) {
			List<String> lives = new ArrayList<String>();
			for(String str : entry.getValue()) {
				boolean isLive = isUrlHostAlive(str);
				if(isLive) {
					lives.add(str);
				} else {
					LOGGER.warn("host {} is inavaliable", str);
				}
			}
			if(lives.isEmpty()) {
				LOGGER.error("class {} has no avaliable hosts in redis", entry.getKey());
			}
			entry.setValue(lives);
		}
	}
	
	/**
	 * 检查url对应的ip 端口是否可用
	 * @param url
	 * @return
	 */
	private static boolean isUrlHostAlive(String url) {
		if(url == null) {
			return false;
		}
		
		if(url.startsWith("http://")) {
			url = url.substring("http://".length());
		} else if (url.startsWith("https://")) {
			url = url.substring("https://".length());
		}
		int index = url.indexOf("/");
		if(index >= 0) {
			url = url.substring(0, index);
		}
		
		index = url.indexOf(":");
		String ip = null, port = null;
		if(index > 0) {
			ip = url.substring(0, index);
			port = url.substring(index + 1);
		} else {
			ip = url;
			port = "80"; // 内网不考虑https
		}
		
		boolean isLive = checkPort(ip, port);
		return isLive;
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
	private static List<String> getUrlByClass(Class<?> clazz) {
		List<String> urls = redisInterfToUrls.get(clazz.getName());
		if(urls == null) { // 第一次拿
			urls = RedisUtils.getURLs(clazz.getName());
			redisInterfToUrls.put(clazz.getName(), urls);
		}
		if(urls == null || urls.isEmpty()) {
			return null;
		}
		
		// 本地优先
		String matchedKey = null;
		for(Entry<String, List<String>> entry : livePkgToHosts.entrySet()) {
			if(clazz.getName().startsWith(entry.getKey())) {
				if(matchedKey == null || matchedKey.length() < entry.getKey().length()) {
					matchedKey = entry.getKey();
				}
			}
		}
		if(matchedKey != null) {
			List<String> hosts = livePkgToHosts.get(matchedKey);
			if(hosts != null && !hosts.isEmpty()) {
				int index = new Random().nextInt(hosts.size());
				String host = hosts.get(index);
				// 拿redis的接口来做替换
				String url = urls.get(0);
				List<String> result = new ArrayList<>();
				result.add(replaceUrl(url, host));
				return result;
			}
		}
		
		// 如果不是本地指定，那么采用就近原则，优先取本机的
		List<String> preferUrls = new ArrayList<>();
		for(String url : urls) {
			try {
				URL _url = new URL(url);
				if(thisMathineIps.contains(_url.getHost())) {
					preferUrls.add(url);
				}
			} catch (MalformedURLException e) {
				// ignore
			}
		}
		
		return preferUrls.isEmpty() ? urls : preferUrls;
	}
	
	/**
	 * 自动从redis中获得地址，必须依赖redis
	 * 
	 * @param clazz
	 * @return
	 */
	public static <T> T getService(Class<T> clazz) {
		if(clazz == null) {
			return null;
		}
		
		List<String> urlList = getUrlByClass(clazz);
		if(urlList == null || urlList.isEmpty()) {
			LOGGER.error("class {} has no url configured.", clazz.getName());
			return null;
		}
		
		// 负载均衡和hosts自动摘除已经实现
		int urlListSize = urlList.size();
		String url = null;
		if(urlListSize == 1) {
			url = urlList.get(0);
		} else {
			int index = new Random().nextInt(urlListSize);
			url = urlList.get(index);
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

	/**
	 * 通过手工指定url获得服务引用。
	 * 
	 * @param clazz 传入接口className
	 * @param url 必须是绝对地址，必须以http://开头
	 * @return null 如果拿不到服务
	 */
	public static <T> T getService(Class<T> clazz, String url) {
		if(url == null) {
			return null;
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
	
	/**
	 * 替换url里面的host:port为host
	 * @param url
	 * @param host 必须提供
	 * @return 替换失败返回null
	 */
	private static String replaceUrl(String url, String host) {
		try {
			URL _url = new URL(url);
			return _url.getProtocol() + "://" + host + _url.getPath() + 
				(_url.getQuery() == null ? "" : "?" + _url.getQuery());
		} catch (MalformedURLException e) {
			return null;
		}
	}
	
}
