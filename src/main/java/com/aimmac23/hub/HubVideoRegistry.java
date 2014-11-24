package com.aimmac23.hub;

import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.openqa.grid.internal.ExternalSessionKey;
import org.openqa.grid.internal.TestSession;
import org.openqa.selenium.remote.internal.HttpClientFactory;

import com.aimmac23.hub.videostorage.IVideoStore;
import com.aimmac23.hub.videostorage.LocalTempFileStore;
import com.aimmac23.hub.videostorage.SessionInfoBean;
import com.aimmac23.hub.videostorage.StoredVideoDownloadContext;
import com.aimmac23.hub.videostorage.StoredVideoInfoContext;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

@SuppressWarnings("unchecked")
public class HubVideoRegistry {
	
	private static final Logger log = Logger.getLogger(HubVideoRegistry.class.getName());

	private static IVideoStore videoStore;
	
	private static Cache<String, VideoFuture> stoppingSessions;
	
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
		
		stoppingSessions = CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).build();
	}
	
	public static void declareSessionStopping(TestSession session) {
		stoppingSessions.put(session.getExternalKey().getKey(), new VideoFuture());	
	}

	public static void copyVideoToHub(TestSession session, String pathKey, URL remoteHost) {
		String serviceUrl = remoteHost + "/extra/VideoRecordingControlServlet";

		SessionInfoBean infoBean = new SessionInfoBean(session);
		
		ExternalSessionKey key = session.getExternalKey();
		
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
			
			Header contentType = response.getFirstHeader("Content-Type");
			if(contentType != null && !"video/webm".equals(contentType.getValue())) {
				log.log(Level.SEVERE, "Incorrect 'Content-Type' header when downloading video - "
						+ "check that the control servlet is correctly setup for node: " + remoteHost);
			}
			else {
				videoStore.storeVideo(response.getEntity().getContent(), response.getEntity().getContentLength(), "video/webm", key.toString(), infoBean);	
			}
			
        }
		catch(Exception e) {
			log.warning("Could not download video, exception caught: " + e.getMessage());
			e.printStackTrace();
		}
        finally {
        	r.releaseConnection();
        	VideoFuture videoFuture = stoppingSessions.getIfPresent(key.getKey());
        	if(videoFuture != null) {
        		videoFuture.setDone();
        	}
        }
	}
	private static void checkVideoIsDone(ExternalSessionKey key) throws Exception {
		VideoFuture videoFuture;
		synchronized(HubVideoRegistry.class) {
			 videoFuture = stoppingSessions.getIfPresent(key.toString());
		}
		
		if(videoFuture != null) {
			videoFuture.get();
		}
	}
	
	public static StoredVideoDownloadContext getVideoForSession(ExternalSessionKey key) throws Exception {
		checkVideoIsDone(key);
		return videoStore.retrieveVideo(key.toString());
	}
	
	public static StoredVideoInfoContext getVideoInfoForSession(ExternalSessionKey key) throws Exception {
		checkVideoIsDone(key);
		return videoStore.getVideoInformation(key.toString());
	}
	
	public static String getVideoStoreType() {
		return videoStore.getVideoStoreTypeIdentifier();
	}
	
	private static class VideoFuture implements Future<Void> {

		private boolean isDone = false;
		
		public synchronized void setDone() {
			this.isDone = true;
			
			this.notifyAll();
		}
		
		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			// cancelling not allowed
			return false;
		}

		@Override
		public boolean isCancelled() {
			// we never cancel this
			return false;
		}

		@Override
		public boolean isDone() {
			return isDone;
		}

		@Override
		public synchronized Void get() throws InterruptedException, ExecutionException {
			while(!isDone) {
				this.wait();
			}
			return null;
		}

		@Override
		public synchronized Void get(long timeout, TimeUnit unit)
				throws InterruptedException, ExecutionException,
				TimeoutException {
			if(!isDone) {
				this.wait(unit.toMillis(timeout));
			}
			return null;
		}
	}
}
