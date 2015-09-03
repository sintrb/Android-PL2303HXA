package com.sin.android.usb.pl2303hxa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

/**
 * PL2303HXA驱动 <br/>
 * 
 * @author trb
 * @date 2013-8-16
 * 
 * @version 1.1
 * @verinfo V1.1<br/>
 *          2014-04-17<br/>
 *          设计为输入输出流的
 *          <hr/>
 */
@SuppressLint("DefaultLocale")
public class PL2303Driver {
	/**
	 * 版本标志
	 */
	public static String VERSION = "1.1";

	/**
	 * 调试标志
	 */
	public static String TAG = "PL2303HXADriver";
	/**
	 * 权限标志
	 */
	public static final String ACTION_PL2303_PERMISSION = "com.sin.android.USB_PERMISSION";

	/**
	 * 卸载USB设备
	 */
	public static final String ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";

	/**
	 * PL303HXA产品ID
	 */
	public static final int PL2303HXA_PRODUCT_ID = 0x2303;

	// 超时
	private int TimeOut = 100;
	private int transferTimeOut = TimeOut;
	private int readTimeOut = TimeOut;
	private int writeTimeOut = TimeOut;

	private Context context;

	private UsbManager usbManager;
	private UsbDevice usbDevice;
	private UsbDeviceConnection usbDeviceConnection;
	private UsbInterface usbInterface;
	private UsbEndpoint uein;
	private UsbEndpoint ueout;
	private boolean opened = false;
	private int baudRate = 115200; // 波特率

	// 收发器属性
	private static final int MAX_SENDLEN = 1;
	private static final int SECVBUF_LEN = 4096;
	private static final int SENDBUF_LEN = MAX_SENDLEN;

	// 缓冲区
	private byte[] recv_buf = new byte[SECVBUF_LEN];
	private byte[] send_buf = new byte[SENDBUF_LEN];
	private long receivecount = 0, sendcount = 0;

	/**
	 * 判断是否是支持的USB设备类型，即是否是PL2303HXA设备
	 * 
	 * @param device
	 *            USB设备
	 * @return true表示支持,false表示不支持
	 */
	public static boolean isSupportedDevice(UsbDevice device) {
		return device.getProductId() == PL2303HXA_PRODUCT_ID;
	}

	/**
	 * 获取所有支持的设备，即获取所有的PL2303HXA设备
	 * 
	 * @param context
	 *            上下文
	 * @return 设备列表
	 */
	public static List<UsbDevice> getAllSupportedDevices(Context context) {
		List<UsbDevice> res = new ArrayList<UsbDevice>();
		UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
		HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
		Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
		while (deviceIterator.hasNext()) {
			UsbDevice device = deviceIterator.next();
			boolean flag = isSupportedDevice(device);
			Log.i(TAG, getDeviceString(device) + (flag ? "" : " not support!"));
			if (flag)
				res.add(device);
		}
		return res;
	}

	/**
	 * 获取设备描述字符串
	 * 
	 * @param device
	 *            设备
	 * @return 描述字符串
	 */
	public static String getDeviceString(UsbDevice device) {
		return String.format("DID:%d VID:%04X PID:%04X", new Object[] { Integer.valueOf(device.getDeviceId()), Integer.valueOf(device.getVendorId()), Integer.valueOf(device.getProductId()) });
	}

	/**
	 * 获取USB设备
	 * 
	 * @return USB设备
	 */
	public UsbDevice getUsbDevice() {
		return usbDevice;
	}

	/**
	 * 创建一个PL2303HXA通信实例
	 * 
	 * @param context
	 *            上下文
	 * @param device
	 *            USB设备
	 */
	public PL2303Driver(Context context, UsbDevice device) {
		this.context = context;
		this.usbManager = (UsbManager) this.context.getSystemService(Context.USB_SERVICE);
		if (!isSupportedDevice(device)) {
			android.util.Log.e(TAG, getDeviceString(device) + "may is not a supported device");
		}
		this.usbDevice = device;
	}

	/**
	 * 获取设备ID
	 * 
	 * @return 设备ID
	 */
	public int getDeviceID() {
		return this.usbDevice.getDeviceId();
	}

	/**
	 * 转换为字符串描述
	 * 
	 * @return 描述字符串
	 */
	public String toString() {
		return String.format("DeviceID:%d VendorID:%04X ProductID:%04X R:%d T:%d", new Object[] { Integer.valueOf(usbDevice.getDeviceId()), Integer.valueOf(usbDevice.getVendorId()), Integer.valueOf(usbDevice.getProductId()), receivecount, sendcount });
	}

	/**
	 * 获取设备名称
	 * 
	 * @return 设备名词
	 */
	public String getName() {
		return String.format("PL2303_%d", usbDevice.getDeviceId());
	}

	/**
	 * 检查权限，如果没有权限则请求授权
	 * 
	 * @return true 有权限, false 无权限，并开始请求授权
	 */
	public boolean checkPermission() {
		return checkPermission(true);
	}

	/**
	 * 检查权限
	 * 
	 * @param autoRequest
	 *            没有权限的时候是否自动请求权限,true自动请求,false不自动请求
	 * @return true 有权限, false 无权限，并开始请求授权
	 */
	public boolean checkPermission(boolean autoRequest) {
		if (!usbManager.hasPermission(usbDevice)) {
			if (autoRequest) {
				PendingIntent mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_PL2303_PERMISSION), 0);
				usbManager.requestPermission(usbDevice, mPermissionIntent);
			}
			return false;
		}
		return true;
	}

	/**
	 * 获取波特率
	 * 
	 * @return 波特率
	 */
	public int getBaudRate() {
		return baudRate;
	}

	/**
	 * 设置波特率，如果串口已经打开则会重置串口
	 * 
	 * @param baudRate
	 *            新的波特率
	 * @throws PL2303Exception
	 *             设置波特率出错
	 */
	public void setBaudRate(int baudRate) throws PL2303Exception {
		this.baudRate = baudRate;
		if (this.isOpened()) {
			this.reset();
		}
	}

	/**
	 * 打开PL2303HXA设备
	 * 
	 * @throws PL2303Exception
	 *             打开失败异常，如果未授权或不支持的设置参数
	 */
	public void open() throws PL2303Exception {
		usbDeviceConnection = usbManager.openDevice(usbDevice);
		if (usbDeviceConnection != null) {
			Log.i(TAG, "openDevice()=>ok!");
			Log.i(TAG, "getInterfaceCount()=>" + usbDevice.getInterfaceCount());

			usbInterface = usbDevice.getInterface(0);

			for (int i = 0; i < usbInterface.getEndpointCount(); ++i) {
				UsbEndpoint ue = usbInterface.getEndpoint(i);
				if (ue.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK && ue.getDirection() == UsbConstants.USB_DIR_IN) {
					uein = ue;
				} else if (ue.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK && ue.getDirection() == UsbConstants.USB_DIR_OUT) {
					ueout = ue;
				}
			}
			if (uein != null && ueout != null) {
				Log.i(TAG, "get Endpoint ok!");
				usbDeviceConnection.claimInterface(usbInterface, true);
				byte[] buffer = new byte[1];
				controlTransfer(192, 1, 33924, 0, buffer, 1, transferTimeOut);
				controlTransfer(64, 1, 1028, 0, null, 0, transferTimeOut);
				controlTransfer(192, 1, 33924, 0, buffer, 1, transferTimeOut);
				controlTransfer(192, 1, 33667, 0, buffer, 1, transferTimeOut);
				controlTransfer(192, 1, 33924, 0, buffer, 1, transferTimeOut);
				controlTransfer(64, 1, 1028, 1, null, 0, transferTimeOut);
				controlTransfer(192, 1, 33924, 0, buffer, 1, transferTimeOut);
				controlTransfer(192, 1, 33667, 0, buffer, 1, transferTimeOut);
				controlTransfer(64, 1, 0, 1, null, 0, transferTimeOut);
				controlTransfer(64, 1, 1, 0, null, 0, transferTimeOut);
				controlTransfer(64, 1, 2, 68, null, 0, transferTimeOut);
				reset();
				opened = true;
			}
		} else {
			Log.e(TAG, "openDevice()=>fail!");
			throw new PL2303Exception("usbManager.openDevice failed!");
		}
	}

	/**
	 * 重置串口
	 * 
	 * @throws PL2303Exception
	 *             不支持的参数异常
	 */
	public void reset() throws PL2303Exception {
		byte[] mPortSetting = new byte[7];
		controlTransfer(161, 33, 0, 0, mPortSetting, 7, transferTimeOut);
		mPortSetting[0] = (byte) (baudRate & 0xff);
		mPortSetting[1] = (byte) (baudRate >> 8 & 0xff);
		mPortSetting[2] = (byte) (baudRate >> 16 & 0xff);
		mPortSetting[3] = (byte) (baudRate >> 24 & 0xff);
		mPortSetting[4] = 0;
		mPortSetting[5] = 0;
		mPortSetting[6] = 8;
		controlTransfer(33, 32, 0, 0, mPortSetting, 7, transferTimeOut);
		controlTransfer(161, 33, 0, 0, mPortSetting, 7, transferTimeOut);
	}

	private int controlTransfer(int requestType, int request, int value, int index, byte[] buffer, int length, int timeout) throws PL2303Exception {
		int res = this.usbDeviceConnection.controlTransfer(requestType, request, value, index, buffer, length, timeout);
		if (res < 0) {
			String err = String.format("controlTransfer fail when: %d %d %d %d buffer %d %d", requestType, request, value, index, length, timeout);
			Log.e(TAG, err);
			throw new PL2303Exception(err);
		}
		return res;
	}

	/**
	 * 设备是否已经打开
	 * 
	 * @return true已经打开，false未打开
	 */
	public boolean isOpened() {
		return opened;
	}

	/**
	 * 向串口发一字节
	 * 
	 * @param data
	 *            要发送的数据
	 * @return 发送了的字节数
	 */
	public int write(byte data) {
		send_buf[0] = data;
		int ret = usbDeviceConnection.bulkTransfer(ueout, send_buf, 1, writeTimeOut);
		++sendcount;
		return ret;
	}

	/**
	 * 发送一串数据
	 * 
	 * @param datas
	 *            要发送的数据数组
	 * @return 发送了的字节数
	 */
	public int write(byte[] datas) {
		int ret = usbDeviceConnection.bulkTransfer(ueout, datas, datas.length, writeTimeOut);
		sendcount += ret;
		return ret;
	}

	private int readix = 0;
	private int readlen = 0;
	private boolean readSuccess = false;

	/**
	 * 读取一个字节，调用该函数之后需要调用isReadSuccess()进行判断返回的数据是否是真正读取的数据
	 * 
	 * @return 被读取的数据
	 */
	public byte read() {
		byte ret = 0;
		if (readix >= readlen) {
			readlen = usbDeviceConnection.bulkTransfer(uein, recv_buf, SECVBUF_LEN, readTimeOut);
			readix = 0;
		}
		if (readix < readlen) {
			ret = recv_buf[readix];
			readSuccess = true;
			++receivecount;
			++readix;
		} else {
			readSuccess = false;
		}
		return ret;
	}

	/**
	 * 判断上一个读操作是否成功
	 * 
	 * @return
	 */
	public boolean isReadSuccess() {
		return readSuccess;
	}

	/**
	 * 关闭串口
	 */
	public void close() {
		if (this.opened) {
			if (usbDeviceConnection.releaseInterface(usbInterface))
				Log.i(TAG, "releaseInterface()=>ok!");
		}
		this.opened = false;
	}

	/**
	 * 清空读缓冲区
	 */
	public void cleanRead() {
		while ((readlen = usbDeviceConnection.bulkTransfer(uein, recv_buf, recv_buf.length, readTimeOut)) > 0) {

		}
		readlen = 0;
		readix = 0;
	}

	public int getReadTimeOut() {
		return readTimeOut;
	}

	public void setReadTimeOut(int readTimeOut) {
		this.readTimeOut = readTimeOut;
	}

	public int getWriteTimeOut() {
		return writeTimeOut;
	}

	public void setWriteTimeOut(int writeTimeOut) {
		this.writeTimeOut = writeTimeOut;
	}

}
