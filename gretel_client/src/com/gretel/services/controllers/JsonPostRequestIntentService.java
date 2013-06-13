package com.gretel.services.controllers;


import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;

import android.content.Intent;

public class JsonPostRequestIntentService extends AbstractRequestIntentService {
	
	public final static String SERIALIZED_DATA = "SERIALIZED_DATA";
	
	public JsonPostRequestIntentService() {
		super("JsonPostRequestIntentService");	
	}

	protected HttpEntity parseIntentData(Intent intent) throws Exception {
		return new StringEntity(intent.getStringExtra(SERIALIZED_DATA));
	}

	@Override
	protected HttpRequestBase buildRequestObject(String url, Intent intent) throws Exception {
		HttpPost httpPost = new HttpPost(url);
	    httpPost.setHeader("Accept", "application/json");
	    httpPost.setHeader("Content-type", "application/json");	    
	    httpPost.setEntity(this.parseIntentData(intent));
	    
		return httpPost;
	}
}