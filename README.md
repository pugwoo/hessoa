# Hessoa

Hessoa是基于Hessian的SOA框架。Hessian是很轻量级的SOA框架，虽然2014年以后没有更新，但是它的代码很简单，设计理念基于servlet规范，很容易进行二次开发。所以Hessian及其协议是很合适作为轻量级SOA框架。

Hessoa通过注解的方式暴露服务，通过手工配置hosts路由的方式发现服务，但已经具备负载均衡和自动摘除和恢复服务的特性。

hessoa的使用请详见hessoa-demo。

# 其它说明

当Spring MVC有配置全局的异常处理器时，如果Hessian的服务抛出异常，那么异常可能会被全局拦截器拦截到，如果拦截到，那么

```java
@Override
public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler,
	Exception ex)
```

拦截方法的handler参数是`com.pugwoo.hessoa.register.SOAHessianServiceExporter`对象。

