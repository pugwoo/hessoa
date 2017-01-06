package com.pugwoo.hessoa.context;

import com.caucho.hessian.client.HessianConnection;
import com.caucho.hessian.client.HessianProxyFactory;

import java.net.URL;
import java.util.Map;


public class HessianProxy  extends com.caucho.hessian.client.HessianProxy{

    protected HessianProxy(URL url, HessianProxyFactory factory) {
        super(url, factory);
    }
    protected HessianProxy(URL url, HessianProxyFactory factory,Class<?> type) {
        super(url, factory,type);
    }
    @Override
    protected void addRequestHeaders(HessianConnection conn)
    {
        super.addRequestHeaders(conn);

        // add Hessian Header
        Map<String, String> headerMap = HessianHeaderContext.getContext().getHeaders();
        for (Map.Entry<String, String> entry : headerMap.entrySet()) {
            conn.addHeader(entry.getKey(), entry.getValue());
        }
    }
}
