package com.aimmac23.hub.videostorage;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;

import java.io.InputStream;

/**
 * A plugin to store videos on Amazon Web Services S3 cloud storage.
 *
 * This should be used when:
 * <ul>
 * <li>You would like to store videos for a longer period of time for auditing</li>
 * <li>Your Selenium Grid nodes do not have sufficient disk space to store videos</li>
 * <li>You would like to retrieve the videos on your Selenium Grid Nodes after each session.
 * This can be achieved with file storage but using S3 is much simpler as the nodes
 * can just download the videos via absolute URLs from S3</li>
 * <li>You don't want the Hub to forget about videos over restarts</li>
 * </ul>
 *
 * This Store depends on several environment variables:
 * <ul>
 *   <li>AWS_REGION: One of the multiple AWS regions supported, this must
 *   be specified in order to upload the videos to the correct bucket</li>
 *   <li>AWS_BUCKET_NAME: The name of the AWS bucket configured from AWS S3</li>
 *   <li>AWS_ACCESS_KEY_ID: The AWS access key credential, configurable from AWS IAM</li>
 *   <li>AWS_SECRET_ACCESS_KEY: The AWS secret key credential, configurable from AWS IAM</li>
 * </ul>
 *
 * @author Ovidiu Bute
 */
public class CloudS3VideoStore implements IVideoStore {
	private AmazonS3 client;
	private String bucketName;

	public CloudS3VideoStore() throws Exception {
		client = AmazonS3ClientBuilder.defaultClient();
		bucketName = System.getProperty("AWS_BUCKET_NAME");

		assertEnvironmentVars();
	}

	@Override
	public void storeVideo(InputStream videoStream, long contentLength, String mimeType, String sessionId, SessionInfoBean sessionInfo) throws Exception {
		// Set correct content-length and content-type headers
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(contentLength);
		metadata.setContentType(mimeType);

		// Make the videos publicly readable
		PutObjectRequest request = new PutObjectRequest(bucketName, sessionId, videoStream, metadata);
		request.setCannedAcl(CannedAccessControlList.PublicRead);

		// Upload the object
		client.putObject(request);
	}

	@Override
	public StoredVideoDownloadContext retrieveVideo(String sessionId) throws Exception {
		final S3Object videoObject = client.getObject(bucketName, sessionId);
		return new CloudS3VideoDownloadContext(new LocationAwareS3Object(videoObject, bucketName, sessionId));
	}

	@Override
	public StoredVideoInfoContext getVideoInformation(String sessionId) throws Exception {
		final S3Object videoObject = client.getObject(bucketName, sessionId);
		return new CloudS3StoredVideoInfoContext(new LocationAwareS3Object(videoObject, bucketName, sessionId));
	}

	@Override
	public String getVideoStoreTypeIdentifier() {
		return "CLOUD_AWS_S3";
	}

	private void assertEnvironmentVars() throws Exception {
		assertEnvironmentVar("AWS_BUCKET_NAME");
		assertEnvironmentVar("AWS_REGION");
		assertEnvironmentVar("AWS_ACCESS_KEY_ID");
		assertEnvironmentVar("AWS_SECRET_ACCESS_KEY");
	}

	/**
	 * Checks the environment for a variable and throws if not found
	 * @param varName Name of the environment variable
	 * @throws Exception
	 */
	private void assertEnvironmentVar(final String varName) throws Exception {
		String var = System.getProperty(varName);

		if (var == null || var.isEmpty()) {
			throw new Exception(String.format("Invalid value for %s! " +
					"You must configure this as an environment variable!", varName));
		}
	}
}
