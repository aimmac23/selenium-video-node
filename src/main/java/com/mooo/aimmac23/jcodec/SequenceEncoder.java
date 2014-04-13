package com.mooo.aimmac23.jcodec;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.jcodec.codecs.h264.H264Encoder;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.vpx.VP8Encoder;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.common.VideoEncoder;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mkv.MKVMuxer;
import org.jcodec.containers.mkv.MKVMuxer.MKVMuxerTrack;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.TrackType;
import org.jcodec.containers.mp4.muxer.FramesMP4MuxerTrack;
import org.jcodec.containers.mp4.muxer.MP4Muxer;
import org.jcodec.scale.AWTUtil;
import org.jcodec.scale.ColorUtil;
import org.jcodec.scale.Transform;

import com.sun.jna.Library;
import com.sun.jna.Native;

/**
 * From http://stackoverflow.com/questions/10969423/jcodec-has-anyone-seen-documentation-on-this-library
 *
 */
public class SequenceEncoder {
 
	
    private SeekableByteChannel ch;
    private Picture toEncode;
    private Transform transform;
    private VideoEncoder encoder;
    private ArrayList<ByteBuffer> spsList;
    private ArrayList<ByteBuffer> ppsList;
    private MKVMuxerTrack outTrack;
    private ByteBuffer _out;
    private int frameNo;
    private int presentationTimestamp = 0;
    private MKVMuxer muxer;
	private int targetFramerate;

    public SequenceEncoder(File out, Size dimensions, int targetFramerate) throws IOException {
        this.ch = NIOUtils.writableFileChannel(out);
        this.targetFramerate = targetFramerate;

        // Create an instance of encoder
        encoder = new VP8Encoder();

        // Transform to convert between RGB and YUV
        transform = ColorUtil.getTransform(ColorSpace.RGB, encoder.getSupportedColorSpaces()[0]);

        // Muxer that will store the encoded frames
        muxer = new MKVMuxer(ch);

        // Add video track to muxer
        outTrack = muxer.addVideoTrack(dimensions, "VP8");

        // Allocate a buffer big enough to hold output frames
        _out = ByteBuffer.allocate(1920 * 1080 * 6);

        // Encoder extra data ( SPS, PPS ) to be stored in a special place of
        // MP4
        spsList = new ArrayList<ByteBuffer>();
        ppsList = new ArrayList<ByteBuffer>();

    }
    
    /**
     * Add another frame of animation, assuming we're still hitting our target rate
     * @param bi
     * @throws IOException
     */
    public void encodeImage(BufferedImage bi) throws IOException {
    	encodeImage(bi, 1);
    }

    /**
     * Encode another frame of animation.
     * @param bi
     * @param frameDuration - how many animation frames this image should be used for (1 if we're hitting 
     * 	our target framerate rate, more if not)
     * @throws IOException
     */
    public void encodeImage(BufferedImage bi, int frameDuration) throws IOException {
        if (toEncode == null) {
            toEncode = Picture.create(bi.getWidth(), bi.getHeight(), encoder.getSupportedColorSpaces()[0]);
        }

        
        transform.transform(AWTUtil.fromBufferedImage(bi), toEncode);

        // Encode image into H.264 frame, the result is stored in '_out' buffer
        _out.clear();
        ByteBuffer result = encoder.encodeFrame(toEncode, _out);

        // Based on the frame above form correct MP4 packet
        spsList.clear();
        ppsList.clear();
        H264Utils.wipePS(result, spsList, ppsList);
        H264Utils.encodeMOVPacket(result);


        // Add packet to video track
        //outTrack.addFrame(new MP4Packet(result, presentationTimestamp, targetFramerate, frameDuration, frameNo, true, null, frameNo, 0));

        frameNo++;
        presentationTimestamp += frameDuration;
    }

    public void finish() throws IOException {
        // Push saved SPS/PPS to a special storage in MP4
        //outTrack.addSampleEntry(H264Utils.createMOVSampleEntry(spsList, ppsList));

        // Write MP4 header and finalize recording
        //muxer.writeHeader();
        NIOUtils.closeQuietly(ch);
    }
}