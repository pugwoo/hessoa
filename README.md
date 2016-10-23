# hessoa

Hessoa是基于Hessian的SOA框架。Hessian是很轻量级的SOA框架，虽然2014年以后没有更新，但是它的代码很简单，设计理念基于servlet规范，很容易进行二次开发。所以Hessian及其协议是很合适作为轻量级SOA框架。

Hessoa通过注解的方式暴露服务，通过手工配置hosts路由的方式发现服务，但已经具备负载均衡和自动摘除和恢复服务的特性。

hessoa的使用请详见hessoa-demo。