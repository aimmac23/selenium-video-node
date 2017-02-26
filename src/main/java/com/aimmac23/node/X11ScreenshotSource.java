package com.aimmac23.node;

import com.aimmac23.exception.MissingFrameException;
import com.aimmac23.node.jna.EncoderInterface;
import com.aimmac23.node.jna.JnaLibraryLoader;
import com.sun.jna.Pointer;

/**
 * Gets screenshots by calling X11 native code directly. Communicates with the X server over shared memory, so
 * this won't work if the X server doesn't support it, or the communication method (remote network connections) makes this impossible.
 * 
 * @author Alasdair Macmillan
 *
 */
public class X11ScreenshotSource implements ScreenshotSource {

	private com.aimmac23.node.jna.X11ScreenshotSource x11ScreenshotSource;
	private Pointer screenshotContext;

	public X11ScreenshotSource() {
		x11ScreenshotSource = JnaLibraryLoader.getX11ScreenshotSource();
	}
	
	@Override
	public void doStartupSanityChecks() {
		
		screenshotContext = x11ScreenshotSource.x11_screenshot_source_init();
		
		if(screenshotContext == null) {
			throw new IllegalStateException("Couldn't initialise X11 screenshot functionality. Check the logs for more information.");
		}
		
		// we have to release some shared memory resources on exit, otherwise the X server may leak memory
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				x11ScreenshotSource.x11_screenshot_source_destroy(screenshotContext);
				
			}
		}, "X11 Screenshot cleanup hook"));
	}

	@Override
	public int applyScreenshot(Pointer encoderContext)
			throws MissingFrameException {
		EncoderInterface encoder = JnaLibraryLoader.getEncoder();
		Pointer screenshotData = x11ScreenshotSource.x11_screenshot_source_getScreenshot(screenshotContext);
		
		if(screenshotData == null) {
			throw new MissingFrameException("Could not fetch screenshot data - result was a null pointer!");
		}
		return encoder.convert_frame(encoderContext, screenshotData);
	}

	@Override
	public int getWidth() {
		return x11ScreenshotSource.x11_screenshot_source_getWidth(screenshotContext);
	}

	@Override
	public int getHeight() {
		return x11ScreenshotSource.x11_screenshot_source_getHeight(screenshotContext);
	}

}
