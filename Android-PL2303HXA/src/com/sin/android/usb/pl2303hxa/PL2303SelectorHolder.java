package com.sin.android.usb.pl2303hxa;

import java.util.List;

/**
 * PL2303选择器信息保持，由于没有使用完整的对话框类，因此需要用该类实例来报存选择过程中的相关操作
 * @author trb
 * @date 2013-11-18
 */
public class PL2303SelectorHolder {
	public List<PL2303Driver> drivers = null;
	public int curDriverIndex = -1;

	/**
	 * 获取当前选中的驱动
	 * @return 当前选中的驱动
	 */
	public PL2303Driver getCurDriver() {
		return curDriverIndex < 0 ? null : drivers.get(curDriverIndex);
	}
}
