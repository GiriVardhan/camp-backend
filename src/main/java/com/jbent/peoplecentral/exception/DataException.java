package com.jbent.peoplecentral.exception;

public class DataException extends Exception {

	private static final long serialVersionUID = -7641394178731435069L;

	private String message;

	public DataException(String message) {

		this.message = message;
	}

	public DataException(String message, Exception e) {
		
		this.message = message;
		super.initCause(e);
	}

	/**
	 * 
	 * @return the message
	 */

	public String getMessage() {

		return message;

	}

}
