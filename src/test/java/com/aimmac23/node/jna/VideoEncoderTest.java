package com.aimmac23.node.jna;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.aimmac23.node.ScreenshotSource;
import com.aimmac23.node.TestScreenshotSource;
import com.aimmac23.node.VideoRecordController;
import com.aimmac23.node.args.IRecordArgs;

/**
 * Test the ability to encode video data, at various resolutions.
 * 
 * On failure, the most likely outcome will be a segmentation fault.
 * 
 * @author aim
 *
 */
public class VideoEncoderTest {
	
	@Before
	public void setup() {
		JnaLibraryLoader.init();
	}
	
	private final class TestRecordArgs implements IRecordArgs {
		
		private EncoderInterface encoderInterface;
		private int width;
		private int height;
		private int targetFramerate;

		public TestRecordArgs(EncoderInterface encoderInterface, int targetFramerate, int width, int height) {
			this.encoderInterface = encoderInterface;
			this.targetFramerate = targetFramerate;
			this.width = width;
			this.height = height;
		}
		
		@Override
		public int getTargetFramerate() {
			return targetFramerate;
		}

		@Override
		public ScreenshotSource getNewScreenshotSource() {
			return new TestScreenshotSource(encoderInterface, width, height);
		}
	}
	
	@Test
	public void testSmallResolution() throws Exception {
		TestRecordArgs recordArgs = new TestRecordArgs(JnaLibraryLoader.getEncoder(), 60, 800, 600);
		
		doTest(recordArgs);
		
	}
	
	@Test
	public void test1080p() throws Exception {
		TestRecordArgs recordArgs = new TestRecordArgs(JnaLibraryLoader.getEncoder(), 60, 1920, 1080);
		
		doTest(recordArgs);
		
	}

	@Test
	public void test4k() throws Exception {
		TestRecordArgs recordArgs = new TestRecordArgs(JnaLibraryLoader.getEncoder(), 60, 3824, 2160);
		
		doTest(recordArgs);
		
	}
	
	private void doTest(TestRecordArgs recordArgs) throws Exception, InterruptedException, IOException {
		try (VideoRecordController controller = new VideoRecordController(recordArgs, JnaLibraryLoader.getLibVPX(), JnaLibraryLoader.getEncoder())) {
			controller.startRecording();

			Thread.sleep(1000);

			File recording = controller.stopRecording();

			Assert.assertTrue("Recording file does not exist!", recording.exists());
			Assert.assertTrue("File is zero length!", recording.length() > 0);
		}
	}

}
