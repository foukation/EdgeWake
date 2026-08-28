package com.fxzs.lingxiagent.util.markdown;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.util.ZUtil.CodeBlockPlugin;
import com.fxzs.lingxiagent.view.table.TableFullscreenActivity;

import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TableBody;
import org.commonmark.ext.gfm.tables.TableCell;
import org.commonmark.ext.gfm.tables.TableHead;
import org.commonmark.node.Code;
import org.commonmark.node.Emphasis;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.StrongEmphasis;
import org.commonmark.node.Text;

/**
 * A compound view wrapping table with a header toolbar (title/copy/fullscreen)
 * and a horizontally scrollable TableLayout for mobile.
 */
public class TableBlockContainerView extends LinearLayout {

    private TextView tvTableTitle;
    private CardView cvCopy;
    private CardView cvFullscreen;
    private LinearLayout tableContainer;
    private View headerContainer;

    private String tableMarkdown = "";

    public TableBlockContainerView(Context context) {
        this(context, null);
    }

    public TableBlockContainerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        LayoutInflater.from(context).inflate(R.layout.item_table_advanced, this, true);
        tvTableTitle = findViewById(R.id.tv_table_title);
        cvCopy = findViewById(R.id.cv_copy);
        cvFullscreen = findViewById(R.id.cv_fullscreen);
        headerContainer = findViewById(R.id.header_container);
        View container = findViewById(R.id.table_container);
        if (container instanceof LinearLayout) {
            tableContainer = (LinearLayout) container;
        } else if (container instanceof ViewGroup) {
            // Fallback: wrap in LinearLayout
            tableContainer = new LinearLayout(getContext());
            ((ViewGroup) container).addView(tableContainer);
        }
        tvTableTitle.setText("表格");

        cvCopy.setOnClickListener(v -> CodeBlockPlugin.copyCodeToClipboard(getContext(), tableMarkdown));
        cvFullscreen.setOnClickListener(v -> TableFullscreenActivity.start(getContext(), tableMarkdown));
    }

    public void setTableMarkdown(String markdown) {
        this.tableMarkdown = markdown != null ? markdown : "";
    }

    /**
     * 控制头部工具栏显示/隐藏，用于全屏模式隐藏重复头部
     */
    public void setHeaderVisible(boolean visible) {
        if (headerContainer != null) {
            headerContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    public void renderTable(TableBlock tableBlock) {
        if (tableContainer == null) return;
        tableContainer.removeAllViews();

        HorizontalScrollView hsv = new HorizontalScrollView(getContext());
        hsv.setFillViewport(true);
        TableLayout tl = new TableLayout(getContext());
        tl.setStretchAllColumns(true);
        tl.setShrinkAllColumns(false);

        // iterate children of tableBlock: head and body
        Node n = tableBlock.getFirstChild();
        while (n != null) {
            if (n instanceof TableHead) {
                addTableSection(tl, (TableHead) n, true);
            } else if (n instanceof TableBody) {
                addTableSection(tl, (TableBody) n, false);
            }
            n = n.getNext();
        }

        hsv.addView(tl, new HorizontalScrollView.LayoutParams(
                HorizontalScrollView.LayoutParams.MATCH_PARENT,
                HorizontalScrollView.LayoutParams.WRAP_CONTENT
        ));
        tableContainer.addView(hsv, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
    }

    private void addTableSection(TableLayout tl, TableHead head, boolean isHeader) {
        for (Node r = head.getFirstChild(); r != null; r = r.getNext()) {
            if (r instanceof org.commonmark.ext.gfm.tables.TableRow) {
                TableRow row = new TableRow(getContext());
                row.setMinimumHeight(MarkdownStyle.dp(getContext(), MarkdownStyle.TABLE_ROW_HEIGHT_DP));
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                for (Node c = r.getFirstChild(); c != null; c = c.getNext()) {
                    if (c instanceof TableCell) {
                        TextView tv = buildTableCell((TableCell) c, true);
                        row.addView(tv);
                    }
                }
                tl.addView(row);
            }
        }
    }

    private void addTableSection(TableLayout tl, TableBody body, boolean isHeader) {
        for (Node r = body.getFirstChild(); r != null; r = r.getNext()) {
            if (r instanceof org.commonmark.ext.gfm.tables.TableRow) {
                TableRow row = new TableRow(getContext());
                row.setMinimumHeight(MarkdownStyle.dp(getContext(), MarkdownStyle.TABLE_ROW_HEIGHT_DP));
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                // zebra striping for body rows
                int rowIndex = tl.getChildCount(); // header rows already added; use current count for parity
                if (rowIndex % 2 == 0) {
                    row.setBackgroundColor(MarkdownStyle.TABLE_ROW_BG_EVEN);
                } else {
                    row.setBackgroundColor(MarkdownStyle.TABLE_ROW_BG_ODD);
                }
                for (Node c = r.getFirstChild(); c != null; c = c.getNext()) {
                    if (c instanceof TableCell) {
                        TextView tv = buildTableCell((TableCell) c, false);
                        row.addView(tv);
                    }
                }
                tl.addView(row);
            }
        }
    }

    private TextView buildTableCell(TableCell cell, boolean isHeader) {
        TextView tv = new TextView(getContext());
        tv.setText(spansFromInline(cell));
        tv.setTextSize(MarkdownStyle.BODY_TEXT_SIZE_SP);
        tv.setLineSpacing(0, 1.15f);
        tv.setPadding(ChatMarkdownRenderer.dpStatic(getContext(),12), ChatMarkdownRenderer.dpStatic(getContext(),6), ChatMarkdownRenderer.dpStatic(getContext(),12), ChatMarkdownRenderer.dpStatic(getContext(),6));
        tv.setTextColor(MarkdownStyle.BODY_COLOR);
        tv.setMinHeight(MarkdownStyle.dp(getContext(), MarkdownStyle.TABLE_ROW_HEIGHT_DP));
        tv.setGravity(android.view.Gravity.CENTER_VERTICAL);
        if (isHeader) {
            tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            tv.setBackgroundColor(MarkdownStyle.TABLE_HEADER_BG);
        }
        // alignment (horizontal)
        TableCell.Alignment align = cell.getAlignment();
        if (align == org.commonmark.ext.gfm.tables.TableCell.Alignment.CENTER) {
            tv.setGravity(tv.getGravity() | android.view.Gravity.CENTER_HORIZONTAL);
        } else if (align == org.commonmark.ext.gfm.tables.TableCell.Alignment.RIGHT) {
            tv.setGravity(tv.getGravity() | android.view.Gravity.END);
        } else {
            tv.setGravity(tv.getGravity() | android.view.Gravity.START);
        }
        return tv;
    }

    private CharSequence spansFromInline(Node container) {
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        buildInline(ssb, container);
        return ssb;
    }

    private void buildInline(SpannableStringBuilder ssb, Node container) {
        for (Node n = container.getFirstChild(); n != null; n = n.getNext()) {
            if (n instanceof Text) {
                ssb.append(((Text) n).getLiteral());
            } else if (n instanceof Emphasis) {
                int start = ssb.length();
                buildInline(ssb, n);
                ssb.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (n instanceof StrongEmphasis) {
                int start = ssb.length();
                buildInline(ssb, n);
                ssb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (n instanceof Code) {
                int start = ssb.length();
                ssb.append(((Code) n).getLiteral());
                ssb.setSpan(new BackgroundColorSpan(MarkdownStyle.INLINE_CODE_BG), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new ForegroundColorSpan(MarkdownStyle.INLINE_CODE_FG), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new TypefaceSpan("monospace"), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new RelativeSizeSpan(0.95f), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (n instanceof SoftLineBreak || n instanceof HardLineBreak) {
                ssb.append('\n');
            } else if (n instanceof Link) {
                int start = ssb.length();
                buildInline(ssb, n);
                ssb.setSpan(new ForegroundColorSpan(MarkdownStyle.LINK_COLOR), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new UnderlineSpan(), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                buildInline(ssb, n);
            }
        }
    }
}


