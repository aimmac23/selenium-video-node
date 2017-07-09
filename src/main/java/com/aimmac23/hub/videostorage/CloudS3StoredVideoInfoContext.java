package com.aimmac23.hub.videostorage;

import com.amazonaws.services.s3.model.ObjectMetadata;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Context information for AWS S3 stored videos
 */
public class CloudS3StoredVideoInfoContext implements StoredVideoInfoContext {
	private final ObjectMetadata metadata;
	private final String bucketName;
	private final String videoFileName;

	CloudS3StoredVideoInfoContext(ObjectMetadata metadata, String bucketName, String videoFileName) {
		this.metadata = metadata;
		this.bucketName = bucketName;
		this.videoFileName = videoFileName;
	}

	@Override
	public boolean isVideoFound() {
		return metadata != null;
	}

	@Override
	public Long getContentLengthIfKnown() {
		return metadata.getContentLength();
	}

	@Override
	public void close() {}

	@Override
	public Map<String, Object> additionalInformation() {
		return new HashMap<String, Object>(Collections.singletonMap("path",
				String.format("https://%s.s3.amazonaws.com/%s", bucketName, videoFileName)));
	}
}
