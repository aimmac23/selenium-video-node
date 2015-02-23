package com.aimmac23.node;

import java.awt.DisplayMode;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

import com.aimmac23.node.jna.EncoderInterface;
import com.aimmac23.node.jna.JnaLibraryLoader;
import com.sun.jna.Pointer;

public class RobotScreenshotSource implements ScreenshotSource {

	private Robot robot;

	public RobotScreenshotSource() throws Exception {
		robot = new Robot();
	}
	
	@Override
	public int applyScreenshot(Pointer encoderContext) {
		BufferedImage image = takeScreenshot();

		int[] screenshotData = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
		
		EncoderInterface encoder = JnaLibraryLoader.getEncoder();

		return encoder.convert_frame(encoderContext, screenshotData);
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
		DisplayMode displayMode = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode();
		return new Rectangle(displayMode.getWidth(), displayMode.getHeight());
		
	}
	
	public BufferedImage takeScreenshot(){
		return robot.createScreenCapture(getScreenSize());
	}

	@Override
	public void doStartupSanityChecks() {
		// test to make assert that the bit depth has 8 bits per pixel
		
		ColorModel colorModel = GraphicsEnvironment.getLocalGraphicsEnvironment().
		getDefaultScreenDevice().getDefaultConfiguration().getColorModel();
		
		int[] bitAllocations = colorModel.getComponentSize();
		
		// don't count alpha bits
		int bitDepth = bitAllocations[0] + bitAllocations[1] + bitAllocations[2];
		
		if(bitDepth != 24) {
			throw new IllegalStateException("Display colour depth incorrect (should be 8 bits of red, blue and green). Currently: " 
					+ bitAllocations[0] + "-" + bitAllocations[1] + "-" + bitAllocations[2]);
		}
	}
}
