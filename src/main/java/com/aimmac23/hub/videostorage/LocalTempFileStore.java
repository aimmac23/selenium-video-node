package com.aimmac23.hub.videostorage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.exec.StreamPumper;
import org.openqa.grid.internal.ExternalSessionKey;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

/**
 * A plugin to temporarily store videos in temporary local files. Videos are deleted when
 * we store over a certain number of them (currently 200) to save on disk space. Use this plugin if:
 * 
 * <ul>
 * <li>You are just getting your grid setup, and you don't want the added complexity</li>
 * <li>You don't mind the grid forgetting about videos over restarts</li>
 * <li>You don't need the grid to store videos to be stored for very long</li>
 * </ul>
 * @author Alasdair Macmillan
 *
 */
public class LocalTempFileStore implements IVideoStore {
	
	private static final Logger log = Logger.getLogger(LocalTempFileStore.class.getName());

	
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
	
	@Override
	public void storeVideo(InputStream videoStream, String mimeType,
			String sessionId,  SessionInfoBean infoBean) throws Exception {
		File outputFile = File.createTempFile("screencast", ".webm");
		FileOutputStream outputStream = new FileOutputStream(outputFile);
		try {
			new StreamPumper(videoStream, outputStream).run();
		}
		finally {
			outputStream.close();
		}
		
		availableVideos.put(new ExternalSessionKey(sessionId), outputFile);
		log.info("Successfully retrieved video for session: " + sessionId + " and temporarily stashed it at: " + outputFile);
	
	}

	@Override
	public LocalTempFileDownloadContext retrieveVideo(String sessionId) throws Exception {
		File file = availableVideos.getIfPresent(new ExternalSessionKey(sessionId));
		if(file != null && file.exists() && file.isFile()) {
			return new LocalTempFileDownloadContext(file);
		}
		
		return new LocalTempFileDownloadContext(null);
	}
	
	@Override
	public StoredVideoInfoContext getVideoInformation(String sessionId)
			throws Exception {
		// XXX: Abuse the class heirarchy for this one
		return retrieveVideo(sessionId);
	}
	
	@Override
	public String getVideoStoreTypeIdentifier() {
		return "TEMP_FILE";
	}
	
	private static class LocalTempFileDownloadContext implements StoredVideoDownloadContext, StoredVideoInfoContext {
		
		private File file;
		private FileInputStream stream;

		public LocalTempFileDownloadContext(File file) throws FileNotFoundException {
			this.file = file;
			if(file != null) {
				stream = new FileInputStream(file);	
			}
			else {
				stream = null;
			}
		}

		@Override
		public boolean isVideoFound() {
			return file != null;
		}

		@Override
		public InputStream getStream() throws IOException {
			return stream;
		}

		@Override
		public Long getContentLengthIfKnown() {
			return new Long(file.length());
		}

		@Override
		public void close() {
			try {
				stream.close();
			} catch (IOException e) {
				log.log(Level.WARNING, "Could not close file: " + file, e);
			}
		}

		@Override
		public Map<String, Object> additionalInformation() {
			return Collections.emptyMap();
		}
	}

}
