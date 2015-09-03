package com.sin.android.usb;

import android.content.Context;

import com.sin.android.usb.USBPermission.PermissionCallback;

/**
 * 授权信息保持 <br/>
 * 
 * @author trb
 * @date 2014-1-22
 */
public class USBPermissionHolder {
	public String action;
	public Context context;
	public PermissionCallback callback;

	public USBPermissionHolder(String action, Context context, PermissionCallback callback) {
		this.action = action;
		this.context = context;
		this.callback = callback;
	}
}
