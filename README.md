# Hessoa

Hessoa是基于Hessian的SOA框架。Hessian是很轻量级的SOA框架，虽然2014年以后没有更新，但是它的代码很简单，设计理念基于servlet规范，很容易进行二次开发。所以Hessian及其协议是很合适作为轻量级SOA框架。

Hessoa通过注解的方式暴露服务，通过手工配置hosts路由的方式发现服务，但已经具备负载均衡和自动摘除和恢复服务的特性。

hessoa的使用请详见hessoa-demo。【必须】请先在本地安装一个redis，默认端口无密码即可，然后即可跑起hessoa-demo项目：将hessoa-demo war包部署到tomcat，然后main方式跑`test.TestClient`类即可。

hessoa依赖于spring mvc，但hessoa不强依赖而spirng mvc，而是maven配置为由使用者引入spring mvc，也即hessoa并不强指定应该用哪个版本的spring mvc，而是由使用者的spring版本来决定。具体详见hessoa-demo。

## 关于配置中心

目前支持redis作为配置中心，在线上环境中，跑起来需要redis服务器。为了最好的体验，必须依赖redis作为配置中心。目前服务端每30秒上报一次服务url给redis，过期时间为60秒，也即如果一台服务器超过1分钟没有上报服务，那么该台机器url将从配置中心删除。

关于配置文件`hessoa-redis-${env}.properties`，约定是这个名称且放在classes根目录下，如果开发有不同的环境，那么通过设置java全局变量env来指定。例如开发环境`-Denv=dev`，线上环境`-Denv=idc`这样，对应的配置文件名称也相应修改，例如`hessoa-redis-dev.properties`和`hessoa-redis-idc.properties`。这个名称是约定的，不支持自定义配置文件的名称，不支持更改全局变量env变量名。此为hessoa约定给使用者带来的限制，请注意。

配置文件内容说明：

```bash
# 这3个是配置中心redis的链接配置
redis.host=127.0.0.1
redis.port=6379
redis.password=

# 这个是redis中服务器url的key的前缀，当多个环境共用一台redis，或者配置服务有set/group分区时，可以使用
#redis.key.prefix default `empty_string`
redis.key.prefix=idc

# 是否优先使用本地的服务,如果本地有服务，则百分比从本地拿，如果本地没有服务，则会通过其它优先级拿。本地、开发、测试机器建议设置为true，线上负载均衡建议设置为false
#network.uselocal default false
network.uselocal=false

# 是否倾向于使用外网还是内网的服务，当开发环境服务在外网，本地开发在局域网时，建议设置为outer，这样不会调用到他人的机器上；线上环境建议设置为inner，优先使用内网
#network.prefer default `empty_string`, values: outer, inner
network.prefer=

# 检查网络是否联调的超时值，单位毫秒，线上建议100，线下建议3000
#network.check.timeout default 1000
network.check.timeout=100

# 第一次拿到服务时，是否检测该url是否可用。线上建议设置为true，本地或开发环境可以设置为false，节省启动时间。
#network.check.first default true
#is check network alive when first time get
network.check.first=true

#extra ip, should config in /etc/hosts
# 对于腾讯云和google云，其VPS上没有外网网卡，如果服务需要暴露给外网的调用者，那么hessoa自动注册时要告诉hessoa外网ip地址。这个外网ip地址每台机器不同，这里设计为配置hostname，然后在/etc/hosts中配置该hostname对应的ip地址，例如`123.34.56.7 hessoa_public_ip`
# 如果服务器上有外网网卡ip，或者不需要暴露给外网，就不需要这一项了
#network.public.hostname=hessoa_public_ip
```

## 关于环境隔离、部署SET隔离

总的有两种隔离方式：

- 1）不同的redis数据库（含相同redis实例但不同的database，redis有0到16个数据库）。
- 2）不同的`redis.key.prefix`前缀。

通过这两种方式的组合，用于实现环境隔离、部署SET隔离。

## 关于上下文传递

Hessoa内置支持SOA调用之间传递上下文信息，每个线程拥有独立的上下文信息。A应用在运行过程中，将数据放入SOA上下文中，调用`SOAClientContext.add("loginUserId", "3");`将登录信息放在SOA上下文中；此后，只要A应用调用了其它应用的SOA服务（无论多少次），那么对方就可以通过它自己的`SOAClientContext.get("loginUserId")`拿到A应用传递过来的信息。

上下文传输的数据会转换成QueryString的形式传输，用户自己定义的全部上下文数据量，建议不超过4k。

## 关于内外网安全

hessoa是基于标准的Servlet容器的，所以当hessoa和外网的接口在一起的时候，就可能导致hessoa的接口被外网访问到，导致安全问题。因此，在设计上，hessoa所有的接口都会以`/_hessoa/`开头，可以在nginx那边配置，所有请求到`/_hessoa/`的请求都禁止掉，即可从运维层面完全隔离和处理安全问题。

## 关于客户端调用的参数是接口定义参数类的子类的情况

hessoa支持参数实例类型是接口定义类型的子类型的情况，同时支持java.util.List, java.util.Set和java.util.Map中，参数是泛型类型的子类型的情况，但只支持一层的泛型。

## 关于传输数据用到枚举Enum的情况

一句话：不建议在接口来回传递参数中使用枚举Enum。这篇[博客](http://yangbolin.cn/2016/05/22/enum-probolems-in-hessian/)有说明。

## 其它说明

当Spring MVC有配置全局的异常处理器时，如果Hessian的服务抛出异常，那么异常可能会被全局拦截器拦截到，如果拦截到，那么

```java
@Override
public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler,
	Exception ex)
```

拦截方法的handler参数是`com.pugwoo.hessoa.register.SOAHessianServiceExporter`对象，请特殊处理掉。

另外，对于Spring MVC的登录拦截器，它也会处理到`/_hessoa/`开头的链接，此时也需要在登录拦截器中对以`/_hessoa/`开头的链接特殊处理。

