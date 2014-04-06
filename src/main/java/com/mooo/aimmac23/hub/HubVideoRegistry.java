package com.mooo.aimmac23.hub;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.logging.Logger;

import org.apache.commons.exec.StreamPumper;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.util.EntityUtils;
import org.openqa.grid.internal.ExternalSessionKey;
import org.openqa.selenium.remote.internal.HttpClientFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

public class HubVideoRegistry {
	
	private static final Logger log = Logger.getLogger(HubVideoRegistry.class.getName());

	
	private static Cache<ExternalSessionKey, File> availableVideos;
	
	static {
		availableVideos = CacheBuilder.newBuilder().maximumSize(200).removalListener(new RemovalListener<ExternalSessionKey, File>() {
			@Override
			public void onRemoval(RemovalNotification<ExternalSessionKey, File> arg0) {
				if(arg0.getValue().delete()) {
					log.info("Deleted recording due to excess videos: " + arg0.getKey());
				}
			}
		}).build();
	}
	
	public static void copyVideoToHub(ExternalSessionKey key, String pathKey, URL remoteHost) {
		String serviceUrl = remoteHost + "/extra/VideoRecordingControlServlet";
		
		HttpHost remote = new HttpHost(remoteHost.getHost(),
				remoteHost.getPort());
        HttpClientFactory httpClientFactory = new HttpClientFactory();
        HttpClient client = httpClientFactory.getHttpClient();
        
		BasicHttpEntityEnclosingRequest r = new BasicHttpEntityEnclosingRequest(
                "POST", serviceUrl + "?command=download&filekey=" + pathKey);
		
        try {
			HttpResponse response = client.execute(remote, r);
			if(response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				log.warning("Could not download video: " + EntityUtils.toString(response.getEntity()));
				return;
			}
			// XXX: Should check mime-type, just in case
			
			File outputFile = File.createTempFile("screencast", ".mp4");
			FileOutputStream outputStream = new FileOutputStream(outputFile);
			try {
				new StreamPumper(response.getEntity().getContent(), outputStream).run();
			}
			finally {
				outputStream.close();
			}
			
			availableVideos.put(key, outputFile);
			log.info("Successfully retrieved video for session: " + key + " and temporarily stashed it at: " + outputFile);
			
        }
		catch(Exception e) {
			log.warning("Could not download video, exception caught: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static File getVideoForSession(ExternalSessionKey key) {
		return availableVideos.getIfPresent(key);
	}
	

}
