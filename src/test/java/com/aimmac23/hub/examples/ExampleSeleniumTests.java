package com.aimmac23.hub.examples;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Example Selenium tests for the video grid. A lot of the interesting functionality
 * is in {@link AbstractVideoSeleniumTest}
 * 
 * @author Alasdair Macmillan
 *
 */
@Ignore
public class ExampleSeleniumTests extends AbstractVideoSeleniumTest {
	
	// change this to point to your own grid hub
	private static String HUB_URL = "http://127.0.0.1:4444/wd/hub";
	
	private static String LINK_TEXT = "Selenium - Web Browser Automation";

	@Override
	protected RemoteWebDriver createWebDriver() {
		
		URL hubUrl;
		try {
			hubUrl = new URL(HUB_URL);
			
			return new RemoteWebDriver(hubUrl, DesiredCapabilities.firefox());
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("Could not construct Hub URL", e);
		}
	}
	
	@Test
	public void successfulTest() throws Exception {
		driver.manage().window().maximize();

		driver.get("http://www.google.com");
		// Google seem to like obscure element IDs
		driver.findElementById("gbqfq").sendKeys("selenium");
		driver.findElementById("gbqfb").click();

		// we need to wait for the search results to appear
		new WebDriverWait(driver, 5).until(ExpectedConditions.presenceOfElementLocated(By.linkText(LINK_TEXT)));

		driver.findElementByLinkText(LINK_TEXT).click();
		
		// the text so happens to be the same as the window title
		assertEquals(LINK_TEXT, driver.getTitle());
	}
	
	@Test
	public void failingTest() throws Exception {
		driver.manage().window().maximize();

		driver.get("http://www.google.com");
		
		// Google seem to like obscure element IDs
		driver.findElementById("gbqfq").sendKeys("news");
		driver.findElementById("gbqfb").click();

		// we need to wait for the search results to appear
		new WebDriverWait(driver, 5).until(ExpectedConditions.presenceOfElementLocated(By.linkText(LINK_TEXT)));
		
		// BOOM! We will not find the above, unless today's news is quite strange.

	}

}
