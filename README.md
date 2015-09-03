# Android-PL2303HXA
A Android USB driver and demo for PL2303HXA(only pl2303hxa), implemented with Java.

Android上PL2303HXA（仅针对PL2303HXA，其他型号不行）的USB驱动实现，基于Java应用层的，因此无需修改内核、无需Root权限。Android3.0以上，需要USBHOST功能。带OTG的设备基本都可以。

### 背景
PL2303HX的其他型号(如PL2303HXD)官方已经出了驱动库了，但是HXA的官方已经没再出了，原因是官方已经停产HXA了，但是在国内，还有很多山寨的HXA型号在卖（没错，说的就是淘宝上），对于这类器件该库也是适用的。

补充一点大部分代码来自于之前的工作单位，它们形成于两年前（2013年底）,当时因为做Android平板和单片机之间的通信而积累了这部分代码，弯路走了不少，现在放出代码也正是希望后面做接触这块的同行可以少走点弯路:)

### 说明
核心的实现是PL2303Driver类，封装了PL2303HXA的基本操作。该类通过一个UsbDevice(当然必须是PL2303HXA设备的)对象进行构造。

注意：AndroidManifest.xml文件中需添加两项USB权限
```xml
<uses-permission android:name="android.hardware.usb.host" />
<uses-permission android:name="android.hardware.usb.accessory" />
```

下面是部分代码片段，详细试用请看demo。
```java
// 通过PL2303Driver的getAllSupportedDevices方法来获取当前Android设备上关在的PL2303器件列表
List<UsbDevice> devices = PL2303Driver.getAllSupportedDevices(this);
if (devices.size() == 0) {
	AddLog("请先插入PL2303HXA设备");
} else {
	AddLog("当前PL2303HXA设备:");
	for (UsbDevice d : devices) {
		AddLog(" " + d.getDeviceId());
	}

	// 使用找到的第1个PL2303HXA设备
	// 通过USB设备来创建一个PL2303Driver对象用于PL2303的操作
	PL2303Driver dev = new PL2303Driver(this, devices.get(0));
	openPL2302Device(dev);
}


// 如果有多个PL2303器件，那么就打开选择对话框的方式
PL2303Selector.createSelectDialog(this, 0, false, new PL2303Selector.Callback() {

	@Override
	public boolean whenPL2303Selected(PL2303Driver driver) {
		openPL2302Device(driver);
		return true;
	}
}).show();

// 打开USB设备会涉及到一个权限请求的过程，在代码里面已经封装了。
// PL2303Driver.checkPermission方法会查看设备权限，如果没有授权的话会打开权限请求。
// PL2303Driver.setBaudRate用于设置通信波特率。
// 设置完毕之后通过PL2303Driver.open方法初始化并打开PL2303HXA设备
// 创建完PL2303Driver对象之后就可以对它进行读写操作了。

// 为了方便流式使用，封装了PL2303InputStream和PL2303OutputStream，通过传人PL2303Driver进行构造。

```

对于Android获取当前挂载的USB设备可以参考我的另外一个工程 [AndroidDemos](https://github.com/sintrb/AndroidDemos)的ListUSBDevice子项，或者直接安装[ListUSBDevice](https://github.com/sintrb/AndroidDemos/blob/master/APK/ListUSBDevice.apk?raw=true) 。


如果想直接体验的话这里有编译好的apk：[https://github.com/sintrb/Android-PL2303HXA/tree/master/APK](https://github.com/sintrb/Android-PL2303HXA/tree/master/APK)

来个简单的截图：
![PL2303HXA截图](https://raw.githubusercontent.com/sintrb/Android-PL2303HXA/master/IMG/screenshot.png)
