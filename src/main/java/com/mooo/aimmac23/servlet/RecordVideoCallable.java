package com.mooo.aimmac23.servlet;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.Callable;

import com.mooo.aimmac23.jcodec.SequenceEncoder;

public class RecordVideoCallable implements Callable<File> {
	
	private volatile boolean shouldStop = false;

	@Override
	public File call() throws Exception {
		File outputFile = File.createTempFile("screencast", ".mp4");
		SequenceEncoder encoder = new SequenceEncoder(outputFile);

		Rectangle screenSize = getScreenSize();
		while(!shouldStop) {
			long start = System.currentTimeMillis();
			BufferedImage image = new Robot().createScreenCapture(screenSize);
			
			encoder.encodeImage(image);
			long finish = System.currentTimeMillis();
			
			if(finish - start < 40 && !shouldStop) {
				System.out.println("We needed to sleep for " + (finish - start));
				Thread.sleep(finish - start);
			}
		}
		
		encoder.finish();
		
		return outputFile;
	}
	
	public void stopRecording() {
		shouldStop = true;
	}
	
	private Rectangle getScreenSize() {
		//XXX: This probably won't work with multiple monitors
		return GraphicsEnvironment.getLocalGraphicsEnvironment().
				getDefaultScreenDevice().getDefaultConfiguration().getBounds();
		
	}

}
