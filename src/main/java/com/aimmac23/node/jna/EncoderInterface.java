package com.aimmac23.node.jna;

import com.sun.jna.Library;
import com.sun.jna.Pointer;

/**
 * The native C interface we use for video encoding. All the 
 * state is kept in C-land - this interface represents the points
 * where we have to cross between Java-land and C-land.
 * 
 * @author Alasdair Macmillan
 *
 */
public interface EncoderInterface extends Library {
	
	Pointer create_context(String outputFile);
	
	int init_encoder(Pointer context, int width, int height, int fps);
	
	int init_codec(Pointer context);
	
	int init_image(Pointer context);
	
	int convert_frame(Pointer context, int[] data);
	
	int encode_next_frame(Pointer context, long duration);
	
	int encode_finish(Pointer context);
	
	String codec_error_detail(Pointer context);

}
	

