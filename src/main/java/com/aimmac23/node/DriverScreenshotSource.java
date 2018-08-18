package com.aimmac23.node;

import java.awt.DisplayMode;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import javax.imageio.ImageIO;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import com.aimmac23.exception.MissingFrameException;
import com.aimmac23.node.jna.EncoderInterface;
import com.aimmac23.node.jna.JnaLibraryLoader;
import com.sun.jna.Pointer;

public class DriverScreenshotSource implements ScreenshotSource {

	private TakesScreenshot driver;

	public DriverScreenshotSource(WebDriver driver) {
		// TODO: Error handling
		this.driver = (TakesScreenshot) driver;
	}
	
	@Override
	public void doStartupSanityChecks() {
		
	}
	
	// From https://stackoverflow.com/a/9417836
	public static BufferedImage resize(BufferedImage img, int newW, int newH) { 
	    Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_FAST);
	    BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

	    Graphics2D g2d = dimg.createGraphics();
	    g2d.drawImage(tmp, 0, 0, null);
	    g2d.dispose();

	    return dimg;
	}
	
	@Override
	public int applyScreenshot(Pointer encoderContext) throws MissingFrameException {
		byte[] pngBytes = driver.getScreenshotAs(OutputType.BYTES);
		
		try {
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(pngBytes));
			
			BufferedImage resized = resize(image, getWidth(), getHeight());
			
			int[] screenshotData = ((DataBufferInt)resized.getRaster().getDataBuffer()).getData();			
			
			
			if(!getScreenSize().equals(new Rectangle(resized.getWidth(), resized.getHeight()))) {
				throw new IllegalStateException("Unequal screenshot sizes: screen: " + getScreenSize() 
					+ ", image: " + resized.getWidth() + "x" + resized.getHeight());
			}
			EncoderInterface encoder = JnaLibraryLoader.getEncoder();

			return encoder.convert_frame(encoderContext, screenshotData);
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int getWidth() {
		// TODO: Making some assumptions here about whether the screenshot is the same size as the screen
		return getScreenSize().width;
	}

	@Override
	public int getHeight() {
		// TODO: Making some assumptions here about whether the screenshot is the same size as the screen
		return getScreenSize().height;
	}

	protected Rectangle getScreenSize() {
		//XXX: This probably won't work with multiple monitors
		DisplayMode displayMode = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode();
		return new Rectangle(displayMode.getWidth(), displayMode.getHeight());
		
	}

	@Override
	public String getSourceName() {
		return "Selenium Driver API";
	}

}
