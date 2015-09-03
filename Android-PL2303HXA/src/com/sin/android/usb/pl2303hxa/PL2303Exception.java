package com.sin.android.usb.pl2303hxa;

/**
 * PL2303异常基类 <br/>
 * 
 * @author trb
 * @date 2013-8-16
 */
public class PL2303Exception extends Exception {
	private static final long serialVersionUID = -1994767580891240702L;

	public PL2303Exception() {
		super();
	}

	public PL2303Exception(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	public PL2303Exception(String detailMessage) {
		super(detailMessage);
	}

	public PL2303Exception(Throwable throwable) {
		super(throwable);
	}
}
