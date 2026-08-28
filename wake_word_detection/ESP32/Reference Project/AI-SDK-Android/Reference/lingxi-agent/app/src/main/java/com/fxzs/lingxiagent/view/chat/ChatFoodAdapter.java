package com.fxzs.lingxiagent.view.chat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.lingxi.service_api.data.FoodItem;
import com.fxzs.lingxiagent.view.common.GlobalToast;
import com.fxzs.lingxiagent.view.common.NoMultiClickListener;

import java.util.ArrayList;
import java.util.Objects;

public class ChatFoodAdapter extends RecyclerView.Adapter<ChatFoodAdapter.ViewHolder> {
	private final ArrayList<FoodItem> honorMenuList;

	public ChatFoodAdapter(ArrayList<FoodItem> honorMenuList) {
		this.honorMenuList = honorMenuList;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.lingxi_card_party_restaurant_item, parent, false);
		return new ViewHolder(view);
	}

	private boolean isAppInstalled(Context context, String PackageName) {
		PackageManager pm = context.getPackageManager();
		try {
			pm.getPackageInfo(PackageName, PackageManager.GET_ACTIVITIES);
			return true;
		} catch (PackageManager.NameNotFoundException e) {
			return false;
		}
	}

	@SuppressLint("SetTextI18n")
	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		FoodItem curItem = honorMenuList.get(position);

		String address = curItem.getAddress();
		String title = curItem.getTitle();
		String imgUrl = curItem.getImg();
		String score = curItem.getScore();
		String[] tag = curItem.getTag().toArray(new String[0]);
		String[] subTag = curItem.getSubTag().toArray(new String[0]);

		holder.itemView.setOnClickListener(new NoMultiClickListener() {
			@Override
			public void onNoMultiClick(View view) {
				String pkgNameGd = curItem.getButtonLink().getNativeApp().getPkgName();
				String urlGd = curItem.getButtonLink().getNativeApp().getUrl();
				String pkgNameBd = "com.baidu.BaiduMap";
				String urlBd = "baidumap://map/direction?origin=&destination=" + address + title + "&mode=driving&region=&src=ios.baidu.openAPIdemo";
				if (isAppInstalled(view.getContext(), pkgNameGd)) {
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlGd));
					intent.setPackage(pkgNameGd);
					view.getContext().startActivity(intent);
				} else if (isAppInstalled(view.getContext(), pkgNameBd)) {
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlBd));
					intent.setPackage(pkgNameBd);
					view.getContext().startActivity(intent);
				} else {
					GlobalToast.show((Activity) view.getContext(), "请先安装高德地图或百度地图应用", GlobalToast.Type.NORMAL);
				}
			}
		});

		if (!Objects.equals(imgUrl, "")) {
			Glide.with(holder.itemView.getContext()).load(imgUrl).centerCrop().into(holder.imgView);
		} else {
			holder.imgView.setImageResource(R.drawable.rest);
		}

		holder.titleView.setText(title);
		holder.addressView.setText(address);
		holder.scoreView.setText(score + "分");
		holder.busTimeView.setText(subTag[0]);
		if (tag.length > 0) {
			holder.tag1.setText(tag[0]);
			if (tag.length > 1) {
				holder.tag2.setText(tag[1]);
			} else {
				holder.tag2.setVisibility(View.GONE);
			}
		} else {
			holder.tag1.setVisibility(View.GONE);
			holder.tag2.setVisibility(View.GONE);
		}

		holder.itemView.setPadding(20, 0, 20, 0);
	}

	@Override
	public int getItemCount() {
		return honorMenuList.size();
	}

	static class ViewHolder extends RecyclerView.ViewHolder {
		View itemView;
		ImageView imgView;
		TextView titleView;
		TextView busTimeView;
		TextView addressView;
		TextView scoreView;
		TextView tag1;
		TextView tag2;

		public ViewHolder(@NonNull View itemView) {
			super(itemView);
			this.itemView = itemView;
			imgView = itemView.findViewById(R.id.trip_item_img);
			titleView = itemView.findViewById(R.id.trip_item_title);
			busTimeView = itemView.findViewById(R.id.trip_item_bus_time);
			addressView = itemView.findViewById(R.id.trip_item_address);
			scoreView = itemView.findViewById(R.id.trip_item_score);
			tag1 = itemView.findViewById(R.id.trip_item_tag1);
			tag2 = itemView.findViewById(R.id.trip_item_tag2);
		}
	}
}