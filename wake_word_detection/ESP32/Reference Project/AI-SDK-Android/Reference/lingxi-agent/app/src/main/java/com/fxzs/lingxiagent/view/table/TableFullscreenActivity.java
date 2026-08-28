package com.fxzs.lingxiagent.view.table;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.util.ZUtil.CodeBlockPlugin;
import com.fxzs.lingxiagent.util.ZUtil.MarkdownUtils;
import com.fxzs.lingxiagent.util.markdown.TableBlockContainerView;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;

import java.util.Arrays;
import java.util.List;

import io.noties.markwon.Markwon;
import io.noties.markwon.recycler.MarkwonAdapter;
import io.noties.markwon.recycler.table.TableEntry;

/**
 * 表格全屏查看Activity
 * 支持横屏显示，提供更好的表格查看体验
 */
public class TableFullscreenActivity extends AppCompatActivity {
    
    private static final String EXTRA_TABLE_CONTENT = "extra_table_content";
    
    private TextView tvTableTitleFullscreen;
    private CardView cvBack;
    private CardView cvCopyFullscreen;
    private CardView cvShare;
    private View tableContentContainer;
    
    private String tableContent;
    
    public static void start(Context context, String tableContent) {
        Intent intent = new Intent(context, TableFullscreenActivity.class);
        intent.putExtra(EXTRA_TABLE_CONTENT, tableContent);
        context.startActivity(intent);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 设置为横屏模式，更适合查看表格
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        
        setContentView(R.layout.activity_table_fullscreen);
        
        initViews();
        initData();
        setupListeners();
    }
    
    private void initViews() {
        tvTableTitleFullscreen = findViewById(R.id.tv_table_title_fullscreen);
        cvBack = findViewById(R.id.cv_back);
        cvCopyFullscreen = findViewById(R.id.cv_copy_fullscreen);
        cvShare = findViewById(R.id.cv_share);
        tableContentContainer = findViewById(R.id.table_content_container);
    }
    
    private void initData() {
        tableContent = getIntent().getStringExtra(EXTRA_TABLE_CONTENT);
        
        if (tableContent == null) {
            tableContent = "";
        }
        
        tvTableTitleFullscreen.setText("表格");
        
        // 渲染表格内容
        renderTableContent();
    }
    
    private void renderTableContent() {
        try {
            if (tableContentContainer instanceof FrameLayout) {
                FrameLayout container = (FrameLayout) tableContentContainer;
                container.removeAllViews();

                // 统一使用 TableBlockContainerView 渲染
                List<Extension> exts = Arrays.asList(TablesExtension.create());
                Parser parser = Parser.builder().extensions(exts).build();
                Node doc = parser.parse(tableContent != null ? tableContent : "");
                TableBlock tableBlock = findFirstTableBlock(doc);
                if (tableBlock != null) {
                    TableBlockContainerView tableView = new TableBlockContainerView(this);
                    tableView.setTableMarkdown(tableContent);
                    tableView.setHeaderVisible(false); // 全屏隐藏头部，避免重复
                    tableView.renderTable(tableBlock);

                    container.addView(tableView, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    ));
                    return;
                }

                // 兜底：保留原 Markwon 渲染方案
                RecyclerView recyclerView = new RecyclerView(this);
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
                Markwon markwon = MarkdownUtils.createMarkwon(this);
                MarkwonAdapter adapter = MarkwonAdapter.builder(
                        R.layout.item_default,
                        R.id.text_view
                    )
                    .include(TableBlock.class, TableEntry.create(builder ->
                        builder
                            .tableLayout(R.layout.mobile_style_table_final, R.id.mobile_style_table_final)
                            .textLayoutIsRoot(R.layout.mobile_table_cell)
                    ))
                    .build();
                adapter.setMarkdown(markwon, tableContent);
                recyclerView.setAdapter(adapter);
                container.addView(recyclerView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                ));
            }
        } catch (Exception e) {
            // 如果渲染失败，显示原始内容
            TextView errorView = new TextView(this);
            errorView.setText("表格渲染失败: " + e.getMessage() + "\n\n原始内容:\n" + tableContent);
            errorView.setPadding(16, 16, 16, 16);
            errorView.setTextSize(14);
            errorView.setTextColor(getResources().getColor(R.color.code_text));

            if (tableContentContainer instanceof FrameLayout) {
                ((FrameLayout) tableContentContainer).removeAllViews();
                ((FrameLayout) tableContentContainer).addView(errorView);
            }
        }
    }

    private TableBlock findFirstTableBlock(Node root) {
        if (root == null) return null;
        if (root instanceof TableBlock) return (TableBlock) root;
        for (Node n = root.getFirstChild(); n != null; n = n.getNext()) {
            TableBlock tb = findFirstTableBlock(n);
            if (tb != null) return tb;
        }
        return null;
    }
    
    private void setupListeners() {
        cvBack.setOnClickListener(v -> finish());
        
        cvCopyFullscreen.setOnClickListener(v -> copyTableToClipboard());
        
        cvShare.setOnClickListener(v -> shareTable());
    }
    
    private void copyTableToClipboard() {
        CodeBlockPlugin.copyCodeToClipboard(this, tableContent);
    }
    
    private void shareTable() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, tableContent);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "分享表格");
        
        Intent chooser = Intent.createChooser(shareIntent, "分享表格到");
        if (shareIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(chooser);
        }
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
