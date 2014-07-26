package com.aimmac23.node;

import java.io.File;

import com.aimmac23.node.jna.EncoderInterface;
import com.aimmac23.node.jna.JnaLibraryLoader;
import com.aimmac23.node.jna.XvfbScreenshotInterface;
import com.sun.jna.Pointer;

public class XvfbFileScreenshotSource implements ScreenshotSource {
	
	private XvfbScreenshotInterface xvfbInterface;
	private Pointer interfacePointer;

	public XvfbFileScreenshotSource(File path) {
		xvfbInterface = JnaLibraryLoader.getXvfbInterface();
		
		interfacePointer = xvfbInterface.xvfb_interface_init(path.getAbsolutePath());
		
		if(interfacePointer == null) {
			throw new IllegalStateException("Could not create xvfb interface");
		}
	}

	@Override
	public int applyScreenshot(Pointer encoderContext) {
		EncoderInterface encoder = JnaLibraryLoader.getEncoder();
		Pointer screenshotData = xvfbInterface.xvfb_interface_getScreenshot(interfacePointer);
		return encoder.convert_frame(encoderContext, screenshotData);
	}

	@Override
	public int getWidth() {
		return xvfbInterface.xvfb_interface_getWidth(interfacePointer);
	}

	@Override
	public int getHeight() {
		return xvfbInterface.xvfb_interface_getHeight(interfacePointer);
	}

	@Override
	public void doStartupSanityChecks() {
		
		String result = xvfbInterface.xvfb_interface_sanityChecks(interfacePointer);
		if(result != null) {
			throw new IllegalStateException("Could not use xvfb accelleration: " + result);
		}
	}

}
