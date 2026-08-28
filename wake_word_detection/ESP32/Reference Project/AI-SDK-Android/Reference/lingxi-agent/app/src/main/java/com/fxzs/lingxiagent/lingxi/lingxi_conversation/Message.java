package com.fxzs.lingxiagent.lingxi.lingxi_conversation;

import android.graphics.Bitmap;

import java.util.ArrayList;

public class Message {
	public static final int TYPE_TEXT = 0;
	public static final int TYPE_IMAGE = 1;
	public static final int TYPE_POI_LIST = 2;
	public static final int TYPE_ACCESSIBILITY_CARD = 3;
	public static final int TYPE_AUDIO_CARD = 4;
	public static final int TYPE_FLOAT_CARD = 5;
	public static final int TYPE_GUIDE = 6;
	public static final int TYPE_TAG = 61;
	public static final int TYPE_MEDIA = 7;
	public static final int TYPE_POI_HONOR_LIST = 8;
	public static final int TYPE_POIHOTELCARD = 9;
	public static final int TYPE_TYPER = 10;
	public static final int TYPE_COT = 11;
	public static final int TYPE_IMAGES = 12;

	public static final int SENDER_USER = 0;
	public static final int SENDER_RECEIVER = 1;
	public static final int TYPE_READ_STATUS_CARD = 12;

	private String content;
	private final int type;
	private final int sender;
	private Object data;
	private Bitmap bitmap;
	private String mediaTitle;
	private boolean isShowButtons = false;
	private boolean isFinish = false;
	private String cotSummaryText;
	private boolean cotExpand;
	private ArrayList<String> imagesPath;
	private boolean isSelected = false;

	public Message(Bitmap bitmap, String content, int type, int sender) {
		this.content = content;
		this.type = type;
		this.sender = sender;
		this.bitmap = bitmap;
	}

	public Message(String mediaTitle, String content, int type, int sender) {
		this.mediaTitle = mediaTitle;
		this.content = content;
		this.type = type;
		this.sender = sender;
	}

	public Message(String content, int type, int sender) {
		this.content = content;
		this.type = type;
		this.sender = sender;
	}

	// 图片集合展示
	public Message(ArrayList<String> imagesPath, int type, int sender) {
		this.imagesPath = imagesPath;
		this.type = type;
		this.sender = sender;
	}

	// 菜单列表消息构造器
	public Message(String content, int type, int sender, Object data) {
		this.content = content;
		this.type = type;
		this.sender = sender;
		this.data = data;
	}

	// [荣耀]-出行聚餐
	public Message(int type, int sender, Object data) {
		this.type = type;
		this.sender = sender;
		this.data = data;
	}

	// [荣耀]-菜单列表消息构造器
	public Message(int type, int sender, Object data, String content) {
		this.type = type;
		this.sender = sender;
		this.data = data;
		this.content = content;
	}

	// 打字机构造器
	public Message(int type, int sender, String content, Boolean isFinish) {
		this.type = type;
		this.sender = sender;
		this.content = content;
		this.isFinish = isFinish;
	}

	public void setShowButtons(boolean isShowButtons) {
		this.isShowButtons = isShowButtons;
	}

	public void appendContent(String newText) {
		this.content = newText;
	}

	public void setFinish(boolean finish) {
		this.isFinish = finish;
	}

	public void setCotSummaryText(String cotSummaryText) {
		this.cotSummaryText = cotSummaryText;
	}

	public void setCotExpand(boolean cotExpand) {
		this.cotExpand = cotExpand;
	}

	public void setIsSelected(boolean isSelected) {
		this.isSelected = isSelected;
	}

	// Getter方法
	public String getContent() {
		return content;
	}

	public int getType() {
		return type;
	}

	public int getSender() {
		return sender;
	}

	public Object getData() {
		return data;
	}

	public Bitmap getBitmap() {
		return bitmap;
	}

	public String getMediaTitle() {
		return mediaTitle;
	}

	public boolean getShowButtons() {
		return isShowButtons;
	}

	public boolean getFinish() {
		return isFinish;
	}

	public String getCotSummaryText() {
		return cotSummaryText;
	}

	public boolean getCotExpand() {
		return cotExpand;
	}

	public ArrayList<String> getImagesPath() {
		return imagesPath;
	}

	public boolean getIsSelected() {
		return isSelected;
	}
}