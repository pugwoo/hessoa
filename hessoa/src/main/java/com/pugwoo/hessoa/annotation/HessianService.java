package com.pugwoo.hessoa.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 2015年1月6日 15:46:44
 * @author pugwoo
 * 该注解用于替代掉类似配置文件:
 * 	<bean name="/userService"
		class="org.springframework.remoting.caucho.HessianServiceExporter">
		<property name="service" ref="userService" />
		<property name="serviceInterface" value="spring_remote.api.service.IUserService" />
	</bean>
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HessianService {

	/**
	 * 对外暴露的hessian url名称，例如'myService'
	 * @return
	 */
    String value() default "";
    
    /**
     * 当注解的实现类有多个实现时，可以自行指定实现接口，
     * 否则按Java返回接口列表选择第一个。
     * 建议指定接口。
     * @return
     */
    Class<?> interf() default Object.class;
    
}
