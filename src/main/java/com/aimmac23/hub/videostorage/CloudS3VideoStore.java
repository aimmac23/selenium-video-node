package com.aimmac23.hub.videostorage;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * A plugin to store videos on Amazon Web Services S3 cloud storage.
 *
 * This should be used when:
 * <ul>
 * <li>You would like to store videos for a longer period of time for auditing</li>
 * <li>Your Selenium Grid nodes do not have sufficient disk space to store videos</li>
 * <li>You would like to retrieve the videos on your Selenium Grid Nodes after each session.
 * This can be achieved with file storage but using S3 is much simpler as the nodes
 * can just download the videos via absolute URLs from S3.</li>
 * <li>You don't want the Hub to forget about videos over restarts.</li>
 * </ul>
 *
 * This Store depends on several environment variables:
 * <ul>
 *   <li>AWS_REGION: One of the multiple AWS regions supported. If you have not selected
 *   any region while configuring your bucket then it has been created in US standard
 *   and has the following identifier: "us-east-1". This must be specified in order to
 *   upload the videos to the correct bucket.
 *   @see com.amazonaws.regions.Regions for all the supported region identifiers.</li>
 *   <li>AWS_BUCKET_NAME: The name of the AWS bucket configured from AWS S3.</li>
 *   <li>AWS_ACCESS_KEY_ID: The AWS access key credential, configurable from AWS IAM.</li>
 *   <li>AWS_SECRET_ACCESS_KEY: The AWS secret key credential, configurable from AWS IAM.</li>
 * </ul>
 *
 * @author Ovidiu Bute
 */
public class CloudS3VideoStore implements IVideoStore {
	public static final Logger log = Logger.getLogger(CloudS3VideoStore.class.getName());
	private AmazonS3 client;
	private String bucketName;
	private String awsRegion;

	/**
	 * CloudS3VideoStore constructor
	 */
	public CloudS3VideoStore() {
		// Fail early if environment variables have not been defined
		assertEnvironmentVars();

		// AWS client automatically picks up the env. variables
		client = AmazonS3ClientBuilder.defaultClient();

		bucketName = System.getenv("AWS_BUCKET_NAME");
		awsRegion = System.getenv("AWS_REGION");
	}

	@Override
	public void storeVideo(InputStream videoStream, long contentLength, String mimeType, String sessionId,
	                       SessionInfoBean sessionInfo) throws Exception {
		// Set correct content-length and content-type headers
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(contentLength);
		metadata.setContentType(mimeType);

		// Make the videos publicly readable
		PutObjectRequest request = new PutObjectRequest(bucketName,
				LocationAwareS3Object.formatFileName(sessionId), videoStream, metadata);
		request.setCannedAcl(CannedAccessControlList.PublicRead);

		// Upload the object
		log.fine(String.format("Uploading video with sessionId=%s to AWS S3 bucket=%s", sessionId, bucketName));
		client.putObject(request);
	}

	@Override
	public StoredVideoDownloadContext retrieveVideo(String sessionId) throws Exception {
		log.fine(String.format("Downloading video with sessionId=%s from AWS S3 bucket=%s", sessionId, bucketName));

		final S3Object videoObject = client.getObject(bucketName, LocationAwareS3Object.formatFileName(sessionId));
		return new CloudS3VideoDownloadContext(new LocationAwareS3Object(videoObject, bucketName, sessionId));
	}

	@Override
	public StoredVideoInfoContext getVideoInformation(String sessionId) throws Exception {
		log.fine(String.format("Retrieving video metadata with sessionId=%s from AWS S3 bucket=%s", sessionId, bucketName));

		final ObjectMetadata objectMetadata = client.getObjectMetadata(bucketName, LocationAwareS3Object.formatFileName(sessionId));
		return new CloudS3StoredVideoInfoContext(objectMetadata, bucketName, LocationAwareS3Object.formatFileName(sessionId),
				Region.fromValue(awsRegion));
	}

	@Override
	public String getVideoStoreTypeIdentifier() {
		return "CLOUD_AWS_S3";
	}

	/**
	 * Checks the environment for a preset list of variables and throws RuntimeException if
	 * any one of them is not found.
	 */
	private void assertEnvironmentVars() {
		assertEnvironmentVars(Arrays.asList("AWS_REGION", "AWS_BUCKET_NAME", "AWS_ACCESS_KEY_ID", "AWS_SECRET_ACCESS_KEY"));
	}

	/**
	 * Checks the environment for a list of variables and throws if any of one them is not found.
	 * @param varNames List of environment variable names to validate.
	 */
	private void assertEnvironmentVars(List<String> varNames) {
		for (String varName : varNames) {
			String var = System.getenv(varName);

			if (var == null || var.isEmpty()) {
				throw new RuntimeException(String.format("Invalid value for %s! " +
						"You must configure this as an environment variable!", varName));
			}
		}
	}
}
