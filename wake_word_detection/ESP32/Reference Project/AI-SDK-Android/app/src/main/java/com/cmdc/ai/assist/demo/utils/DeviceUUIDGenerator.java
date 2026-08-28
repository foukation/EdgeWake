package com.cmdc.ai.assist.demo.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.NetworkInterface;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.UUID;

public class DeviceUUIDGenerator {
	private static final String PREF_FILE = "device_uuid_pref";

	// 生成或获取设备唯一UUID（优先从本地存储读取，不存在则生成）
	public static String getDeviceUUID(Context context) {
		String uuid = readUUIDFromPrefs(context);
		if (uuid != null) {
			return uuid;
		}

		// 组合设备特征生成UUID
		String deviceId = getDeviceId(context);
		uuid = UUID.nameUUIDFromBytes(deviceId.getBytes()).toString();
		saveUUIDToPrefs(context, uuid);
		return uuid;
	}

	// 读取本地存储的UUID（若存在）
	private static String readUUIDFromPrefs(Context context) {
		try {
			File file = new File(context.getFilesDir(), PREF_FILE);
			if (!file.exists()) {
				return null;
			}

			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
			String uuid = (String) ois.readObject();
			ois.close();
			return uuid;
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	// 保存UUID到本地存储
	private static void saveUUIDToPrefs(Context context, String uuid) {
		try {
			File file = new File(context.getFilesDir(), PREF_FILE);
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
			oos.writeObject(uuid);
			oos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// 组合设备特征生成唯一ID
	private static String getDeviceId(Context context) {
		StringBuilder sb = new StringBuilder();

		// 添加ANDROID_ID（设备首次启动生成，刷机后会改变）
		@SuppressLint("HardwareIds") String androidId = Settings.Secure.getString(context.getContentResolver(),
				Settings.Secure.ANDROID_ID);
		sb.append(androidId != null ? androidId : "");

		// 添加MAC地址（适配各版本）
		String macAddress = getMacAddress(context);
		sb.append(macAddress);

		// 哈希处理确保唯一性（避免过长字符串）
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			byte[] hash = digest.digest(sb.toString().getBytes());
			return bytesToHex(hash);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return sb.toString();
		}
	}

	// 字节数组转十六进制字符串
	private static String bytesToHex(byte[] bytes) {
		StringBuilder result = new StringBuilder();
		for (byte b : bytes) {
			result.append(String.format("%02x", b));
		}
		return result.toString();
	}

	// 获取MAC地址（适配各Android版本）
	private static String getMacAddress(Context context) {
		// Android 6.0+通过NetworkInterface获取
		try {
			for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
				if (ni.getName().equals("wlan0") || ni.getName().equals("eth0")) {
					byte[] mac = ni.getHardwareAddress();
					if (mac != null) {
						StringBuilder sb = new StringBuilder();
						for (int i = 0; i < mac.length; i++) {
							sb.append(String.format("%02X:", mac[i]));
						}
						if (sb.length() > 0) {
							sb.deleteCharAt(sb.length() - 1);
						}
						return sb.toString();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		// 默认虚拟地址
		return "02:00:00:00:00:00";
	}
}
