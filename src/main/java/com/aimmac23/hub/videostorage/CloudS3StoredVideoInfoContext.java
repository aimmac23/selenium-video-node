package com.aimmac23.hub.videostorage;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.Region;

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
	private final Region s3Region;

	CloudS3StoredVideoInfoContext(ObjectMetadata metadata, String bucketName, String videoFileName,
	                              Region s3Region) {
		this.metadata = metadata;
		this.bucketName = bucketName;
		this.videoFileName = videoFileName;
		this.s3Region = s3Region;
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
		// AWS S3 endpoint for US standard does not have the region identifier
		// in it for legacy reasons. All other regions contain the identifier in the URI.
		// Examples:
		// 1. eu-west-1
		//    https://s3-eu-west-1.amazonaws.com/<bucketName>/<key>
		// 2. us-east-1 (default)
		//    https://<bucketName>.amazonaws.com/<key>
		String endpointUri = String.format("https://%s.s3.amazonaws.com/%s", bucketName, videoFileName);
		if (!s3Region.equals(Region.US_Standard)) {
			endpointUri = String.format("https://s3-%s.amazonaws.com/%s/%s", s3Region, bucketName, videoFileName);
		}

		return new HashMap<String, Object>(Collections.singletonMap("path", endpointUri));
	}
}
