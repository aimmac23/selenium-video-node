package com.aimmac23.node;

import java.io.File;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import com.aimmac23.node.args.IRecordArgs;
import com.aimmac23.node.jna.EncoderInterface;
import com.aimmac23.node.jna.LibVPX;

/**
 * A test of the recording controller logic, with mock inputs
 *
 */
public class VideoRecordControllerTest {

	private final class TestRecordArgs implements IRecordArgs {
		

		private ScreenshotSource screenshotSource;

		public TestRecordArgs(ScreenshotSource screenshotSource) {
			this.screenshotSource = screenshotSource;
		}
		
		@Override
		public int getTargetFramerate() {
			return 15;
		}

		@Override
		public ScreenshotSource getNewScreenshotSource() {
			return screenshotSource;
		}
	}
	
	@Test
	public void canRecordVideo() throws Exception {

		// mock the LibVPX dependency - technically we only need it for error handling
		LibVPX vpx = EasyMock.createStrictMock(LibVPX.class);
		
		// mock the screenshot source
		ScreenshotSource source = EasyMock.mock(ScreenshotSource.class);
		EasyMock.expect(source.getWidth()).andReturn(800).atLeastOnce();
		EasyMock.expect(source.getHeight()).andReturn(600).atLeastOnce();
		EasyMock.expect(source.applyScreenshot(EasyMock.anyObject())).andReturn(0).atLeastOnce();
		EasyMock.expect(source.getSourceName()).andReturn("TEST").anyTimes();
		source.doStartupSanityChecks(); EasyMock.expectLastCall().once();
		
		EasyMock.replay(vpx, source);
		
		EncoderInterface encoderInterface = new DummyEncoderInterface();

		IRecordArgs args = new TestRecordArgs(source);
		try (VideoRecordController controller = new VideoRecordController(args, vpx, encoderInterface)) {
			controller.startRecording();

			Thread.sleep(1000);

			File recording = controller.stopRecording();

			Assert.assertTrue("Recording file does not exist!", recording.exists());
			Assert.assertTrue("File is zero length!", recording.length() > 0);
		}
		
		EasyMock.verify(vpx, source);
	}
}
