package com.aimmac23.node;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class RobotScreenshotSource implements ScreenshotSource {

	private Robot robot;

	public RobotScreenshotSource() throws Exception {
		robot = new Robot();
	}
	
	@Override
	
	public int[] getScreenshot() {
		BufferedImage image = robot.createScreenCapture(getScreenSize());

		return ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
	}

	@Override
	public int getWidth() {
		return getScreenSize().width;
	}

	@Override
	public int getHeight() {
		return getScreenSize().height;
	}

	protected Rectangle getScreenSize() {
		//XXX: This probably won't work with multiple monitors
		return GraphicsEnvironment.getLocalGraphicsEnvironment().
				getDefaultScreenDevice().getDefaultConfiguration().getBounds();
		
	}
}
