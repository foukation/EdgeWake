package com.fxzs.lingxiagent.util.ZUtil;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.view.table.TableFullscreenActivity;

import org.commonmark.ext.gfm.tables.TableBlock;

import io.noties.markwon.Markwon;
import io.noties.markwon.recycler.MarkwonAdapter;
import io.noties.markwon.recycler.table.TableEntry;

/**
 * 高级表格Entry，支持复制和全屏功能
 * 包装原有的TableEntry，添加头部工具栏
 */
public class AdvancedTableEntry extends MarkwonAdapter.Entry<TableBlock, AdvancedTableEntry.Holder> {

    private final Context context;
    private final TableEntry innerTableEntry;
    private static String currentMarkdownContent = ""; // 存储当前的Markdown内容

    public static AdvancedTableEntry create(Context context, int tableLayoutRes, int tableLayoutId, int cellLayoutRes) {
        TableEntry innerEntry = TableEntry.create(builder ->
            builder
                .tableLayout(tableLayoutRes, tableLayoutId)
                .textLayoutIsRoot(cellLayoutRes)
        );
        return new AdvancedTableEntry(context, innerEntry);
    }

    /**
     * 设置当前处理的Markdown内容（用于提取表格内容）
     */
    public static void setCurrentMarkdownContent(String content) {
        currentMarkdownContent = content != null ? content : "";
    }

    private AdvancedTableEntry(Context context, TableEntry innerTableEntry) {
        this.context = context;
        this.innerTableEntry = innerTableEntry;
    }

    public int layoutResId() {
        return R.layout.item_table_advanced;
    }
    
    @Override
    public Holder createHolder(LayoutInflater inflater, ViewGroup parent) {
        View view = inflater.inflate(layoutResId(), parent, false);
        return new Holder(view);
    }
    
    @Override
    public void bindHolder(@NonNull Markwon markwon, @NonNull Holder holder, @NonNull TableBlock tableBlock) {
        // 设置表格标题
        holder.tvTableTitle.setText("表格");
        
        // 获取表格的Markdown内容
        String tableContent = extractTableContent(tableBlock);
        
        // 使用内部TableEntry渲染表格到容器中
        View tableContainer = holder.tableContainer;

        // 清空之前的内容
        if (tableContainer instanceof ViewGroup) {
            ((ViewGroup) tableContainer).removeAllViews();

            try {
                // 创建TableEntry的Holder
                TableEntry.Holder tableHolder = (TableEntry.Holder) innerTableEntry.createHolder(
                    LayoutInflater.from(context),
                    (ViewGroup) tableContainer
                );

                // 将表格视图添加到容器中
                ((ViewGroup) tableContainer).addView(tableHolder.itemView);

                // 绑定表格数据
                innerTableEntry.bindHolder(markwon, tableHolder, tableBlock);

            } catch (Exception e) {
                // 如果渲染失败，显示错误信息
                TextView errorView = new TextView(context);
                errorView.setText("表格渲染失败: " + e.getMessage());
                errorView.setPadding(16, 16, 16, 16);
                errorView.setTextColor(context.getResources().getColor(R.color.code_text));
                ((ViewGroup) tableContainer).addView(errorView);
            }
        }
        
        // 设置按钮点击事件
        final String finalTableContent = tableContent;
        
        holder.cvCopy.setOnClickListener(v -> 
            CodeBlockPlugin.copyCodeToClipboard(context, finalTableContent));
        
        holder.cvFullscreen.setOnClickListener(v -> 
            TableFullscreenActivity.start(context, finalTableContent));
    }
    
    /**
     * 提取表格的Markdown内容
     */
    private String extractTableContent(TableBlock tableBlock) {
        try {
            // 如果有当前的Markdown内容，尝试从中提取表格
            if (!currentMarkdownContent.isEmpty()) {
                // 简单的表格提取逻辑：查找包含 | 的行
                String[] lines = currentMarkdownContent.split("\n");
                StringBuilder tableContent = new StringBuilder();
                boolean inTable = false;

                for (String line : lines) {
                    line = line.trim();
                    if (line.contains("|") && !line.isEmpty()) {
                        if (!inTable) {
                            inTable = true;
                        }
                        tableContent.append(line).append("\n");
                    } else if (inTable && line.isEmpty()) {
                        // 表格结束
                        break;
                    } else if (inTable) {
                        // 表格结束
                        break;
                    }
                }

                if (tableContent.length() > 0) {
                    return tableContent.toString();
                }
            }

            // 如果无法提取，返回一个示例表格
            return "| 列1 | 列2 | 列3 |\n|-----|-----|-----|\n| 数据1 | 数据2 | 数据3 |\n| 数据4 | 数据5 | 数据6 |";

        } catch (Exception e) {
            return "| 错误 | 表格内容提取失败 |\n|------|------------------|\n| 原因 | " + e.getMessage() + " |";
        }
    }
    
    /**
     * ViewHolder for advanced table
     */
    public static class Holder extends MarkwonAdapter.Holder {
        
        final TextView tvTableTitle;
        final ImageView ivCopy;
        final ImageView ivFullscreen;
        final CardView cvCopy;
        final CardView cvFullscreen;
        final View tableContainer;
        
        public Holder(@NonNull View itemView) {
            super(itemView);
            tvTableTitle = itemView.findViewById(R.id.tv_table_title);
            ivCopy = itemView.findViewById(R.id.iv_copy);
            ivFullscreen = itemView.findViewById(R.id.iv_fullscreen);
            cvCopy = itemView.findViewById(R.id.cv_copy);
            cvFullscreen = itemView.findViewById(R.id.cv_fullscreen);
            tableContainer = itemView.findViewById(R.id.table_container);
        }
    }
}
