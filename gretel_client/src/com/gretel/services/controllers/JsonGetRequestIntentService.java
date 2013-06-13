package com.gretel.services.controllers;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import android.content.Intent;

public class JsonGetRequestIntentService extends AbstractRequestIntentService {

	public JsonGetRequestIntentService() {
		super("JsonGetRequestIntentService");	
	}
	
	@Override
	protected HttpRequestBase buildRequestObject(String url, Intent intent)
			throws Exception {
		HttpGet httpGet = new HttpGet(url);
		httpGet.setHeader("Accept", "application/json");
		httpGet.setHeader("Content-type", "application/json");
	    
		return httpGet;
	}
}
