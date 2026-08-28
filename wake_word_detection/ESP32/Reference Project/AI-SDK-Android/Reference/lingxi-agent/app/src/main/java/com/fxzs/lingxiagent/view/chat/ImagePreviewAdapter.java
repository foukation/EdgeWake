package com.fxzs.lingxiagent.view.chat;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.fxzs.lingxiagent.R;

import java.util.ArrayList;

public class ImagePreviewAdapter extends RecyclerView.Adapter<ImagePreviewAdapter.ViewHolder> {
	private final ArrayList<String> imageUrls;
	private final Context context;

	public ImagePreviewAdapter(Context context, ArrayList<String> urls) {
		this.context = context;
		this.imageUrls = urls;
	}

	public static class ViewHolder extends RecyclerView.ViewHolder {
		ImageView imageView;
		public ViewHolder(View view) {
			super(view);
			imageView = view.findViewById(R.id.imageView);
		}
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(context).inflate(R.layout.lingxi_img_preview_item, parent, false);
		return new ViewHolder(view);
	}

	@Override
	public void onBindViewHolder(ViewHolder holder, int position) {
		Glide.with(context)
				.load(imageUrls.get(position))
				.into(holder.imageView);
	}

	@Override
	public int getItemCount() {
		return imageUrls.size();
	}
}