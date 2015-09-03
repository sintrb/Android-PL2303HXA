package com.sin.android.usb.pl2303hxa;

/**
 * 未发现PL2303 <br/>
 * 
 * @author trb
 * @date 2013-8-16
 */
public class PL2303NotFoundException extends PL2303Exception {
	private static final long serialVersionUID = -1994767580891240709L;

	public PL2303NotFoundException() {
		super();
	}

	public PL2303NotFoundException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	public PL2303NotFoundException(String detailMessage) {
		super(detailMessage);
	}

	public PL2303NotFoundException(Throwable throwable) {
		super(throwable);
	}
}
