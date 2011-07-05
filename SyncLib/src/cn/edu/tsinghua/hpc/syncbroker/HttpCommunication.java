package cn.edu.tsinghua.hpc.syncbroker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
//import org.apache.http.NameValuePair;
import org.apache.http.client.*;
import org.apache.http.entity.*;
//import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
//import org.apache.http.protocol.HTTP;

import android.util.Log;

/**
 * post the XML request to server and get the response. 
 * */
public class HttpCommunication {
	
//	private static final String HTTP_SESSION_ID = "SessionID";
	private static final String HTTP_SESSION_ID = "Sessionid";
	private static HttpParams httpParams = null;
	private static Map<String, String> httpHeads = null;
	private static Header sessionID = null;
	private final String TAG = "HttpCommunication";
	
    static {
    	httpParams = new BasicHttpParams();
    	httpHeads = new HashMap<String, String>();
    	
    	// set timeout in milliseconds until a connection is established.
    	HttpConnectionParams.setConnectionTimeout(httpParams, 10000);
    	// default socket timeout in milliseconds waiting for data
    	HttpConnectionParams.setSoTimeout(httpParams, 10000);
    }
    
    public static void initHttpParameters(Map<String, String> params) {
    	if (null != params && !params.isEmpty()) {
    		httpHeads.putAll(params);
    	}
    }

    public static void initHttpParameter(String name, String value) {
    	if (null != name && null != value) {
    		httpHeads.put(name, value);
    	}
    }
    
    public String postXML(String url, String xml) 
    	throws ClientProtocolException, ServerActionFailed, IOException {
    	Log.v(TAG, xml);
    	StringBuilder contents = new StringBuilder();    	
    	HttpClient hc = null;
    	BufferedReader in = null;
    	
    	try {
       		hc = new DefaultHttpClient(httpParams);
    		HttpPost post = new HttpPost(url);
    
    		if (sessionID == null || sessionID.getValue().equalsIgnoreCase("-1")) {
    			for (Entry<String, String>  entry : httpHeads.entrySet()) {
    				String name = entry.getKey();
    				String value = entry.getValue();
    				post.setHeader(name, value);
    			}
    			sessionID = null;
    		} else {
    			post.setHeader(sessionID);
    		}
    		
    		StringEntity entity = new StringEntity(xml, "UTF-8");
    		entity.setContentType("text/xml; charset=UTF-8");

    		post.setEntity(entity);
	    	HttpResponse rp = hc.execute(post);
	    	Log.d(TAG, "----sessionID---1--"+sessionID);
	    	if (rp.containsHeader(HTTP_SESSION_ID)) {
	    		sessionID = rp.getFirstHeader(HTTP_SESSION_ID);
	    	}
	    	Log.d(TAG, "----sessionID---2--"+sessionID);
	    	in = new BufferedReader(new InputStreamReader(rp.getEntity().getContent()));
	   	    		
	    	String line;
	    	while ((line = in.readLine()) != null) {
	    		contents.append(line + "\n");
	    	}
	  
	    	int statusCode = rp.getStatusLine().getStatusCode();
	    	if (statusCode != HttpStatus.SC_OK) {
	    		StringBuilder sb = new StringBuilder();
	    		sb.append(rp.getStatusLine() + "\n");	    		
	    		sb.append(contents);
	    		throw new ServerActionFailed(sb.toString());
	    	}	  
	     } catch (UnsupportedEncodingException e) {
	    	Log.d(this.getClass().getName(), "error here: " + e.getMessage());
	    	e.printStackTrace();
	    } finally {	    	
	    	if (in != null) {
	    		in.close();
	    	}
	    	hc.getConnectionManager().shutdown();	    	
	    }
	    Log.v(TAG, contents.toString());
	    return contents.toString();
    }
}