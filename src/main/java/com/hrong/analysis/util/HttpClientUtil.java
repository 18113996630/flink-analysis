package com.hrong.analysis.util;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.util.Map;
import java.util.Map.Entry;

public class HttpClientUtil {
	/**
	 * 发送post请求
	 */
	public static String httpPost(String url, String requestJSON, Map<String, String> headers) throws Exception {
		String result = null;
		HttpClientBuilder builder = HttpClientBuilder.create();
		CloseableHttpClient client = builder.build();
		HttpPost post = new HttpPost(url);
		// 设置header
		post.setHeader("Content-type", "application/json");
		if (headers != null && headers.size() > 0) {
			for (Entry<String, String> entry : headers.entrySet()) {
				post.setHeader(entry.getKey(), entry.getValue());
			}
		}
		StringEntity entity = new StringEntity(requestJSON, "utf-8");
		entity.setContentType("application/x-www-form-urlencoded");
		post.setEntity(entity);
		try {
			CloseableHttpResponse response = client.execute(post);
			result = EntityUtils.toString(response.getEntity(), "utf-8");
		} catch (Exception e) {
			System.out.println("调用" + url + "出错:" + e.getMessage());
			throw new RuntimeException(e);
		} finally {
			client.close();
		}
		return result;
	}
}
