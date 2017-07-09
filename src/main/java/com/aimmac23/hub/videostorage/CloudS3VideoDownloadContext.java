package com.aimmac23.hub.videostorage;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * DownloadContext for AWS S3 stored videos
 */
public class CloudS3VideoDownloadContext implements StoredVideoDownloadContext {
	private LocationAwareS3Object video;

	private static final Logger log = Logger.getLogger(CloudS3VideoDownloadContext.class.getName());

	CloudS3VideoDownloadContext(LocationAwareS3Object video) {
		this.video = video;
	}

	@Override
	public boolean isVideoFound() {
		return (video.getS3Object() != null);
	}

	@Override
	public InputStream getStream() throws IOException {
		return video.getS3Object().getObjectContent();
	}

	@Override
	public Long getContentLengthIfKnown() {
		return video.getS3Object().getObjectMetadata().getContentLength();
	}

	@Override
	public void close() {
		try {
			video.getS3Object().close();
		} catch (IOException e) {
			log.warning("Unable to close InputStream, this may lead to resource leaks!");
		}
	}
}
