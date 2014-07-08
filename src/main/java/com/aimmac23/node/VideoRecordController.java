package com.aimmac23.node;

import java.io.File;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class VideoRecordController {
	
	private static final Logger log = Logger.getLogger(RecordVideoCallable.class.getSimpleName());

	private ThreadPoolExecutor executor;
	RecordVideoCallable currentCallable;
	private Future<File> currentFuture;
	private final int targetFramerate;
	
	public VideoRecordController() {
		executor = new ThreadPoolExecutor(1, 1, 1, TimeUnit.MINUTES, new ArrayBlockingQueue<Runnable>(5));
		executor.setThreadFactory(new RecorderThreadFactory());
		executor.prestartAllCoreThreads();
		
		String framerateString = System.getProperty("video.framerate", "15");
		targetFramerate = Integer.parseInt(framerateString);
		
		log.info("Will attempt to record at  " + targetFramerate + " frames per second - adjust this value " +
		" by setting -Dvideo.framerate=<value>");
	}
	
	public void startRecording() {
		if(currentCallable != null) {
			throw new IllegalStateException("Video recording currently in progress, cannot record again");
		}
		currentCallable = new RecordVideoCallable(targetFramerate);
		currentFuture = executor.submit(currentCallable);
	}
	
	public File stopRecording() throws Exception {
		if(currentCallable == null) {
			throw new IllegalStateException("Video recording not currently in progress, cannot stop!");
		}
		
		// sleep for half a second, to make sure we catch the end of the test
		// XXX: Do we really need this?
		Thread.sleep(500);
		
		currentCallable.stopRecording();
		currentCallable = null;
		return currentFuture.get();
	}
	
	public void resetRecording() {
		// if we are currently recording, stop
		if(currentCallable != null) {
			currentCallable.stopRecording();
			currentCallable = null;
			
			log.info("Stopped recording due to resetRecording being called");
		}
		else {
			log.info("resetRecording called but not recording - nothing to do");	
		}
		
	}
	class RecorderThreadFactory implements ThreadFactory {
		
		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r);
			thread.setDaemon(true);
			thread.setName("VideoRecordingThread");
			return thread;
		}
	}
}
