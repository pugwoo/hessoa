package com.pugwoo.hessoa.register;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.Query;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.remoting.caucho.HessianServiceExporter;
import org.springframework.web.context.ServletConfigAware;

import com.pugwoo.hessoa.context.HessianHeaderContext;
import com.pugwoo.hessoa.utils.Configs;
import com.pugwoo.hessoa.utils.Constants;
import com.pugwoo.hessoa.utils.NetUtils;
import com.pugwoo.hessoa.utils.QueryStringUtils;
import com.pugwoo.hessoa.utils.RedisUtils;

/**
 * 用于自动向配置中心注册当前提供的服务的信息，最主要的就是连接地址:
 * http://localhost:8080/hessianweb/_hessoa/helloServiceExporter
 * 它是动态的，根据web容器不同而不同
 * 
 * @author pugwoo
 */
public class SOAHessianServiceExporter extends HessianServiceExporter implements
		 ServletConfigAware{
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SOAHessianServiceExporter.class);
	
	private String beanName; // 就是url，由注解HessianServiceScanner注入
	
	private static Map<String, List<String>> interfUrls = new ConcurrentHashMap<String, List<String>>();
	
	static { // 30秒汇报一次
		if(RedisUtils.isConfigRedis()) {
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					while(true) {
						try {
							Thread.sleep(30 * 1000);
						} catch (InterruptedException e1) { // ignore
						}
						for(Entry<String, List<String>> entry : interfUrls.entrySet()) {
							try {
								RedisUtils.addUrl(entry.getKey(), entry.getValue(), 60); //60秒数据过时
							} catch (Throwable e) {
								LOGGER.error("RedisUtils.addUrl {} exception", entry.getKey(), e);
							}
						}
					}
				}
			}, "SOAHessianServiceExporter");
			thread.setDaemon(true);
			thread.start();
		}
	}
	
	/**
	 * 读取自定义的header，并全量放到HessianHeaderContext中
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		HessianHeaderContext.clear();
		Enumeration<String> enumeration = request.getHeaderNames();
		while (enumeration.hasMoreElements()) {
		 	String name = enumeration.nextElement().toString();
		 	String value = request.getHeader(name);
		 	if(name != null && name.startsWith(Constants.HESSOA_CONTEXT_HEADER_PREFIX)) {
		 		String namespace = name.substring(Constants.HESSOA_CONTEXT_HEADER_PREFIX.length());
		 		HessianHeaderContext.set(namespace, QueryStringUtils.parse(value));
		 	}
		}
		
		super.handleRequest(request, response);
	}

	@Override
	public void setServletConfig(ServletConfig servletConfig) {
		List<String> urls = new ArrayList<String>();
		ServletContext servletContext = servletConfig.getServletContext();
		try {
			List<String> endPoints = getEndPoints();			
			String contextPath = servletContext.getContextPath();
			String servletName = servletConfig.getServletName();
			File webXml = new File(servletContext.getRealPath("/WEB-INF/web.xml"));
			
			List<String> urlMappings = getServletUrlMapping(webXml, servletName);
			if(urlMappings.isEmpty()) {
				throw new Exception("servlet url mapping not found,servlet:"
						+ servletName);
			}
			
			// 只取出第一个，并去掉*
			String urlMapping = urlMappings.get(0).trim().replace("*", "");
			
			for(String endPoint : endPoints) {
				String url = null;
				if(urlMapping.endsWith("/") && beanName.startsWith("/")) {
					url = urlMapping + beanName.substring(1);
				} else {
					url = urlMapping + beanName;
				}
				urls.add(endPoint + contextPath + url);
			}
		} catch (Exception e) {
			LOGGER.error("scan hessoa service exporter ex", e);
		}
		
		if(!urls.isEmpty()) {
			if(RedisUtils.isConfigRedis()) { // 起一个线程，不停向redis报告，每分钟报告一次
				interfUrls.put(getServiceInterface().getName(), urls);
				RedisUtils.addUrl(getServiceInterface().getName(), urls, 60); // 60秒数据过时
			}
		}
	}
	
	/**
	 * 获取当前容器的ipv4的访问ip:port。
	 * 返回值格式示例：http://192.168.0.1:8080
	 * @return
	 * @throws Exception
	 */
	private List<String> getEndPoints() throws Exception {
		List<String> endPoints = new ArrayList<String>();
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		Set<ObjectName> objs = mbs.queryNames(new ObjectName(
				"*:type=Connector,*"), Query.match(Query.attr("protocol"),
				Query.value("HTTP/1.1")));
		List<String> addresses = NetUtils.getIpv4IPs();
		
		// 获得额外的ip地址
		String networkPublicHostname = Configs.getNetworkPublicHostname();
		
		for (Iterator<ObjectName> i = objs.iterator(); i.hasNext();) {
			ObjectName obj = i.next();
			String scheme = mbs.getAttribute(obj, "scheme").toString();
			String port = obj.getKeyProperty("port");
			for (String ip : addresses) {
				String ep = scheme + "://" + ip + ":" + port;
				endPoints.add(ep);
			}
			
			if(!networkPublicHostname.isEmpty()) {
				try {
					InetAddress address = InetAddress.getByName(networkPublicHostname);
					if(address != null) {
						String ep = scheme + "://" + address.getHostAddress() + ":" + port;
						endPoints.add(ep);
					}
				} catch (Throwable e) {
					LOGGER.error("get networkPublicHostname:{} ip fail, ignore", networkPublicHostname, e);
				}
			}
		}
		
		return endPoints;
	}
	
	/**
	 * 读取web.xml来获取到servletName对应的url-mapping的原始值，可能有多个或0个
	 * 
	 * @param servletName
	 * @return
	 * @throws FileNotFoundException 
	 */
	private List<String> getServletUrlMapping(File webXml, String servletName)
			throws IOException {
		List<String> urlMappings = new ArrayList<String>();
		
		StringBuilder sb = new StringBuilder();
		BufferedReader br = new BufferedReader(new FileReader(webXml));
		String line = null;
		while ((line = br.readLine()) != null) {
			sb.append(line); // 已经去掉回车符
		}
		br.close();
		
		String xml = sb.toString();
		
		// 删除注释
		String regexPattern = "<!--.[^-]*(?=-->)-->";
		Pattern pattern = Pattern.compile(regexPattern);
		Matcher matcher = pattern.matcher(xml);
		xml = matcher.replaceAll("");
		
		// 通过正则表达式找到servletName对应url mapping
		regexPattern = "<servlet-name>\\s*" + servletName 
				+ "\\s*</servlet-name>\\s*<url-pattern>(.*?)</url-pattern>";
		pattern = Pattern.compile(regexPattern);
		matcher = pattern.matcher(xml);
		while(matcher.find()) {
			String str = matcher.group(1);
			if(str != null) {
				urlMappings.add(str);
			}
		}
		
		return urlMappings;
	}
	
	public String getBeanName() {
		return beanName;
	}

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}
}
