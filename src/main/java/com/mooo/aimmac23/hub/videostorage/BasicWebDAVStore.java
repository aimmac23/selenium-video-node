package com.mooo.aimmac23.hub.videostorage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Logger;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;

public class BasicWebDAVStore implements IVideoStore {
	
	private static final Logger log = Logger.getLogger(BasicWebDAVStore.class.getName());
	
	private boolean useSSL = false;
	private String username;
	private String password;
	private HttpHost remoteHost;
	private HttpClient client;
	private URL url;

	private HttpClientContext clientContext;

	public BasicWebDAVStore() throws Exception {
		String urlString = System.getProperty("webdav.url");
		if(urlString == null) {
			throw new IllegalStateException("System property 'webdav.url' not defined - cannot use " + 
					this.getClass().getName());
		}
		
		url = new URL(urlString);
		
		String protocol = url.getProtocol().toLowerCase();
		
		
		if("https".equals(protocol)) {
			useSSL  = true;
		}
		
		username = System.getProperty("webdav.username");
		password = System.getProperty("webdav.password");
		
		if(username != null && password == null) {
			log.warning("'webdav.username' set without 'webdav.password' - Video uploading will probably not work");
		}
		
		int port;
		if(url.getPort() != -1) {
			port = url.getPort();
		}
		else if(useSSL) {
			port = 443;
		}
		else {
			port = 80;
		}
		
		remoteHost = new HttpHost(url.getHost(), port, useSSL ? "https" : "http");
        
        BasicCredentialsProvider credentials = new BasicCredentialsProvider();
        if(username != null) {
        	credentials.setCredentials(new AuthScope(remoteHost), new UsernamePasswordCredentials(username, password));
        }
        
        HttpClientBuilder builder = HttpClientBuilder.create();

        if(useSSL) {
        	builder.setSslcontext(new SSLContextBuilder().useSSL().build());	
        }
        
        client = builder.setDefaultCredentialsProvider(credentials).build();
        
        clientContext = createClientContext(remoteHost, credentials);

	}
	
	/**
	 * The Apache HTTP client makes it as hard as possible to eagerly send auth credentials to remote hosts for
	 * security reasons. We have to do this because we are streaming the video file to upload, so we cannot
	 * send a second HTTP PUT with credentials.
	 * 
	 * @param host - the remote host we are connecting to
	 * @param credentials - an object possibly holding HTTP credentials
	 * @return A Context object that the HTTP client should use for connection-specific properties (clone before use)
	 */
	private HttpClientContext createClientContext(HttpHost host, BasicCredentialsProvider credentials) {
		HttpClientContext context = HttpClientContext.create();
        
        AuthCache authCache = new BasicAuthCache();
        BasicScheme basicAuth = new BasicScheme();
        context.setAuthCache(authCache);
        authCache.put(host, basicAuth);
        
        context.setCredentialsProvider(credentials);
        context.setAuthCache(authCache);
        
        return context;
	}

	@Override
	public void storeVideo(InputStream videoStream, String mimeType,
			String sessionId) throws Exception {
		
		HttpPut request = new HttpPut(url.toExternalForm() + "/" + sessionId + ".webm");
		request.setEntity(new InputStreamEntity(videoStream, ContentType.create(mimeType)));
		
		try {
			HttpResponse response = client.execute(remoteHost, request, new HttpClientContext(clientContext));
			
			int statusCode = response.getStatusLine().getStatusCode();
			
			if(statusCode != HttpStatus.SC_OK && statusCode != HttpStatus.SC_NO_CONTENT && statusCode != HttpStatus.SC_CREATED) {
				throw new HttpException("Could not upload video - response code: " + statusCode + 
						" reason: " + response.getStatusLine().getReasonPhrase());
			}
			
			
			log.info("Successfully uploaded video for session: " + sessionId);
		}
		finally {
			request.releaseConnection();
		}
	}

	@Override
	public WebDAVDownloadContext retrieveVideo(String sessionId) throws Exception {
		HttpGet request = new HttpGet(url.toExternalForm() + "/" + sessionId + ".webm");
		HttpResponse response = client.execute(remoteHost, request);
		
		int statusCode = response.getStatusLine().getStatusCode();
		
		if(statusCode == HttpStatus.SC_NOT_FOUND) {
			return new WebDAVDownloadContext(request, response, false);
		}
		
		if(statusCode != HttpStatus.SC_OK) {
			throw new HttpException("Could not retrieve video - for session: " + sessionId + 
					" response code: " + statusCode + 
					" reason: " + response.getStatusLine().getReasonPhrase());
		}
		
		log.info("Got video for session: " + sessionId);
		
		return new WebDAVDownloadContext(request, response, true);
	}
	
	private static class WebDAVDownloadContext implements StoredVideoDownloadContext {
		
		private HttpRequestBase request;
		private HttpResponse response;
		private boolean videoFound;

		public WebDAVDownloadContext(HttpRequestBase request, HttpResponse response, boolean videoFound) {
			this.request = request;
			this.response = response;
			this.videoFound = videoFound;
		}

		@Override
		public boolean isVideoFound() {
			return videoFound;
		}

		@Override
		public InputStream getStream() throws IOException {
			if(videoFound) {
				return response.getEntity().getContent();
			}
			return null;
		}

		@Override
		public Long getContentLengthIfKnown() {
			Header contentLength = response.getFirstHeader("Content-Length");
			if(contentLength != null) {
				return new Long(contentLength.getValue());
			}
			return null;
		}

		@Override
		public void close() {
			request.releaseConnection();
		}
		
	}

}
