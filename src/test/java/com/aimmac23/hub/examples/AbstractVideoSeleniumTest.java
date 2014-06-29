package com.aimmac23.hub.examples;

import java.net.URL;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.openqa.selenium.remote.HttpCommandExecutor;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;

/**
 * Base class for some Selenium Tests. Sets up the WebDriver, and wraps the
 * running test in a JUnit rule that wraps failing test exceptions in another exception
 * which contains a URL for the test video.
 * 
 * This is a easy way to get the video links into your JUnit/CI integration reports, if a
 * bit nasty.
 * 
 * @author Alasdair Macmillan
 *
 */
public abstract class AbstractVideoSeleniumTest extends Assert {

	protected RemoteWebDriver driver;
	private SessionId sessionId;

	@Before
	public void setUp() {
		driver = createWebDriver();
		sessionId = driver.getSessionId();
	}
	
	@After
	public void tearDown() {
		// could be null, if we failed to create this
		if(driver != null) {
			driver.quit();
		}
	}
		
	protected abstract RemoteWebDriver createWebDriver();
	
	@Rule
	public TestRule createVideoExceptionWrapRule() {
		return new TestRule() {

			@Override
			public Statement apply(final Statement base, Description description) {
					return new Statement() {

						@Override
						public void evaluate() throws Throwable {
							try {
								// this bit actually runs the test.
								base.evaluate();
							}
							catch(Throwable e) {
								if(driver != null) {
									URL remoteServer = ((HttpCommandExecutor)driver.getCommandExecutor()).getAddressOfRemoteServer();
									
									// XXX: We should really verify with an HTTP HEAD request that the video exists. We can only
									// do that after the test has finished, which is a bit too late for this test rule.
									
									URL videoUrl = new URL(remoteServer, "/grid/admin/HubVideoDownloadServlet/?sessionId=" + sessionId);
									
									// Selenium exceptions can be quite verbose - just give the classname
									String message = e.getClass().getName();
									
									throw new RuntimeException("Test failed due to exception: " + message + ". Video available at " + videoUrl, e);
								}
								else {
									// we failed to get a Webdriver - re-throw original exception
									throw e;
																	
								}
							}
						}
					};
			}
		};
	}
}
