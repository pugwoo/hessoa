package com.pugwoo.hessoa.test.web.interceptors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * 拦截器会处理_hessoa的请求，例如登录态校验，如果不希望soa的url请求被拦截，那么可以设置一个变量来处理
 * @author nick
 */
public class DefaultInterceptor implements HandlerInterceptor {
	
	/**排除掉的context url前缀，主要配合soa使用*/
	private String excludePrefix;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		if(excludePrefix != null) {
			String requestURI = request.getRequestURI();
			String contextPath = request.getContextPath();
			if(requestURI != null && contextPath != null &&
					requestURI.startsWith(contextPath)) {
				requestURI = requestURI.substring(contextPath.length());
			}
			if(requestURI != null) {
				if(requestURI.startsWith(excludePrefix)) {
					return true;
				}
			}
		}
		
		// 做其它拦截器的事情
		
		return true;
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
		
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
		
	}

	public String getExcludePrefix() {
		return excludePrefix;
	}

	public void setExcludePrefix(String excludePrefix) {
		this.excludePrefix = excludePrefix;
	}
	
}
