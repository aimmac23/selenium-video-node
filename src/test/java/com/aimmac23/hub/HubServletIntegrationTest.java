package com.aimmac23.hub;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;

import java.util.Arrays;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.socket.PortFactory;
import org.openqa.grid.internal.utils.configuration.GridHubConfiguration;
import org.openqa.grid.web.Hub;
import org.testng.Assert;

import com.aimmac23.http.HttpClientFactory;
import com.aimmac23.hub.servlet.HubVideoDownloadServlet;
import com.aimmac23.hub.servlet.HubVideoInfoServlet;

public class HubServletIntegrationTest {
	
	private ClientAndServer mockServer;
	private Hub hub;
	private int hubPort;
	private HttpClient httpClient;

	private int mockClientPort;
	
	@Before
	public void setup() throws Exception {
		hubPort = PortFactory.findFreePort();
		mockClientPort = PortFactory.findFreePort();

		// don't strictly need this for the test, but it absorbs any HTTP requests the Hub makes
	    mockServer = startClientAndServer(mockClientPort);
	    GridHubConfiguration hubConfig = new GridHubConfiguration();
	    
	    hubConfig.servlets = Arrays.asList(HubVideoDownloadServlet.class.getCanonicalName(),
	    		HubVideoInfoServlet.class.getCanonicalName());
	    hubConfig.port = hubPort;
	    // As of 
	    hubConfig.host = "127.0.0.1";
	    
	    
	    hub = new Hub(hubConfig);
	    hub.start();
	    
	    httpClient = new HttpClientFactory().getHttpClient();

	}
	
	@After
	public void tearDown() throws Exception {
	    mockServer.stop();
	    hub.stop();
	}
	
	@Test
	public void testVideoInfoServletInitialised() throws Exception {
		HttpGet get = new HttpGet("http://127.0.0.1:" + hubPort + "/grid/admin/HubVideoInfoServlet/");
		 HttpResponse response = httpClient.execute(get);
		 try {
			 Assert.assertEquals(response.getStatusLine().getStatusCode(), 400);
			 String entity = EntityUtils.toString(response.getEntity());
			 // we didn't pass a session ID
			 Assert.assertTrue(entity.contains("Missing parameter"));
		 } finally {
			 get.releaseConnection();
		 }
	}
	
	@Test
	public void testVideoDownloadServletInitialised() throws Exception {
		HttpGet get = new HttpGet("http://127.0.0.1:" + hubPort + "/grid/admin/HubVideoDownloadServlet/");
		 HttpResponse response = httpClient.execute(get);
		 try {
			 Assert.assertEquals(response.getStatusLine().getStatusCode(), 400);
			 String entity = EntityUtils.toString(response.getEntity());
			 // we didn't pass a session ID
			 Assert.assertTrue(entity.contains("Missing parameter"));
		 } finally {
			 get.releaseConnection();
		 }
	}
	
	

}
