package com.sin.android.usb.pl2303hxa;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.usb.UsbDevice;
import android.util.Log;

/**
 * PL2303选择相关，该类封装存在多个PL2303设备时候的选择操作
 * 
 * @author trb
 * @date 2013-11-18
 * 
 */
public class PL2303Selector {
	private static final String TAG = "PL2303Selector";
	public static int DEFAULT_BAUDRATE = 2400;

	public interface Callback {
		public boolean whenPL2303Selected(PL2303Driver driver);
	}

	/**
	 * 选择PL2303设备，没有设备的时候提示信息，只有一个的时候并且拥有的时候自动选择，有多个设备或者未授权的时候弹出选择对话框
	 * 
	 * @param context
	 *            上下文
	 * @param seldevid
	 *            默认被选中的设备ID,-1不默认
	 * @param autosel
	 *            只有一个并且被授权的时候是否自动选择
	 * @param callback
	 *            选择回调
	 */
	public static void selectDevice(final Context context, int seldevid, boolean autosel, final Callback callback) {
		Dialog dlg = createSelectDialog(context, seldevid, autosel, callback);
		if (dlg != null) {
			dlg.show();
		}
	}

	/**
	 * 创建选择PL2303设备的对话框，没有设备的时候提示信息，只有一个的时候并且拥有的时候自动选择，有多个设备或者未授权的时候弹出选择对话框
	 * 
	 * @param context
	 *            上下文
	 * @param seldevid
	 *            默认被选中的设备ID,-1不默认
	 * @param autosel
	 *            只有一个并且被授权的时候是否自动选择
	 * @param callback
	 *            选择回调
	 */
	public static Dialog createSelectDialog(final Context context, int seldevid, boolean autosel, final Callback callback) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		final Dialog dlg;
		builder.setTitle("选择PL2303设备");
		List<UsbDevice> devs = PL2303Driver.getAllSupportedDevices(context);
		if (devs.size() > 0) {
			// 有PL2303设备
			final PL2303SelectorHolder holder = new PL2303SelectorHolder();
			
			holder.drivers = new ArrayList<PL2303Driver>();
			String[] sitems = new String[devs.size()];
			int selix = -1;
			for (int i = 0; i < devs.size(); ++i) {
				PL2303Driver dr = new PL2303Driver(context, devs.get(i));
				if (dr.getDeviceID() == seldevid) {
					selix = i;
				}
				try {
					dr.setBaudRate(DEFAULT_BAUDRATE);
				} catch (PL2303Exception e) {
					e.printStackTrace();
				}
				String name = dr.getName();
				sitems[i] = name;
				holder.drivers.add(dr);
			}

			if (autosel && holder.drivers.size() == 1 && holder.drivers.get(0).checkPermission(false)) {
				// 只有一个设备的时候不用显示对话框
				if (callback != null) {
					callback.whenPL2303Selected(holder.drivers.get(0));
				}
				return null;
			}

			builder.setSingleChoiceItems(sitems, selix, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (holder.curDriverIndex != which || holder.curDriverIndex < 0 || holder.drivers.get(holder.curDriverIndex).isOpened() == false) {
						holder.curDriverIndex = which;
						Log.i(TAG, "clk: " + which);
					}
				}
			});
			builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (callback != null) {
						if (holder.getCurDriver() != null && holder.getCurDriver().isOpened())
							holder.getCurDriver().close();
						if (holder.getCurDriver() == null)
							return;
						callback.whenPL2303Selected(new PL2303Driver(context, holder.getCurDriver().getUsbDevice()));
					}
				}
			});
			builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			});
			dlg = builder.create();
		} else {
			// 没有设备
			builder.setMessage("没有找到任何可用的PL2303设备");
			builder.setPositiveButton("确定", null);
			dlg = builder.create();
		}
		return dlg;
	}
}
