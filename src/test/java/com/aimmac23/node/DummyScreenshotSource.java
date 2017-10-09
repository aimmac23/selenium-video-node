package com.aimmac23.node;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import com.aimmac23.exception.MissingFrameException;
import com.aimmac23.node.jna.EncoderInterface;
import com.aimmac23.node.jna.JnaLibraryLoader;
import com.sun.jna.Pointer;

public class DummyScreenshotSource implements ScreenshotSource {

	int redness = 0;
	private EncoderInterface encoderInterface;
	
	public DummyScreenshotSource(EncoderInterface encoderInterface) {
		this.encoderInterface = encoderInterface;
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
		return 800;
	}

	@Override
	public int getHeight() {
		return 600;
	}

	@Override
	public String getSourceName() {
		return "Dummy";
	}

}
