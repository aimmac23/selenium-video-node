package com.mooo.aimmac23.hub.videostorage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.exec.StreamPumper;

/**
 * A plugin to store permamently videos in a local filesystem directory.
 * 
 * This should be used when:
 * <ul>
 *   <li>You don't want your Selenium Grid to delete videos</li>
 *   <li>You don't want your Selenium Grid to forget about videos over restarts</li>
 *   <li>You are trying to integrate with something that needs to access videos directly 
 *   (NOT through the Hub), and you so happen to share a filesystem with that thing. </li>
 *   </ul>
 *   
 * @author Alasdair Macmillan
 *
 */
public class LocalFileVideoStore implements IVideoStore {
	
	private static final Logger log = Logger.getLogger(LocalFileVideoStore.class.getName());
	
	private File directory;

	public LocalFileVideoStore() {
		
		String path = System.getProperty("video.path");
		
		if(path == null) {
			throw new IllegalArgumentException("'video.path' is not defined - "
					+ "you need to pass -Dvideo.path=<directory> to use " + this.getClass().getName());
		}
		directory = new File(path);
		
		if(!directory.exists()) {
			directory.mkdirs();
		}
		
		if(!directory.exists()) {
			throw new IllegalStateException("Target directory does not exist: " + directory);
		}
		
		if(!directory.isDirectory()) {
			throw new IllegalStateException("Target directory is not a directory: " + directory);
		}
		
		if(!directory.canWrite()) {
			throw new IllegalStateException("Target directory is now writeable: " + directory);
		}
	}

	@Override
	public void storeVideo(InputStream videoStream, String mimeType,
			String sessionId, Map<String, Object> requestedCapabilities, 
			Map<String, Object> nodeCapabilities) throws Exception {
		
		File target = new File(directory, sessionId + ".webm");
		
		FileOutputStream fileStream = new FileOutputStream(target);
		
		try {
			new StreamPumper(videoStream, fileStream).run();			
		}
		finally {
			fileStream.close();
		}
		
		log.info("Successfully written video file for session to: " + target);
	}

	@Override
	public LocalFileVideoStoreDownloadContext retrieveVideo(String sessionId)
			throws Exception {
		
		File target = new File(directory, sessionId + ".webm");
		
		if(target.exists() && target.isFile() && target.canRead()) {
			return new LocalFileVideoStoreDownloadContext(target);
		}
		
		log.info("File not found, or is not readable for sessionId: " + sessionId);
		return new LocalFileVideoStoreDownloadContext(null);
	}

	@Override
	public StoredVideoInfoContext getVideoInformation(String sessionId)
			throws Exception {
		return retrieveVideo(sessionId);
	}
	
	private static class LocalFileVideoStoreDownloadContext implements StoredVideoDownloadContext, StoredVideoInfoContext {
		
		private File file;
		private FileInputStream stream;
		private String canonicalPath;

		public LocalFileVideoStoreDownloadContext(File file) throws FileNotFoundException, IOException {
			this.file = file;
			if(file != null && file.exists()) {
				stream = new FileInputStream(file);
				this.canonicalPath = file.getCanonicalPath();
			}
			else {
				stream = null;
				canonicalPath = null;
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
			return new HashMap<String, Object>(Collections.singletonMap("path", canonicalPath));
		}
	}
}

