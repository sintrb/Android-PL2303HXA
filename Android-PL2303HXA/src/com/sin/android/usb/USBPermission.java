package com.sin.android.usb;

import java.util.HashMap;
import java.util.Map;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

/**
 * USB授权相关 <br/>
 * 
 * @author trb
 * @date 2014-1-22
 */
public class USBPermission {
	public interface PermissionCallback {
		public boolean callback(boolean granted);
	}

	static Map<String, USBPermissionHolder> perHolders = new HashMap<String, USBPermissionHolder>();

	public static boolean hasPermission(Context context, UsbDevice usbDevice) {
		return USBUtil.getUsbManager(context).hasPermission(usbDevice);
	}

	/**
	 * 向用户请求获取某个USB设备的操作权限
	 * 
	 * @param context
	 *            上下文
	 * @param usbDevice
	 *            需要获取权限的USB设备
	 * @param callback
	 *            回调，当获得权限的时候会被调用
	 * @return 请求之前已经有权限返回true，还没有权限返回false
	 */
	public static boolean requestPermission(Context context, UsbDevice usbDevice, PermissionCallback callback) {
		return requestPermission(context, usbDevice, "action" + System.currentTimeMillis(), callback);
	}

	/**
	 * 向用户请求获取某个USB设备的操作权限
	 * 
	 * @param context
	 *            上下文
	 * @param usbDevice
	 *            需要获取权限的USB设备
	 * @param action
	 *            动作描述
	 * @param callback
	 *            回调，当获得权限的时候会被调用
	 * @return 请求之前已经有权限返回true，还没有权限返回false
	 */
	public static boolean requestPermission(Context context, UsbDevice usbDevice, String action, PermissionCallback callback) {
		if (USBPermission.hasPermission(context, usbDevice)) {
			if (callback != null) {
				callback.callback(true);
			}
			return true;
		} else {
			PendingIntent mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(action), 0);
			IntentFilter filter = new IntentFilter(action);
			filter.addAction(action);
			context.registerReceiver(permissionReceiver, filter);
			USBPermissionHolder holder = new USBPermissionHolder(action, context, callback);
			perHolders.put(action, holder);
			USBUtil.getUsbManager(context).requestPermission(usbDevice, mPermissionIntent);
			return false;
		}
	}

	// 广播接收器
	private final static BroadcastReceiver permissionReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (perHolders.containsKey(action)) {
				USBPermissionHolder holder = perHolders.get(action);
				if (holder.callback != null)
					holder.callback.callback(intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false));
				perHolders.remove(action);
				holder.context.unregisterReceiver(permissionReceiver);
			}
		}
	};
}
