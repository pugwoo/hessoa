package com.pugwoo.hessoa.context;

import com.caucho.hessian.io.HessianRemoteObject;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.URL;


public class HessianProxyFactory extends com.caucho.hessian.client.HessianProxyFactory {

    @Override
    public Object create(Class<?> api, URL url, ClassLoader loader)
    {
        if (api == null) {
            throw new NullPointerException("api must not be null for HessianProxyFactory.create()");
        }

        InvocationHandler handler = new HessianProxy(url, this, api);

        return Proxy.newProxyInstance(loader,
                new Class[]{api, HessianRemoteObject.class},
                handler);
    }
}
