/*
 * Created on Dec 19, 2007
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.borzak.cncmill;

public class MillingException extends RuntimeException {
	
	private static final long serialVersionUID = -8207580458694213335L;
	public static final int UNKNOWN = 0;
	public static final int SERIAL_ABORT = 1;
	public static final int LIMIT_ERROR = 2;
	public static final int INVALID_COMMAND = 3;
	public static final int NOT_IMPLEMENTED = 4;
	public static final int SOFT_LIMIT = 5;

	private int reason = UNKNOWN;

	private static String REASON_DESCRIPTION[] = {
		"Unknown Exception",
		"Serial Character cancelled receive",
		"Limit Switch has been tripped",
		"Invalid Command - probablt communications error",
		"Command not implemented - old firmware or communications error",
		"Software Location Limit Exceeeded: "};

	public MillingException() {
		super();
	}

	public MillingException(String arg0) {
		super(arg0);
	}

	public MillingException(Throwable arg0) {
		super(arg0);
	}

	public MillingException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}
	
	public MillingException(int reason) {
		this(REASON_DESCRIPTION[reason]);
	}

	public MillingException(int reason, String text) {
		this(REASON_DESCRIPTION[reason]+text);
	}
	
	public int getReason() {
		return reason;
	}

}
