package com.pugwoo.hessoa.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import com.caucho.hessian.client.HessianConnectionException;
import com.caucho.hessian.client.HessianRuntimeException;
import com.pugwoo.hessoa.context.HessianProxyFactory;
import com.pugwoo.hessoa.exceptions.NoHessoaServerException;
import com.pugwoo.hessoa.exceptions.WrongHessoaUrlException;
import com.pugwoo.hessoa.utils.Configs;
import com.pugwoo.hessoa.utils.NetUtils;
import com.pugwoo.hessoa.utils.NetUtils.IPPort;
import com.pugwoo.hessoa.utils.RedisUtils;

/**
 * 拿到soa的具体url，这是一种手工的方式。
 * 
 * class到ip:port的映射关系，写在机器的文件上，格式为：
 * com.abc.xxx  127.0.0.1:8080
 * com.abc.xxx  127.1.2.3:8081
 * 
 * 配置文件每10秒读取更新一次。
 * 
 * 匹配package如果有多个时，以最细的package更优先为原则。例如认为com.abc.order.list比com.abc.order优先级高。
 * 
 * 机器查找路径为：confPaths写定的。
 * 
 * ==================2016年12月12日 14:50:48 =======================
 * 现在支持redis作为配置中心服务器[必须]，但是本地文件依然有效。而且本地文件优先生效。
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
			"C:/soa_host.txt",
			"D:/soa_host.txt"};
		
	private static Map<String, List<String>> pkgToHosts = new HashMap<String, List<String>>();
	private static Map<String, List<String>> livePkgToHosts = new HashMap<String, List<String>>();
	
	private static long fileLastModified = 0;
	private static File hostFile = null;
	
	// redis上存放的从接口到url的映射,当本地拿过一次之后，服务就会自动来更新这个列表并检查服务是否存活
	private static Map<String, List<String>> redisInterfToUrls = new ConcurrentHashMap<String, List<String>>();
	// redis存放的从接口到url的映射，取一个作为模版
	private static Map<String, String> redisInterfTemplate = new HashMap<String, String>();
	
	// 本机的ipv4 ip
	private static List<String> thisMathineIps = NetUtils.getIpv4IPs();
	
	static {
		updatePkgToHosts();
		livePkgToHosts = pkgToHosts;
		
		// 更新本地配置文件
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(10000);
						updatePkgToHosts();
					} catch (Exception e) {
						LOGGER.error("updatePkgToHosts fail", e);
					}				
				}
			}
		}, "SOA-updatePkgToHosts-thread");
		thread.setDaemon(true);
		thread.start();
		
		// 更新redis的urls，10秒更新一次，更新不到不清空
		Thread thread2 = new Thread(new Runnable() {
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
		}, "SOA-sync-Redis-urls");
		thread2.setDaemon(true);
		thread2.start();
		
		// 检查服务器存活情况
		Thread thread3 = new Thread(new Runnable() {
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
		}, "SOA-checkHostsLive-thread");
		thread3.setDaemon(true);
		thread3.start();
	}
	
	/**处理本地文件*/
	private static synchronized void updatePkgToHosts() {
		if(hostFile == null) {
			hostFile = getExistFile();
			if(hostFile == null) {
				return; // ignore
			}
		}
		if(!hostFile.exists()) { // 处理删除文件的情况
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
					// 兼容旧版本写的 ip:port/path的格式，现在只要ip:port
					int indexPath = host.indexOf("/");
					if(indexPath > 0) {
						host = host.substring(0, indexPath);
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
			LOGGER.error("updatePkgToHosts error", e);
		}
	}
	
	/**更新redis url，把所有可用的ip放到列表中*/
	private static void updateRedisUrl() {
		synchronized (redisInterfToUrls) { // 避免和checkHostsLive冲突
			for(Entry<String, List<String>> entry : redisInterfToUrls.entrySet()) {
				List<String> urls = getRedisUrlAndFilterIp(entry.getKey(), false);
				entry.setValue(urls);
			}
		}
	}
	
	/**
	 * 检查服务器是否存活，失效的则移除掉。
	 */
	private static synchronized void checkHostsLive() {
		synchronized (redisInterfToUrls) { // 避免和updateRedisUrl冲突
			// 检查redis的，局部替换即可,不会减少key
			// 2017年12月24日 17:29:30 优化：由检查url修改为检查host，将检查次数减少90%
			Map<IPPort, Boolean> liveHosts = new HashMap<IPPort, Boolean>();
			for(Entry<String, List<String>> entry : redisInterfToUrls.entrySet()) {
				List<String> lives = new ArrayList<String>();
				for(String str : entry.getValue()) {
					IPPort ipPort = NetUtils.parseIpPort(str);
					if(!liveHosts.containsKey(ipPort)) {
						liveHosts.put(ipPort, NetUtils.checkUrlAlive(ipPort));
					}
					if(liveHosts.get(ipPort)) {
						lives.add(str);
					} else {
						LOGGER.warn("host {} is inavaliable", str);
					}
				}
				if(lives.isEmpty()) {
					LOGGER.error("class {} has no avaliable hosts in redis.", entry.getKey());
				} else {
					entry.setValue(lives);// 在全部失效的情况下，不移除，因为移除也没有意义。还可能是本机网络中断的原因。
				}
			}
		}
		
		// 检查本地配置文件的，因为livePkgToHosts可能删除key，所以使用全量替换
		Map<String, List<String>> _livePkgToHosts = new HashMap<String, List<String>>();
		for(Entry<String, List<String>> entry : pkgToHosts.entrySet()) {
			List<String> lives = new ArrayList<String>();
			for(String str : entry.getValue()) {
				if(NetUtils.checkUrlAlive(NetUtils.parseIpPort(str))) {
					lives.add(str);
				} else {
					LOGGER.warn("host {} is inavaliable", str);
				}
			}
			if(lives.isEmpty()) {
				LOGGER.error("class {} has no avaliable hosts in local.", entry.getKey());
			} else {
				_livePkgToHosts.put(entry.getKey(), lives); // 全部失效不移除
			}
		}
		livePkgToHosts = _livePkgToHosts;
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
			urls = getRedisUrlAndFilterIp(clazz.getName(), true);
			redisInterfToUrls.put(clazz.getName(), urls);
		}
		if(urls == null || urls.isEmpty()) {
			return null;
		}
		
		// 本地配置优先
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
				String url = redisInterfTemplate.get(clazz.getName());
				List<String> result = new ArrayList<String>();
				result.add(replaceUrl(url, host));
				return result;
			}
		}
		
		// 确定下是否本机ip优先
		if(Configs.getNetworkUseLocal()) {
			List<String> preferUrls = new ArrayList<String>();
			for(String url : urls) {
				try {
					URL _url = new URL(url);
					if(thisMathineIps.contains(_url.getHost())) {
						preferUrls.add(url);
					}
				} catch (MalformedURLException e) { // ignore
				}
			}
			if(!preferUrls.isEmpty()) {
				return preferUrls;
			}
		}
		
		// 确定是否有内网外网优先配置
		List<String> preferUrls = new ArrayList<String>();
		if("inner".equalsIgnoreCase(Configs.getNetworkPrefer())) {
			for(String url : urls) {
				try {
					URL _url = new URL(url);
					if(NetUtils.isIpLAN(_url.getHost())) {
						preferUrls.add(url);
					}
				} catch (MalformedURLException e) {
				}
			}
		} else if ("outer".equalsIgnoreCase(Configs.getNetworkPrefer())) {
			for(String url : urls) {
				try {
					URL _url = new URL(url);
					if(!NetUtils.isIpLAN(_url.getHost())) {
						preferUrls.add(url);
					}
				} catch (MalformedURLException e) {
				}
			}
		}
		
		return preferUrls.isEmpty() ? urls : preferUrls;
	}
	
	/**
	 * 随机获得一个远程服务url
	 * @param clazz
	 * @return
	 */
	private static String getOneUrlByClass(Class<?> clazz) {
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
		
		return url;
	}
	
	/**
	 * 自动从redis中获得地址，必须依赖redis
	 * 
	 * @param clazz
	 * @return 
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getService(final Class<T> clazz) {
		if(clazz == null) {
			return null;
		}
		
		// 动态代理以实现负载均衡、host切换
		Class<?>[] interfaces = new Class[1];
		interfaces[0] = clazz;
		return (T) Proxy.newProxyInstance(clazz.getClassLoader(),
				interfaces, new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				InvocationTargetException hce = null;
				// 对于HessianRuntimeException，重试15次，间隔200ms；即总时间3秒，超过soaClient检查节点存活的1秒周期
				// 从而提高了调用成功率，理论上调用不会因为机器重启而失败(只要当前节点有一台是活着的)
				for(int i = 1; i <= 15; i++) { 
					String url = getOneUrlByClass(clazz);
					if(url == null) {
						LOGGER.error("no hessoa server for interface {}", clazz);
						throw new NoHessoaServerException("no hessoa server for interface " + 
								clazz.toString());
					}
					
					// 这里使用继承后带头部context的
					HessianProxyFactory factory = new HessianProxyFactory(); 
					factory.setOverloadEnabled(true); 
					try {
						T t = (T) factory.create(clazz, url);
						args = handleArgs(method, args);
						Object result = method.invoke(t, args);
						return result;
					} catch (MalformedURLException e) {
						LOGGER.error("wrong url {} for interface {}", url, clazz.getName(), e);
					    throw new WrongHessoaUrlException("wrong url " + url + " for interface "
					    		+ clazz.getName());
					} catch (InvocationTargetException e1) {
						// 只处理连接异常的重试
						boolean isRetry = false;
						if(e1.getTargetException() instanceof HessianConnectionException) {
							isRetry = true;
						}
						if(!isRetry && e1.getTargetException() instanceof HessianRuntimeException) {
							if(e1.getTargetException().getCause() instanceof ConnectException) {
								isRetry = true;
							}
						}
						if(!isRetry) {
							throw e1;
						}

						hce = e1;
						LOGGER.error("HessianConnectionException and try at {} times", i, e1);
						try {
							Thread.sleep(200);
						} catch (Exception e) { // ignore
						}
					}
				}
				throw hce;
			}
		});
	}

	/**
	 * 通过手工指定url获得服务引用。该方法建议在开发联调时使用。
	 * 
	 * @param clazz 传入接口className
	 * @param url 必须是绝对地址，必须以http://开头
	 * @return null 如果拿不到服务
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getService(final Class<T> clazz, final String url) {
		if(url == null) {
			return null;
		}

		Class<?>[] interfaces = new Class[1];
		interfaces[0] = clazz;
		return (T) Proxy.newProxyInstance(clazz.getClassLoader(),
				interfaces, new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				HessianProxyFactory factory = new HessianProxyFactory();
				factory.setOverloadEnabled(true); 
				try {
					T t = (T) factory.create(clazz, url);
					args = handleArgs(method, args);
					Object result = method.invoke(t, args);
					return result;
				} catch (MalformedURLException e) {
					LOGGER.error("wrong url {} for interface {}", url, clazz.getName(), e);
				    throw new WrongHessoaUrlException("wrong url " + url + " for interface "
				    		+ clazz.getName());
				}
			}
		});
	}
	
	/**
	 * 处理当参数args是接口类型的子类时，把它转化成接口准确的类型。
	 * 支持处理泛型的类：java.util.List, java.util.Map, java.util.Set，且只支持一层泛型，
	 * 对于Object类或WildcardType ?不进行处理，接口定义请尽量不要用WildcardType或Object，不确定因素太多。
	 */
	@SuppressWarnings("unchecked")
	private static Object[] handleArgs(Method method, Object[] args) {
		if(args == null || args.length == 0) {
			return args;
		}
		
		Class<?> classes[] = method.getParameterTypes();
		Type[] types = method.getGenericParameterTypes();
		for(int i = 0; i < args.length && i < classes.length; i++) {
			if(args[i] == null) {
				continue;
			}
			
			// 如果class[i]是基本类型，则不处理
			if(classes[i] == byte.class || classes[i] == short.class || classes[i] == int.class
			 ||classes[i] == long.class || classes[i] == float.class || classes[i] == double.class
			 ||classes[i] == boolean.class || classes[i] == char.class) {
				continue;
			}
			
			if(classes[i] == List.class) {
				List<Object> list = (List<Object>) args[i];
				ParameterizedType type = (ParameterizedType) types[i];
				Class<?> clazz = null;
				if(type.getActualTypeArguments()[0] instanceof Class<?>) {
					clazz = (Class<?>) type.getActualTypeArguments()[0];
					if(clazz == Object.class) {clazz = null;}
				}
				if(clazz == null) { // 对于泛型中的WildcardType不进行处理
					continue;
				}
				for(int j = 0; j < list.size(); j++) {
					if(list.get(j) == null) {continue;}
					if(clazz != list.get(j).getClass()) {
						Object obj = BeanUtils.instantiateClass(clazz);
						BeanUtils.copyProperties(list.get(j), obj);
						list.set(j, obj);
					}
				}
			} else if (classes[i] == Set.class) {
				Set<Object> set = (Set<Object>) args[i];
				ParameterizedType type = (ParameterizedType) types[i];
				Class<?> clazz = null;
				if(type.getActualTypeArguments()[0] instanceof Class<?>) {
					clazz = (Class<?>) type.getActualTypeArguments()[0];
					if(clazz == Object.class) {clazz = null;}
				}
				if(clazz == null) { // 对于泛型中的WildcardType不进行处理
					continue;
				}
				Iterator<Object> it = set.iterator();
				Set<Object> newSet = null;
				while(it.hasNext()) {
					Object obj = it.next();
					if(obj == null) {continue;}
					if(clazz != obj.getClass()) {
						Object o = BeanUtils.instantiateClass(clazz);
						BeanUtils.copyProperties(obj, o);
						it.remove();
						if(newSet == null) {
							newSet = new HashSet<Object>();
						}
						newSet.add(o);
					}
				}
				if(newSet != null && !newSet.isEmpty()) {
					for(Object obj : newSet) {
						set.add(obj);
					}
				}
				args[i] = set;
			} else if (classes[i] == Map.class) {
				Map<Object, Object> map = (Map<Object, Object>) args[i];
				ParameterizedType type = (ParameterizedType) types[i];
				Class<?> clazz0 = null;
				if(type.getActualTypeArguments()[0] instanceof Class<?>) {
					clazz0 = (Class<?>) type.getActualTypeArguments()[0];
					if(clazz0 == Object.class) {clazz0 = null;}
				}
				Class<?> clazz1 = null;
				if(type.getActualTypeArguments()[1] instanceof Class<?>) {
					clazz1 = (Class<?>) type.getActualTypeArguments()[1];
					if(clazz1 == Object.class) {clazz1 = null;}
				}
				if(clazz0 == null && clazz1 == null) {continue;}
				
				Set<Object> removeKey = null;
				Map<Object, Object> newMap = null;
				for(Entry<Object, Object> entry : map.entrySet()) {
					Object key = entry.getKey();
					Object oldKey = key;
					Object value = entry.getValue();
					boolean isChange = false;
					if(key != null && clazz0 != null && clazz0 != key.getClass()) {
						isChange = true;
						Object obj = BeanUtils.instantiateClass(clazz0);
						BeanUtils.copyProperties(key, obj);
						key = obj;
					}
					if(value != null && clazz1 != null && clazz1 != value.getClass()) {
						isChange = true;
						Object obj = BeanUtils.instantiateClass(clazz1);
						BeanUtils.copyProperties(value, obj);
						value = obj;
					}
					if(isChange) {
						if(removeKey == null) {
							removeKey = new HashSet<Object>();
						}
						if(newMap == null) {
							newMap = new HashMap<Object, Object>();
						}
						newMap.put(key, value);
						removeKey.add(oldKey);
					}
				}
				if(removeKey != null) {
					for(Object key : removeKey) {
						map.remove(key);
					}
				}
				if(newMap != null) {
					for(Entry<Object, Object> entry : newMap.entrySet()) {
						map.put(entry.getKey(), entry.getValue());
					}
				}
			} else {
				if(classes[i] != args[i].getClass()) { // 当类型不一致时，强制转换类型
					Object obj = BeanUtils.instantiateClass(classes[i]);
					BeanUtils.copyProperties(args[i], obj);
					args[i] = obj;
				}
			}
		}
		
		return args;
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
	
	/**
	 * 从redis拿链接：
	 * 1. 同步更新redisInterfTemplate
	 * 2. 去掉不同局域网的。虽然不同的局域网也可以访问，但在实际场景中，这种情况太少。后面需要再做成配置化的吧。
	 * 3. 只留下alive可以连通的。如果全部都不联调，那么不过滤
	 * @param key
	 * @return
	 */
	private static List<String> getRedisUrlAndFilterIp(String key, boolean isFirstGet) {
		List<String> urls = RedisUtils.getURLs(key);
		if(urls != null && !urls.isEmpty()) {
			redisInterfTemplate.put(key, urls.get(0));
		}
		
		// 一般云服务器是10.的局域网，本地是192.168.或172.16~31的ip
		List<String> newUrls = new ArrayList<String>();
		for(String url : urls) {
			try {
				URL _url = new URL(url);
				String host = _url.getHost();
				boolean isLAN = false, isSameLAN = false;
				for(int i = 1; i <= 3; i++) { // 检查3类局域网
					if(NetUtils.isIpInRange(host, i)) {
						isLAN = true;
						for(String localIp : thisMathineIps) {
							if(NetUtils.isIpInRange(localIp, i)) {
								newUrls.add(url);
								isSameLAN = true;
								break;
							}
						}
					}
					if(isSameLAN) break;
				}
				if(!isLAN) {
					newUrls.add(url);
				}
			} catch (MalformedURLException e) {
			}
		}
		
		// 如果配置了第一次拿不检测
		if(!isFirstGet || Configs.isFirstGetCheckAlive()) {
			List<String> liveUrls = new ArrayList<String>();
			for(String url : newUrls) {
				if(NetUtils.checkUrlAlive(NetUtils.parseIpPort(url))) {
					liveUrls.add(url);
				}
			}
			return liveUrls.isEmpty() ? newUrls : liveUrls;
		} else {
			return newUrls;
		}
	}
}
