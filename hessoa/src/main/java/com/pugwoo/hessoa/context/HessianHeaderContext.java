package com.pugwoo.hessoa.context;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Element on 2017/1/5.
 */
public class HessianHeaderContext {

    private static final ThreadLocal<HessianHeaderContext> THREAD_LOCAL = new ThreadLocal<>();

    private Map<String,String> headers = new HashMap<>();

    private HessianHeaderContext(){

    }
    public static HessianHeaderContext getContext() {
        HessianHeaderContext context = THREAD_LOCAL.get();
        if (context == null) {
            context = new HessianHeaderContext();
            THREAD_LOCAL.set(context);
        }
        return context;
    }

    public void addHeader(String name, String value) {
        headers.put(name, value);
    }

    public String getHeader(String name) {
        return headers.get(name);
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public static void close() {
        HessianHeaderContext context = THREAD_LOCAL.get();
        if (context != null) {
            context.headers.clear();
            THREAD_LOCAL.set(null);
        }
    }
}
