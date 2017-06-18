package com.aimmac23.hub.videostorage;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Context information for AWS S3 stored videos
 */
public class CloudS3StoredVideoInfoContext implements StoredVideoInfoContext {
	private final LocationAwareS3Object video;

	private static final Logger log = Logger.getLogger(CloudS3StoredVideoInfoContext.class.getName());

	CloudS3StoredVideoInfoContext(LocationAwareS3Object videoObject) {
		this.video = videoObject;
	}

	@Override
	public boolean isVideoFound() {
		try {
			return (video.getS3Object().getObjectContent().available() > 0);
		} catch (IOException e) {
			return false;
		}
	}

	@Override
	public Long getContentLengthIfKnown() {
		try {
			return (long) video.getS3Object().getObjectContent().available();
		} catch (IOException e) {
			return 0L;
		}
	}

	@Override
	public void close() {
		try {
			video.getS3Object().close();
		} catch (IOException e) {
			log.warning("Unable to close InputStream, this may lead to resource leaks!");
		}
	}

	@Override
	public Map<String, Object> additionalInformation() {
		return new HashMap<String, Object>(Collections.singletonMap("path",
				String.format("https://%s.s3.amazonaws.com/%s", video.getBucketName(), video.getFileName())));
	}
}
