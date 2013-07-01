package com.gretel.services.controllers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;

public abstract class AbstractRequestIntentService extends IntentService {
	
	public final static String RESULT_RECEIVER = "RESULT_RECEIVER";

	public final static String REQUEST_URL = "REQUEST_URL";
	
	public AbstractRequestIntentService() {
		super("AbstractRequestIntentService");	
	}
	
	protected AbstractRequestIntentService(String className) {
		super(className);
	}

	@Override
	public void onHandleIntent(Intent intent) {
		final ResultReceiver resultReceiver = 
				intent.getParcelableExtra(RESULT_RECEIVER);
		String url = intent.getStringExtra(REQUEST_URL);
        Bundle serviceInfo = new Bundle();
        
        resultReceiver.send(ServiceStatus.RUNNING, Bundle.EMPTY);
        
        try {
        	HttpRequestBase requestObject = this.buildRequestObject(url, intent);
        	HttpResponse response = this.makeRequest(requestObject);
        	serviceInfo = this.parseHttpResponse(response);
        	
        	resultReceiver.send(ServiceStatus.FINISHED, serviceInfo);
        } catch (Exception e) {
        	serviceInfo.putString(Intent.EXTRA_TEXT, e.getMessage());
        	resultReceiver.send(ServiceStatus.ERROR, serviceInfo);
        }		
	}	
	
	private HttpResponse makeRequest(HttpRequestBase requestObject) 
			throws IOException
	{
	    DefaultHttpClient httpClient = new DefaultHttpClient();	    
	    return httpClient.execute(requestObject);
	}
	
	protected Bundle parseHttpResponse(HttpResponse response) throws Exception {
	    BufferedReader reader = new BufferedReader(
	    		new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
	    String responseLine;
	    StringBuilder responseString = new StringBuilder();
	    
	    while ((responseLine = reader.readLine()) != null) {
	        responseString = responseString.append(responseLine);
	    }
	    
	    Bundle responseInfo = new Bundle();
	    responseInfo.putString(Intent.EXTRA_TEXT, responseString.toString());
	    
	    return responseInfo;
	}
	
	protected abstract HttpRequestBase buildRequestObject(String url, Intent intent) throws Exception;
}
