package com.mooo.aimmac23.node.jna;

import com.sun.jna.Library;

/**
 * libyuv library - http://code.google.com/p/libyuv/
 * 
 *  We don't depend on this directly - the C code calls it for us.
 * @author Alasdair Macmillan
 *
 */
public interface YUVLib extends Library {
}