package com.aimmac23.hub;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;

import java.util.Arrays;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.socket.PortFactory;
import org.mockserver.verify.VerificationTimes;
import org.openqa.grid.internal.utils.configuration.GridHubConfiguration;
import org.openqa.grid.web.Hub;
import org.openqa.selenium.remote.internal.HttpClientFactory;
import org.testng.Assert;

import com.aimmac23.hub.servlet.HubVideoDownloadServlet;
import com.aimmac23.hub.servlet.HubVideoInfoServlet;

public class HubProxyIntegrationTest {

	private static final HttpRequest RESET_REQUEST = HttpRequest.request("/extra/VideoRecordingControlServlet")
			.withMethod("POST").withQueryStringParameter("command", "reset");
	
	private ClientAndServer mockServer;
	private Hub hub;
	private int hubPort;
	private HttpClient httpClient;

	private int mockClientPort;
	
	@Before
	public void setup() throws Exception {
		hubPort = PortFactory.findFreePort();
		mockClientPort = PortFactory.findFreePort();

	    mockServer = startClientAndServer(mockClientPort);
	    GridHubConfiguration hubConfig = new GridHubConfiguration();
	    
	    hubConfig.servlets = Arrays.asList(HubVideoDownloadServlet.class.getCanonicalName(),
	    		HubVideoInfoServlet.class.getCanonicalName());
	    hubConfig.port = hubPort;
	    hubConfig.host = "127.0.0.1";

	    
	    hub = new Hub(hubConfig);
	    hub.start();
	    
	    httpClient = new HttpClientFactory().getHttpClient();

	}
	
	private void expectNodeServletCalls() {

		mockServer.when(RESET_REQUEST)
			.respond(org.mockserver.model.HttpResponse.response("OK").withStatusCode(200));
		
	}
	
	@Test
	public void testBasicRegistration() throws Exception {
		expectNodeServletCalls();
		
		String registrationRequest = String.format("{\"name\": \"test\", \"configuration\": {\"proxy\": \"com.aimmac23.hub.proxy.VideoProxy\", \"remoteHost\": \"http://127.0.0.1:%s\", \"capabilities\": [{\"browserName\": \"htmlunit\"}]}}", mockClientPort);
		HttpPost post = new HttpPost("http://127.0.0.1:" + hubPort + "/grid/register/");
		post.setEntity(new StringEntity(registrationRequest));
		
		HttpResponse response = httpClient.execute(post);
		try {
			Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
			EntityUtils.consumeQuietly(response.getEntity());
		} finally {
			post.releaseConnection();
		}
		Thread.sleep(2000);
		
		// check that the one HTTP call the VideoProxy makes on startup gets made
		mockServer.verify(RESET_REQUEST, VerificationTimes.once());
	}
}
