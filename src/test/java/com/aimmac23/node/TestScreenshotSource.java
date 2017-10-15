package com.aimmac23.node;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import com.aimmac23.exception.MissingFrameException;
import com.aimmac23.node.jna.EncoderInterface;
import com.aimmac23.node.jna.JnaLibraryLoader;
import com.sun.jna.Pointer;

/**
 * Creates a screenshot suitable for testing video encoding without worrying where it came from.
 * 
 * Still depends on native dependencies to do the YUV conversion.
 * 
 * @author aim
 *
 */
public class TestScreenshotSource implements ScreenshotSource {

	int redness = 0;
	private EncoderInterface encoderInterface;
	private int width;
	private int height;
	
	public TestScreenshotSource(EncoderInterface encoderInterface, int width, int height) {
		this.encoderInterface = encoderInterface;
		this.width = width;
		this.height = height;
	}
	
	@Override
	public void doStartupSanityChecks() {		
	}

	@Override
	public int applyScreenshot(Pointer encoderContext) throws MissingFrameException {
		redness += 3;
		// TODO: Is the type correct?
		BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);

		Graphics2D graphics = image.createGraphics();
		
		graphics.setColor(new Color(redness % 255, 255, 255));
		graphics.fillRect(0, 0, getWidth(), getHeight());
		
		int[] screenshotData = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
		
		return encoderInterface.convert_frame(encoderContext, screenshotData);
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public String getSourceName() {
		return "Dummy";
	}

}
