# Hessoa

Hessoa是基于Hessian的SOA框架。Hessian是很轻量级的SOA框架，虽然2014年以后没有更新，但是它的代码很简单，设计理念基于servlet规范，很容易进行二次开发。所以Hessian及其协议是很合适作为轻量级SOA框架。

Hessoa通过注解的方式暴露服务，通过手工配置hosts路由的方式发现服务，但已经具备负载均衡和自动摘除和恢复服务的特性。

hessoa的使用请详见hessoa-demo。

## 配置中心

目前支持redis作为配置中心，在线上环境中，跑起来需要redis服务器。

请先在本地安装一个redis，默认端口无密码即可，然后即可跑起hessoa-demo项目。

## 关于上下文传递

Hessoa内置支持SOA调用之间传递上下文信息，每个线程拥有独立的上下文信息。A应用在运行过程中，将数据放入SOA上下文中，调用`SOAClientContext.add("loginUserId", "3");`将登录信息放在SOA上下文中；此后，只要A应用调用了其它应用的SOA服务（无论多少次），那么对方就可以通过它自己的`SOAClientContext.get("loginUserId")`拿到A应用传递过来的信息。

## 关于内外网安全

hessoa是基于标准的Servlet容器的，所以当hessoa和外网的接口在一起的时候，就可能导致hessoa的接口被外网访问到，导致安全问题。因此，在设计上，hessoa所有的接口都会以`/_hessoa/`开头，可以在nginx那边配置，所有请求到`/_hessoa/`的请求都禁止掉，即可从运维层面完全隔离和处理安全问题。

## 其它说明

当Spring MVC有配置全局的异常处理器时，如果Hessian的服务抛出异常，那么异常可能会被全局拦截器拦截到，如果拦截到，那么

```java
@Override
public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler,
	Exception ex)
```

拦截方法的handler参数是`com.pugwoo.hessoa.register.SOAHessianServiceExporter`对象，请特殊处理掉。

另外，对于Spring MVC的登录拦截器，它也会处理到`/_hessoa/`开头的链接，此时也需要在登录拦截器中对以`/_hessoa/`开头的链接特殊处理。

