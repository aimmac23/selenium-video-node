package com.mooo.aimmac23.hub;

import java.net.URL;
import java.util.logging.Logger;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.openqa.grid.internal.ExternalSessionKey;
import org.openqa.selenium.remote.internal.HttpClientFactory;

import com.mooo.aimmac23.hub.videostorage.IVideoStore;
import com.mooo.aimmac23.hub.videostorage.LocalTempFileStore;
import com.mooo.aimmac23.hub.videostorage.StoredVideoDownloadContext;
import com.mooo.aimmac23.hub.videostorage.StoredVideoInfoContext;

@SuppressWarnings("unchecked")
public class HubVideoRegistry {
	
	private static final Logger log = Logger.getLogger(HubVideoRegistry.class.getName());

	private static IVideoStore videoStore;
	
	static {
		try {
			Class<? extends IVideoStore> storageClass = LocalTempFileStore.class;
			String classname = System.getProperty("video.storage");
			if(classname != null) {
				storageClass = (Class<? extends IVideoStore>) Class.forName(classname);
			}
			
			videoStore = storageClass.newInstance();
			log.info("Using " + storageClass + " to store videos");
			
		} catch (Exception e) {
			// throw a nasty error to hopefully prevent the Hub from trying to continue without this 
			throw new Error("Could not initialize video store due to exception", e);
		}
	}

	public static void copyVideoToHub(ExternalSessionKey key, String pathKey, URL remoteHost) {
		String serviceUrl = remoteHost + "/extra/VideoRecordingControlServlet";
		
		HttpHost remote = new HttpHost(remoteHost.getHost(),
				remoteHost.getPort());
        HttpClientFactory httpClientFactory = new HttpClientFactory();
        HttpClient client = httpClientFactory.getHttpClient();
        
        HttpGet r = new HttpGet(serviceUrl + "?command=download&filekey=" + pathKey);
		
        try {
			HttpResponse response = client.execute(remote, r);
			if(response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				log.warning("Could not download video: " + EntityUtils.toString(response.getEntity()));
				return;
			}
			// XXX: Should check mime-type, just in case
			videoStore.storeVideo(response.getEntity().getContent(), "video/webm", key.toString());
        }
		catch(Exception e) {
			log.warning("Could not download video, exception caught: " + e.getMessage());
			e.printStackTrace();
		}
        finally {
        	r.releaseConnection();
        }
	}
	
	public static StoredVideoDownloadContext getVideoForSession(ExternalSessionKey key) throws Exception {
		
		return videoStore.retrieveVideo(key.toString());
	}
	
	public static StoredVideoInfoContext getVideoInfoForSession(ExternalSessionKey key) throws Exception {
		return videoStore.getVideoInformation(key.toString());
	}
	

}
