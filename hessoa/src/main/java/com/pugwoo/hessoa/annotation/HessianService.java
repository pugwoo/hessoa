package com.pugwoo.hessoa.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 2015年1月6日 15:46:44 实现等同于xml配置hessian服务的注解。
 * @author pugwoo
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HessianService {
	
	/**
	 * 需要暴露的接口，必须指定。
	 * @return
	 */
	Class<?> value();
    
}
