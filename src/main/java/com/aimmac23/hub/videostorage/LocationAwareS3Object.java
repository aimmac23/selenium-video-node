package com.aimmac23.hub.videostorage;

import com.amazonaws.services.s3.model.S3Object;

/**
 * Light wrapper over an AWS S3 object that also includes the bucket and file name
 * in order to build a URI for the video uploaded to S3
 */
public final class LocationAwareS3Object {
	private final S3Object s3Object;
	private final String bucketName;
	private final String fileName;

	public LocationAwareS3Object(S3Object videoObject, String bucketName, String fileName) {
		this.s3Object = videoObject;
		this.bucketName = bucketName;
		this.fileName = fileName;
	}

	public S3Object getS3Object() {
		return s3Object;
	}

	public String getBucketName() {
		return bucketName;
	}

	public String getFileName() {
		return fileName;
	}
}
