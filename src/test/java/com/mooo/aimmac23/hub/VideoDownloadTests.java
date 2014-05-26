package com.mooo.aimmac23.hub;

import junit.framework.Assert;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Test;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;


public class VideoDownloadTests extends Assert {
	
	/**
	 * To simplify client-side coding, the video should be immediately available for download if 
	 * available. At the time of writing, this work is not done synchronously with the session tear-down,
	 * which complicates video retrieval and storage.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testVideoImmediatelyAvailable() throws Exception {
		DesiredCapabilities caps = DesiredCapabilities.firefox();
		
		RemoteWebDriver driver = new RemoteWebDriver(caps);
		System.out.println("Session: " + driver.getSessionId().toString());
		driver.get("http://www.google.com");
		
		SessionId sessionId = driver.getSessionId();

		driver.quit();	
		
		CloseableHttpClient httpClient = HttpClientBuilder.create().build();
		HttpGet httpGet = new HttpGet("http://127.0.0.1:4444/grid/admin/HubVideoDownloadServlet?sessionId=" + sessionId.toString());
		
		CloseableHttpResponse response = httpClient.execute(httpGet);
		
		Assert.assertEquals(200, response.getStatusLine().getStatusCode());
	}

}
