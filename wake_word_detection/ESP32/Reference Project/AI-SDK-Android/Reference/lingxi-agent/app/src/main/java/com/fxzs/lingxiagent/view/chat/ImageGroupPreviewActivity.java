package com.fxzs.lingxiagent.view.chat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.util.ZUtil.DrawingActionUtils;
import com.fxzs.lingxiagent.util.ZUtil.ImageUtil;
import com.fxzs.lingxiagent.util.ZUtils;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.viewmodel.main.VMEmpty;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

import timber.log.Timber;

public class ImageGroupPreviewActivity extends BaseActivity {
	ArrayList<String> imageUrls;
	private int position = 0;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		imageUrls = getIntent().getStringArrayListExtra("imagesPath");
		position = getIntent().getIntExtra("position", 0);
		super.onCreate(savedInstanceState);
	}

	@Override
	protected int getLayoutResource() {
		return R.layout.lingxi_img_preview;
	}

	@Override
	protected Class getViewModelClass() {
		return VMEmpty.class;
	}

	@Override
	protected void setupDataBinding() {

	}

	@Override
	protected void initializeViews() {
		ViewPager2 viewPager = findViewById(R.id.viewPager);
		ImagePreviewAdapter adapter = new ImagePreviewAdapter(this, imageUrls);
		viewPager.setAdapter(adapter);
		viewPager.setCurrentItem(position, false);

		setTabText(position, imageUrls.size());
		viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
			@Override
			public void onPageSelected(int position) {
				setTabText(position, imageUrls.size());
			}
		});

		findViewById(R.id.closeBtn).setOnClickListener(v -> {
			finish();
		});

		findViewById(R.id.btnDownload).setOnClickListener(v -> {
			int pos = viewPager.getCurrentItem();
			String url = imageUrls.get(pos);
			DrawingActionUtils.performDownload(this, url);
		});

		findViewById(R.id.btnShare).setOnClickListener(v -> {
			int pos = viewPager.getCurrentItem();
			String url = imageUrls.get(pos);
			downloadImage(this, url);
		});
	}

	@Override
	protected void setupObservers() {

	}

	@SuppressLint("SetTextI18n")
	public void setTabText(int Left, int total) {
		TextView tabText = findViewById(R.id.tabTitle);
		tabText.setText((Left + 1) + "/" + total);
	}

	public void downloadImage(Context context, String imageUrl) {
		Glide.with(context)
				.asBitmap()
				.load(imageUrl)
				.into(new CustomTarget<Bitmap>() {
					@Override
					public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
						shareBitmap(context, resource);
					}

					@Override
					public void onLoadCleared(@Nullable Drawable placeholder) {
					}
				});
	}

	public static void shareBitmap(Context context, Bitmap bitmap) {
		try {
			File cachePath = new File(context.getCacheDir(), "images");
			cachePath.mkdirs();
			File imageFile = new File(cachePath, "lingxi_shared_image.png");
			FileOutputStream stream = new FileOutputStream(imageFile);
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
			stream.close();
			Uri contentUri = FileProvider.getUriForFile(
					context,
					context.getPackageName() + ".fileprovider",
					imageFile
			);

			Intent shareIntent = new Intent(Intent.ACTION_SEND);
			shareIntent.setType("image/*");
			shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
			shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			context.startActivity(Intent.createChooser(shareIntent, "分享图片"));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}