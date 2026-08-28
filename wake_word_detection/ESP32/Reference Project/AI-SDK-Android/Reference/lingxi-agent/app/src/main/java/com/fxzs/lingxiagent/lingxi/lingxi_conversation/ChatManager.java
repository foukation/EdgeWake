package com.fxzs.lingxiagent.lingxi.lingxi_conversation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.lingxi.config.PermissionType;
import com.fxzs.lingxiagent.lingxi.main.utils.BroadcastUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChatManager {
	@SuppressLint("StaticFieldLeak")
//	public static MessageAdapter messageAdapter;
	public static List<Message> messageList;
	private Message currentReply;
	private Activity context;

	@SuppressLint("SuspiciousIndentation")
	public void init(Activity context) {
		this.context = context;
		messageList = new ArrayList<>();
//		messageAdapter = new MessageAdapter(messageList);
//		messageAdapter.setShareListener(listener);
	}

	// 发送一个提问，input 为null 时则从输入框获取
	public void sendQuestion(String input) {
		sendMessage(input);
	}

	@SuppressLint("NotifyDataSetChanged")
	public void addSampleMessages() {
		if (messageList != null && messageList.size() > 0) {
			messageList.clear();
		}
		//todo 增加获取历史
		//1 如果没历史，展示引导页面  2 如果有对应的历史，历史+**智能体
		int size = messageList.size();
		if (size == 0) {
			messageList.add(new Message("", Message.TYPE_GUIDE, Message.SENDER_RECEIVER));
//			messageAdapter.notifyItemInserted(0);
		}
		if (size == 2) {
			Message lastItem = messageList.get(size - 1);
			if (lastItem.getType() == Message.TYPE_TAG) {
				messageList.remove(size - 1);
				messageList.remove(0);
//				messageAdapter.notifyItemRangeRemoved(0, 2);
			}
			messageList.add(new Message("", Message.TYPE_GUIDE, Message.SENDER_RECEIVER));
//			messageAdapter.notifyItemInserted(messageList.size() - 1);
		}

		if (size > 2) {
			Message lastItem = messageList.get(size - 1);
			if (lastItem.getType() == Message.TYPE_TAG) {
				messageList.remove(size - 1);
//				messageAdapter.notifyItemRangeRemoved(size - 1, 1);
			}
		}

		messageList.add(new Message("", Message.TYPE_TAG, Message.SENDER_RECEIVER));
//		messageAdapter.notifyItemChanged(messageList.size() - 1);
	}

	public void sendMultimodalQuestion(Uri uri) throws IOException {
		Bitmap bitmap;
		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				ImageDecoder.Source source = ImageDecoder.createSource(context.getContentResolver(), uri);
				bitmap = ImageDecoder.decodeBitmap(source);
			} else {
				bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		if (bitmap == null) {
			return;
		}
		simulateImage(bitmap);
		simulateReply(context.getString(R.string.text_waiting_reply), false, false);
	}

	@SuppressLint("NotifyDataSetChanged")
	public void addAllMessages(List<Message> messages) {
		messageList.addAll(messages);
//		messageAdapter.notifyDataSetChanged();
	}

	public void setMessageList(Message message) {
		messageList.add(message);
//		messageAdapter.notifyItemInserted(messageList.size() - 1);
		BroadcastUtils.INSTANCE.viewScrollBottomBroadcast();
	}


	public void sendTypeWrite(String content, Boolean isFinish) {
		messageList.remove(messageList.size() - 1);
//		messageAdapter.notifyItemRemoved(messageList.size() - 1);
//		messageAdapter.notifyItemChanged(messageList.size() - 2);
		Message menuMessage = new Message(Message.TYPE_TYPER, Message.SENDER_RECEIVER, content, isFinish);
		setMessageList(menuMessage);
		BroadcastUtils.INSTANCE.completeHonorCardBroadcast();
	}

	public void sendCot(String content) {
		messageList.remove(messageList.size() - 1);
//		messageAdapter.notifyItemRemoved(messageList.size() - 1);
//		messageAdapter.notifyItemChanged(messageList.size() - 1);
		Message menuMessage = new Message(content, Message.TYPE_COT, Message.SENDER_RECEIVER);
		setMessageList(menuMessage);
		BroadcastUtils.INSTANCE.completeHonorCardBroadcast();
	}

	private void sendMessage(String content) {
//		messageAdapter.notifyItemRangeChanged(messageList.size() - 3,3);
		Message message = new Message(content, Message.TYPE_TEXT, Message.SENDER_USER);
		setMessageList(message);
	}

	void simulateImage(Bitmap bitmap) {
		Message message = new Message(bitmap, null, Message.TYPE_IMAGE, Message.SENDER_USER);
		setMessageList(message);
	}

	public void simulateReplyImages(ArrayList<String> imagesPath) {
		Message message = new Message(imagesPath, Message.TYPE_IMAGES, Message.SENDER_RECEIVER);
		setMessageList(message);
	}

	public void simulateReplyImage(String imagePath) {
		Message message = new Message(imagePath, Message.TYPE_IMAGE, Message.SENDER_RECEIVER);
		setMessageList(message);
	}

	public void simulateReplyMedia(String mediaTitle, String mediaUrl) {
		Message message = new Message(mediaTitle, mediaUrl, Message.TYPE_MEDIA, Message.SENDER_RECEIVER);
//		if (messageAdapter != null && !messageList.isEmpty()) {
//			messageList.remove(messageList.size() - 1);
//			messageAdapter.notifyItemRemoved(messageList.size() - 1);
//			messageAdapter.notifyItemChanged(messageList.size() - 1, 1);
//		}
		setMessageList(message);
	}

	public void replyPermissionCard(PermissionType type) {
		int typeCard = -1;
		if (type == PermissionType.ACCESSIBILITY) {
			typeCard = Message.TYPE_ACCESSIBILITY_CARD;
		} else if (type == PermissionType.AUDIO) {
			typeCard = Message.TYPE_AUDIO_CARD;
		} else if (type == PermissionType.FLOAT) {
			typeCard = Message.TYPE_FLOAT_CARD;
		} else if (type == PermissionType.READ_PHONE_STATE) {
			typeCard = Message.TYPE_READ_STATUS_CARD;
		}
		Message message = new Message("", typeCard, Message.SENDER_RECEIVER, type);
		setMessageList(message);
	}

	public void simulateReply(String text, boolean shouldMerge, boolean isShowBtn) {
		if (!shouldMerge || currentReply == null) {
			currentReply = new Message(text, Message.TYPE_TEXT, Message.SENDER_RECEIVER);
			currentReply.setShowButtons(isShowBtn);
			setMessageList(currentReply);
		} else {
			currentReply.appendContent(text);
			currentReply.setShowButtons(isShowBtn);
//			messageAdapter.notifyItemChanged(messageList.size() - 1);
//			messageAdapter.notifyItemChanged(messageList.size() - 3);
			BroadcastUtils.INSTANCE.viewScrollBottomBroadcast();
		}
	}

	// 结束回复时调用（例如用户输入新内容时）
	public void finishCurrentReply() {
		currentReply = null;
	}

	public void stopHonorCardMessage() {
		if (messageList.isEmpty()) {
			return;
		}
		messageList.remove(messageList.size() - 1);
//		messageAdapter.notifyItemRemoved(messageList.size() - 1);
//		messageAdapter.notifyItemChanged(messageList.size() - 1, 1);
	}

//	public boolean setShareType(ShareItem item) {
//		return messageAdapter.setShareType(item);
//	}
}