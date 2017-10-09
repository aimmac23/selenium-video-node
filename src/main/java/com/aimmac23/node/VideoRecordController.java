package com.aimmac23.node;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.aimmac23.node.args.IRecordArgs;
import com.aimmac23.node.jna.EncoderInterface;
import com.aimmac23.node.jna.LibVPX;

public class VideoRecordController implements Closeable {
	
	private static final Logger log = Logger.getLogger(RecordVideoCallable.class.getSimpleName());

	private ThreadPoolExecutor executor;
	RecordVideoCallable currentCallable;
	private Future<File> currentFuture;
	private final int targetFramerate;

	private ScreenshotSource screenshotSource;

	private LibVPX vpxLibrary;
	private EncoderInterface encoderInterface;
	
	public VideoRecordController(IRecordArgs recordArgs, LibVPX vpxLibrary, EncoderInterface encoderInterface) {
		this.vpxLibrary = vpxLibrary;
		this.encoderInterface = encoderInterface;
		executor = new ThreadPoolExecutor(1, 1, 1, TimeUnit.MINUTES, new ArrayBlockingQueue<Runnable>(5));
		executor.setThreadFactory(new RecorderThreadFactory());
		executor.prestartAllCoreThreads();
		
		targetFramerate = recordArgs.getTargetFramerate();
		
		log.info("Will attempt to record at " + targetFramerate + " frames per second - adjust this value " +
		" by setting -Dvideo.framerate=<value>");
		
		screenshotSource = recordArgs.getNewScreenshotSource();
		
		log.info("Using " + screenshotSource.getSourceName() + " Screenshot Source");
		
		screenshotSource.doStartupSanityChecks();
		
	}
	
	public void startRecording() throws Exception {
		if(currentCallable != null) {
			throw new IllegalStateException("Video recording currently in progress, cannot record again");
		}
		
		currentCallable = new RecordVideoCallable(targetFramerate, screenshotSource, vpxLibrary, encoderInterface);
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
	
	@Override
	public void close() throws IOException {
		// really only for unit tests
		executor.shutdown();		
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
