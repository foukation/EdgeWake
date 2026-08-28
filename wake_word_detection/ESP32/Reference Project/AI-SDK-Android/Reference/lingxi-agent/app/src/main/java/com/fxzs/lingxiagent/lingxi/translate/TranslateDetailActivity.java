package com.fxzs.lingxiagent.lingxi.translate;

import android.content.Intent;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.lingxi.translate.model.TranslateResult;
import com.fxzs.lingxiagent.util.ZUtil.Constant;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.viewmodel.translate.VMTranslateDetail;

public class TranslateDetailActivity extends BaseActivity<VMTranslateDetail> {
    TranslateDetailAdapter adapter;
    TextView tv_title;
    @Override
    protected int getLayoutResource() {
        return R.layout.activity_translate_detail;
    }

    @Override
    protected Class<VMTranslateDetail> getViewModelClass() {
        return VMTranslateDetail.class;
    }

    @Override
    protected void setupDataBinding() {

    }

    @Override
    protected void initializeViews() {
        ImageView closeIcon = findViewById(R.id.iv_back);
        closeIcon.setOnClickListener((view) -> finish());

        RecyclerView recyclerView = findViewById(R.id.rv);
         tv_title = findViewById(R.id.tv_title);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
         adapter = new TranslateDetailAdapter(this, viewModel.getTranslateResults().getValue(),
                new TranslateDetailAdapter.OnOptionSelectedListener() {
                    @Override
                    public void onOptionSelected(TranslateResult option) {

                    }
                });
        recyclerView.setAdapter(adapter);
        Intent intent = getIntent();
       String id = intent.getLongExtra(Constant.INTENT_ID,0)+"";
        String type =   intent.getStringExtra(Constant.INTENT_TYPE);

        viewModel.getList(id,type);
        if(type.equals("1")){
            tv_title.setText("同传聆听模式");
        }else {

            tv_title.setText("同传对话模式");
        }
    }

    @Override
    protected void setupObservers() {
        viewModel.getTranslateResults().observe(this,list->{
            adapter.setData(list);
        });
        viewModel.getTranslateType().observe(this,type->{
            if(type.equals("1")){
                tv_title.setText("同传聆听模式");
            }else {

                tv_title.setText("同传对话模式");
            }
        });
    }
}
