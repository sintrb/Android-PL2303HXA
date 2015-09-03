package com.sin.android.usb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

/**
 * USB工具类 <br/>
 * 该类用于USB操作
 * 
 * @author trb
 * @date 2014-1-22
 */
public class USBUtil {

	private static Context context;
	private static UsbManager usbManager;
	private static String TAG = "USBUtil";

	private static void updateStaticVar(Context context) {
		if (USBUtil.context != context) {
			Log.i(TAG, "updateAllStatic");
			USBUtil.context = context;
			USBUtil.usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
		}
	}

	public static UsbManager getUsbManager(Context context) {
		updateStaticVar(context);
		return usbManager;
	}

	/**
	 * 获取指定厂商ID和产品ID的所有USB设备
	 * 
	 * @param context
	 *            上下文
	 * @param vendorId
	 *            产商ID, -1时不做判断
	 * @param productId
	 *            产品ID, -1时不做判断
	 * @return 符合的设备列表
	 */
	public static List<UsbDevice> getUsbDevices(Context context, int vendorId, int productId) {
		updateStaticVar(context);

		HashMap<String, UsbDevice> devices = usbManager.getDeviceList();
		Iterator<UsbDevice> deviceIterator = devices.values().iterator();
		List<UsbDevice> retDevices = new ArrayList<UsbDevice>();
		while (deviceIterator.hasNext()) {
			UsbDevice device = deviceIterator.next();
			if ((vendorId == -1 || device.getVendorId() == vendorId) && (productId == -1 || device.getProductId() == productId)) {
				retDevices.add(device);
			}
		}

		return retDevices;
	}

	/**
	 * 获取指定厂商ID和产品ID的USB设备
	 * 
	 * @param context
	 *            上下文
	 * @param vendorId
	 *            产商ID, -1时不做判断
	 * @param productId
	 *            产品ID, -1时不做判断
	 * @return
	 */
	public static UsbDevice getUsbDevice(Context context, int vendorId, int productId) {
		List<UsbDevice> devices = getUsbDevices(context, vendorId, productId);
		return devices.size() > 0 ? devices.get(0) : null;
	}
}
