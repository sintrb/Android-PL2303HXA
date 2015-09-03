package com.sin.android.demo;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.sin.android.pl2303hxa.R;
import com.sin.android.sinlibs.activities.BaseActivity;
import com.sin.android.sinlibs.base.Callable;
import com.sin.android.sinlibs.utils.InjectUtils;
import com.sin.android.usb.pl2303hxa.PL2303Driver;
import com.sin.android.usb.pl2303hxa.PL2303Exception;

public class MainActivity extends BaseActivity {
	private Spinner sp_baudrate = null;
	private Button btn_open = null;
	private Button btn_close = null;
	private CheckBox cb_hex = null;
	private CheckBox cb_hex_rev = null;
	private Button btn_send = null;
	private EditText et_send = null;
	private TextView tv_log = null;

	private TextView et_sleep = null;
	private CheckBox cb_auto = null;

	// PL2303驱动
	private PL2303Driver curDriver = null;

	private static final String TEXT_CHARSET = "UTF-8"; // 字符编码方式

	private static final String[] BAUDRATES = { "9600", "14400", "19200", "38400", "56000", "57600", "115200" };

	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (com.sin.android.usb.pl2303hxa.PL2303Driver.ACTION_PL2303_PERMISSION.equals(action)) {
				synchronized (this) {
					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						AddLog("授权成功");
						open();
					} else {
						curDriver = null;
						AddLog("授权失败");
					}
				}
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		sharedPreferences = getSharedPreferences("config", MODE_PRIVATE);

		setContentView(R.layout.activity_main);

		initControls();

		// 注册USB广播接收器，用于监听授权
		IntentFilter filter = new IntentFilter(PL2303Driver.ACTION_PL2303_PERMISSION);
		filter.addAction(PL2303Driver.ACTION_PL2303_PERMISSION);
		this.registerReceiver(mUsbReceiver, filter);
	}

	private void initControls() {
		// find and set all View to this.xx
		InjectUtils.injectViews(this, R.id.class);

		sp_baudrate = (Spinner) findViewById(R.id.sp_baudrate);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, BAUDRATES);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		sp_baudrate.setAdapter(adapter);

		tv_log.setOnLongClickListener(new View.OnLongClickListener() {

			@Override
			public boolean onLongClick(View arg0) {
				logct = 0;
				tv_log.setText("");
				return false;
			}
		});

		// event
		btn_open.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				preOpen();
			}
		});

		btn_close.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				close();
			}
		});

		btn_send.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				send();
			}
		});

		cb_auto.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				refreshButton();
			}
		});

		refreshButton();
	}

	private int logct = 0;

	private void AddLog(String ftm, Object... args) {
		String log = String.format(ftm, args);
		safeCall(new Callable() {
			@Override
			public void call(Object... args) {
				++logct;
				tv_log.append(String.format("%03d ", logct));
				tv_log.append(args[0].toString());
				tv_log.append("\n");

				((ScrollView) tv_log.getParent()).post(new Runnable() {
					@Override
					public void run() {
						((ScrollView) tv_log.getParent()).fullScroll(ScrollView.FOCUS_DOWN);
					}
				});
			}
		}, log);
	}

	// 招到PL2303并请求打开
	private void preOpen() {
		// 试用第1个PL2303设备
		List<UsbDevice> devices = PL2303Driver.getAllSupportedDevices(this);
		if (devices.size() == 0) {
			AddLog("请先插入PL2303HXA设备");
		} else {
			AddLog("当前PL2303HXA设备:");
			for (UsbDevice d : devices) {
				AddLog(" " + d.getDeviceId());
			}

			// 使用找到的第1个PL2303HXA设备
			PL2303Driver dev = new PL2303Driver(this, devices.get(0));
			openPL2302Device(dev);
		}

		// 打开选择对话框选择设备
		/*
		 * PL2303Selector.createSelectDialog(this, 0, false, new
		 * PL2303Selector.Callback() {
		 * 
		 * @Override public boolean whenPL2303Selected(PL2303Driver driver) {
		 * openPL2302Device(driver); return true; } }).show();
		 */
	}

	private void openPL2302Device(PL2303Driver dev) {
		if (dev != null) {
			curDriver = dev;
			if (curDriver.checkPermission()) {
				// 如果已经授权就直接打开
				open();
			}
		}
	}

	private String getReceive(ArrayList<Byte> recs) {
		StringBuffer sb = new StringBuffer();
		sb.append("收到: ");
		if (cb_hex_rev.isChecked()) {
			for (Byte b : recs) {
				sb.append(String.format("%02x ", b));
			}
		} else {
			int len = recs.size();
			byte[] data = new byte[len];
			for (int i = 0; i < len; ++i) {
				data[i] = recs.get(i);
			}
			try {
				sb.append(new String(data, TEXT_CHARSET));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				AddLog("转换编码失败");
			}
		}
		return sb.toString();
	}

	// 接收线程,始终接收
	private void startReceiveThread() {
		asynCall(new Callable() {
			@Override
			public void call(Object... arg0) {
				AddLog("开始接收");

				ArrayList<Byte> recs = new ArrayList<Byte>();
				long pretime = System.currentTimeMillis();
				while (curDriver != null) {
					PL2303Driver tDriver = curDriver;
					byte dat = 0;
					synchronized (tDriver) {
						dat = tDriver.read();
					}
					if (tDriver.isReadSuccess()) {
						recs.add(dat);
					} else {
						// 暂时没有收到数据
						pretime = 0;
						try {
							Thread.sleep(50);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}

					}

					// 至少没200ms显示一下接收到的数据
					if ((System.currentTimeMillis() - pretime) > 200 && recs.size() > 0) {
						AddLog(getReceive(recs));
						recs.clear();
						pretime = System.currentTimeMillis();
					}
					if (recs.size() == 0)
						pretime = System.currentTimeMillis();
				}
				AddLog("接收结束");
			}
		});
	}

	private void startAutoSendThread() {
		asynCall(new Callable() {
			@Override
			public void call(Object... arg0) {

				int sleep = 1000;
				while (curDriver != null) {
					if (cb_auto.isChecked()) {
						// 自动发送
						send();

						try {
							int v = Integer.parseInt(et_sleep.getText().toString());
							// 最快不能小于10ms
							if (v >= 10)
								sleep = v;
						} catch (Exception e) {
							AddLog("间隔错误");
						}

						try {
							Thread.sleep(sleep);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					} else {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
		});
	}

	// 打开串口
	private void open() {
		if (curDriver == null)
			return;
		try {
			if (curDriver.isOpened()) {
				curDriver.cleanRead();
				curDriver.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		AddLog("打开串口", curDriver.getDeviceID());
		try {
			curDriver.setBaudRate(Integer.parseInt(sp_baudrate.getSelectedItem().toString()));
			curDriver.open();

			startReceiveThread();
			startAutoSendThread();
			refreshButton();
		} catch (PL2303Exception e) {
			AddLog("打开失败");
			AddLog(e.getMessage());
			e.printStackTrace();
		}
	}

	private void close() {
		AddLog("关闭串口");
		synchronized (curDriver) {
			curDriver.cleanRead();
			curDriver.close();
		}
		curDriver = null;
		AddLog("关闭成功");

		refreshButton();
	}

	private void send() {
		if (curDriver == null) {
			AddLog("请先打开串口");
			return;
		}

		String ins = et_send.getText().toString();
		if (cb_hex.isChecked()) {
			// HEX
			ins = ins.trim();
			if (ins.length() > 0) {
				String[] hexs = ins.split(" ");
				List<Byte> data = new ArrayList<Byte>();
				boolean okflag = true;
				for (String hex : hexs) {
					try {
						int d = Integer.parseInt(hex, 16);
						if (d > 255) {
							AddLog("%s 大于0xff", hex);
							okflag = false;
						} else {
							data.add((byte) d);
						}
					} catch (NumberFormatException e) {
						AddLog("%s 不是十六进制数", hex);
						e.printStackTrace();
						okflag = false;
					}
				}
				if (okflag && data.size() > 0) {
					for (Byte b : data) {
						curDriver.write(b);
					}
				}
			} else {
				AddLog("要发送的十六进制数据为空");
			}
		} else {
			try {
				byte[] data = ins.getBytes(TEXT_CHARSET);
				curDriver.write(data);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				AddLog("转换编码失败");
			}
		}
	}

	private void refreshButton() {
		btn_open.setVisibility(curDriver != null ? View.GONE : View.VISIBLE);
		btn_close.setVisibility(curDriver == null ? View.GONE : View.VISIBLE);
		sp_baudrate.setEnabled(curDriver == null);
		btn_send.setEnabled(curDriver != null);

		et_sleep.setEnabled(!cb_auto.isChecked());
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		this.unregisterReceiver(mUsbReceiver);
	}

	private SharedPreferences sharedPreferences = null;

	@Override
	protected void onPause() {
		Editor edit = sharedPreferences.edit();
		edit.putBoolean("cb_hex", cb_hex.isChecked());
		edit.putBoolean("cb_hex_rev", cb_hex_rev.isChecked());
		edit.putInt("sp_baudrate", sp_baudrate.getSelectedItemPosition());
		edit.putString("et_send", et_send.getText().toString());
		edit.putString("et_sleep", et_sleep.getText().toString());
		edit.putBoolean("cb_auto", cb_auto.isChecked());
		edit.commit();
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (cb_auto != null) {
			cb_hex.setChecked(sharedPreferences.getBoolean("cb_hex", true));
			cb_hex_rev.setChecked(sharedPreferences.getBoolean("cb_hex_rev", true));
			cb_auto.setChecked(sharedPreferences.getBoolean("cb_auto", false));

			sp_baudrate.setSelection(sharedPreferences.getInt("sp_baudrate", 0));
			et_send.setText(sharedPreferences.getString("et_send", ""));
			et_sleep.setText(sharedPreferences.getString("et_sleep", "1000"));

			refreshButton();
		}
	}
}
