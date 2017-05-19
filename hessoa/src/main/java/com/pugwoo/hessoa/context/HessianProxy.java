package com.pugwoo.hessoa.context;

import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;

import com.caucho.hessian.client.HessianConnection;
import com.pugwoo.hessoa.utils.Constants;
import com.pugwoo.hessoa.utils.QueryStringUtils;

public class HessianProxy extends com.caucho.hessian.client.HessianProxy {

	private static final long serialVersionUID = 71614181756294088L;

	protected HessianProxy(URL url, HessianProxyFactory factory) {
		super(url, factory);
	}

	protected HessianProxy(URL url, HessianProxyFactory factory, Class<?> type) {
		super(url, factory, type);
	}

	@Override
	protected void addRequestHeaders(HessianConnection conn) {
		super.addRequestHeaders(conn);

		// add hessoa Header
		Map<String, Map<String, String>> map = HessianHeaderContext.get();
		for (Entry<String, Map<String, String>> entry : map.entrySet()) {
			conn.addHeader(Constants.HESSOA_CONTEXT_HEADER_PREFIX + entry.getKey(),
					QueryStringUtils.format(entry.getValue()));
		}
	}

}
