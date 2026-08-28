package com.fxzs.lingxiagent.lingxi.help;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;

import java.util.ArrayList;
import java.util.List;

public class HelpTabFragment extends Fragment {
    private RecyclerView recyclerView;

    public static HelpTabFragment newInstance(List<String> content) {
        HelpTabFragment fragment = new HelpTabFragment();
        Bundle args = new Bundle();
        args.putStringArrayList("content", new ArrayList<>(content));
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_function_help_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        recyclerView = view.findViewById(R.id.recyclerView);
        List<String> content = getArguments() != null ? 
            getArguments().getStringArrayList("content") : new ArrayList<>();

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        HelpContentAdapter adapter = new HelpContentAdapter(content, getContext());
        recyclerView.setItemViewCacheSize(20);
        recyclerView.setAdapter(adapter);
        // 设置点击监听
        // 处理点击事件
        adapter.setOnItemClickListener(this::handleItemClick);
    }

    private void handleItemClick(int position, String item) {
        // 显示Toast
        //ToastUtils.longCall("点击了: " + item);

        // 或者跳转到详情页面
        // Intent intent = new Intent(getActivity(), DetailActivity.class);
        // intent.putExtra("item", item);
        // startActivity(intent);

        // 也可以根据不同的position执行不同的操作
        switch (position) {
            case 0:
                // 处理第一个item的点击
                break;
            case 1:
                // 处理第二个item的点击
                break;
            // 其他case...
        }
    }
}