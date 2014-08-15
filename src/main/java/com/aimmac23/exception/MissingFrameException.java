package com.aimmac23.exception;

/**
 * Exception thrown when image data for an input frame is missing/damaged.
 * 
 * @author Alasdair Macmillan
 *
 */
public class MissingFrameException extends Exception {
	
	private static final long serialVersionUID = 1L;

	public MissingFrameException(String message) {
		super(message);
	}

}
