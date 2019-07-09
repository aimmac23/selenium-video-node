package com.aimmac23.node.jna;
import com.sun.jna.Library;
import com.sun.jna.Pointer;

/**
 * The libvpx encoder library - does the VP8 video encoding.
 * 
 * @author Alasdair Macmillan
 *
 */
public interface LibVPX extends Library {
	
	String vpx_codec_err_to_string(int result);
	
	String vpx_codec_iface_name(Pointer codecInterface);
	
	/**
	 * @return Returns a pointer to some sort of VP8 codec definition
	 */
	Pointer vpx_codec_vp8_cx();
}
