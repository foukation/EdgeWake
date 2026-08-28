package com.fxzs.lingxiagent.view.chat;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.lingxi.main.utils.GsonUtils;
import com.fxzs.lingxiagent.model.deepresearch.dto.DeepResearchItem;
import com.fxzs.lingxiagent.model.deepresearch.dto.Step;
import com.fxzs.lingxiagent.util.ZUtil.MarkdownUtils;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class DeepResearchAdapter extends RecyclerView.Adapter<DeepResearchAdapter.DeepResearchViewHolder> {

    private static final String TAG = "DeepResearchAdapter";
    private List<DeepResearchItem> items;
    private Context context;
    private OnItemClickListener onItemClickListener;
    private ObjectAnimator animator;
    private List<ObjectAnimator> list = new ArrayList<>();
    int think_count = 0;
    int report_count = 0;
    public interface AnimationCallback {
        void back(ObjectAnimator animator);
    }
    private AnimationCallback animationCallback = new AnimationCallback() {
        @Override
        public void back(ObjectAnimator animator) {
            list.add(animator);
        }
    };
    public DeepResearchAdapter(Context context, List<DeepResearchItem> items) {
        this.context = context;
        this.items = items;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void updateItems(List<DeepResearchItem> newItems, String step) {
        this.items = newItems;
        notifyItemChanged(newItems.size() -1);
        if(step.equals(Step.WEB_SEARCH.getAlias()) || step.equals(Step.REPORTING.getAlias())){
            notifyItemChanged(newItems.size() - 2);
            if (newItems.size() > 3) {
                notifyItemChanged(newItems.size() - 3);
            }
        }
    }

    @NonNull
    @Override
    public DeepResearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.deep_research_card, parent, false);
        return new DeepResearchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeepResearchViewHolder holder, int position) {
        MarkdownUtils.renderSmart(context,items.get(position).getThinkContent(),holder.tv_websearch_think);
//        holder.tv_websearch_think.setText(items.get(position).getThinkContent());
        if (items.get(position).getStatus() == 0){
            think_count++;

            if(think_count == 1){
                holder.rl_websearch.setVisibility(View.VISIBLE);
                holder.rl_websearch_think.setVisibility(View.VISIBLE);
                holder.view_line_bot.setVisibility(View.VISIBLE);
                holder.iv_item_title.setBackground(context.getDrawable(R.drawable.deep_research_gradient_ring));
//                RotationAnimationUtil.rotateIndefinitely(holder.iv_item_title, 2000,animationCallback);
                Animation rotation = AnimationUtils.loadAnimation(context, R.anim.rotate_animation);
                holder.iv_item_title.startAnimation(rotation);
            }
//            Timber.tag(TAG).d("onBindViewHolder think count %s list size = %s position = %s", think_count,list.size(),position);

        }else if(items.get(position).getStatus() == 1){
            think_count = 0;
            holder.rl_websearch.setVisibility(View.VISIBLE);
            holder.rl_websearch_think.setVisibility(View.VISIBLE);
            holder.view_line_bot.setVisibility(View.VISIBLE);
            holder.iv_item_title.setBackground(context.getDrawable(R.drawable.icon_done));
            holder.iv_item_title.clearAnimation();

//            Timber.tag(TAG).d("onBindViewHolder think count %s list size = %s position = %s", think_count,list.size(),position);
        }else if(items.get(position).getStatus() == 2){
            think_count = 0;
            report_count++;
            if(report_count == 1){

                holder.rl_websearch.setVisibility(View.GONE);
                holder.rl_websearch_think.setVisibility(View.GONE);
                holder.iv_item_title.setBackground(context.getDrawable(R.drawable.deep_research_gradient_ring));
//                RotationAnimationUtil.rotateIndefinitely(holder.iv_item_title, 2000,animationCallback);
                holder.view_line_bot.setVisibility(View.INVISIBLE);
                Animation rotation = AnimationUtils.loadAnimation(context, R.anim.rotate_animation);
                holder.iv_item_title.startAnimation(rotation);
            }
        }else if(items.get(position).getStatus() == 3){
            think_count = 0;
            Timber.tag(TAG).d("onBindViewHolder %s", GsonUtils.toJson(items.get(position)));
            holder.view_line_bot.setVisibility(View.INVISIBLE);
            holder.rl_websearch.setVisibility(View.GONE);
            holder.rl_websearch_think.setVisibility(View.GONE);
            holder.iv_item_title.setBackground(context.getDrawable(R.drawable.icon_report_done));
            holder.iv_item_title.clearAnimation();
        }

        if(null != items.get(position).getWeb_search()){
            holder.tv_item_title.setText(items.get(position).getWeb_search().getQuery());
            if( null != items.get(position).getWeb_search().getWeb_search()){
                if(items.get(position).getWeb_search().getWeb_search().size() > 0){
                    holder.rl_websearch.setVisibility(View.VISIBLE);
                    holder.tv_websearch.setText("已搜索"+items.get(position).getWeb_search().getWeb_search().size()+"网页");
//                    Timber.tag(TAG).d("onBindViewHolder think count %s list size = %s position = %s", think_count,list.size(),position);
                }else {
                    holder.rl_websearch.setVisibility(View.GONE);
                }
            }else{
                holder.rl_websearch.setVisibility(View.GONE);
            }
        } else {
            holder.rl_websearch.setVisibility(View.GONE);
        }


    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    public interface OnItemClickListener<T> {
        void onItemClick(T item);
    }

    @Override
    public void onViewRecycled(@NonNull DeepResearchViewHolder holder) {
        super.onViewRecycled(holder);
        holder.iv_item_title.clearAnimation();// 回收时清除动画
    }

    static class DeepResearchViewHolder extends RecyclerView.ViewHolder {

        RelativeLayout rl_item_title,rl_websearch,rl_websearch_think;
        ImageView iv_item_title;
        TextView tv_item_title,tv_websearch,tv_websearch_think,tv_more;
        Animation rotateAnimation;
        View view_line_bot;


        public DeepResearchViewHolder(@NonNull View itemView) {
            super(itemView);
            rl_item_title = itemView.findViewById(R.id.rl_item_title);
            iv_item_title = itemView.findViewById(R.id.iv_item_title);
            tv_item_title = itemView.findViewById(R.id.tv_item_title);
            rl_websearch = itemView.findViewById(R.id.rl_websearch);
            tv_websearch = itemView.findViewById(R.id.tv_websearch);
            rl_websearch_think = itemView.findViewById(R.id.rl_websearch_think);
            tv_websearch_think = itemView.findViewById(R.id.tv_websearch_think);
            tv_more = itemView.findViewById(R.id.tv_more);
            view_line_bot = itemView.findViewById(R.id.view_line_bot);
            rotateAnimation = AnimationUtils.loadAnimation(
                    itemView.getContext(), R.anim.rotate_animation);
        }
    }
}