package com.fxzs.lingxiagent.view.agent;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.chat.callback.OnItemClick;
import com.fxzs.lingxiagent.network.ZNet.ApiResponse;
import com.fxzs.lingxiagent.network.ZNet.HttpRequest;
import com.fxzs.lingxiagent.network.ZNet.bean.getCatDetailListBean;
import com.fxzs.lingxiagent.util.ZUtil.Constant;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import timber.log.Timber;

public class ZNSubFragment extends Fragment {
    private String TAG = "HistoryFragment";

    public static int TYPE_HISTORY = 1;

    int type;
    AgentAdapter adapter;
    RecyclerView recyclerView;

    List<getCatDetailListBean> list = new ArrayList<>();
//    private RefreshLayout refreshLayout;
    private int page = 0;
    private LinearLayout ad_container;

    HttpRequest request;
    
    public ZNSubFragment() {
        // Required empty public constructor
    }
    
    public ZNSubFragment(int type) {
        this();
        Bundle args = new Bundle();
        args.putInt("type", type);
        setArguments(args);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_zn_sub, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 从 arguments 获取 type 参数
        if (getArguments() != null) {
            type = getArguments().getInt("type", TYPE_HISTORY);
        }

        request = new HttpRequest();

        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));





        getData();



    }
    @Override
    public void onStart() {
        super.onStart();
//        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
//        EventBus.getDefault().unregister(this);
    }

//    @Subscribe(threadMode = ThreadMode.MAIN) // 确保在主线程中接收事件
//    @Subscribe(sticky = true)
//    public void onRefreshEvent(RefreshEvent event) {
//        Timber.tag(TAG).e("接收到信息");
//
//        getData();
//    }

    private void getData() {





        request.getCatDetailList(type, new Observer<ApiResponse<List<getCatDetailListBean>>>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(ApiResponse<List<getCatDetailListBean>> res) {
                if (res.getCode() == 0) {
                    List<getCatDetailListBean> listApiResponse = res.getData();

                    Timber.tag(TAG).e("type = "+type+" listApiResponse = "+listApiResponse.size());
                    list.clear();
                    list.addAll(listApiResponse);
                    adapter.notifyDataSetChanged();


                }
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });
        Timber.tag(TAG).e("type = "+type+" list = "+list);
//        Collections.sort(list, new Comparator<VideoData>() {
//            @Override
//            public int compare(VideoData v1, VideoData v2) {
//                return Long.compare(v2.getDate(), v1.getDate()); // 降序
//            }
//        });

        adapter = new AgentAdapter(getActivity(), type,list, new OnItemClick() {
            @Override
            public void onItemClick(int position) {
//                toDetail(list.get(position));
                getCatDetailListBean bean = list.get(position);
                Intent intent = new Intent(getActivity(), AgentContainActivity.class);
                intent.putExtra(Constant.INTENT_TYPE, AgentContainActivity.TYPE_AGENT);
//                intent.putExtra(Constant.INTENT_DATA, content);
//                intent.putExtra(Constant.INTENT_DATA1, selectOptionModel);
                intent.putExtra(Constant.INTENT_DATA2, bean);
                getActivity().startActivity(intent);
            }
        });
        recyclerView.setAdapter(adapter);

    }

}