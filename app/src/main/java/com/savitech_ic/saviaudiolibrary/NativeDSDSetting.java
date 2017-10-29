package com.savitech_ic.saviaudiolibrary;

public class NativeDSDSetting {
	static {
		System.loadLibrary("savitech_nativedsdsetting_promadic");
	}

	public native String getSavitechCopyright(boolean bAccept);
	
	public native String getLibraryVersion();

	public native int setNativeDSD(int fd, String usb_path, byte[] barryrawDescriptors, int nrawDescriptorSize, int SV_CMD);
}
