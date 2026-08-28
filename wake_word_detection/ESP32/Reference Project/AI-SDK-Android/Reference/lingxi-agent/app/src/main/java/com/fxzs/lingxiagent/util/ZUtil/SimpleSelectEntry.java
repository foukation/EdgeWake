package com.fxzs.lingxiagent.util.ZUtil;

import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

import com.fxzs.lingxiagent.view.dialog.TextSelectorView;

import org.commonmark.node.Node;

import java.util.HashMap;
import java.util.Map;

import io.noties.markwon.Markwon;
import io.noties.markwon.recycler.MarkwonAdapter;
import io.noties.markwon.utils.NoCopySpannableFactory;

public class SimpleSelectEntry extends MarkwonAdapter.Entry<Node, SimpleSelectEntry.Holder> {

    public static View.OnLongClickListener mLongClickListener;

    @NonNull
    public static SimpleSelectEntry createTextViewIsRoot(@LayoutRes int layoutResId) {
        return new SimpleSelectEntry(layoutResId, 0);
    }

    @NonNull
    public static SimpleSelectEntry create(@LayoutRes int layoutResId, @IdRes int textViewIdRes, View.OnLongClickListener listener) {
        mLongClickListener = listener;
        return new SimpleSelectEntry(layoutResId, textViewIdRes);
    }

    // small cache for already rendered nodes
    private final Map<Node, Spanned> cache = new HashMap<>();

    private final int layoutResId;
    private final int textViewIdRes;

    public SimpleSelectEntry(@LayoutRes int layoutResId, @IdRes int textViewIdRes) {
        this.layoutResId = layoutResId;
        this.textViewIdRes = textViewIdRes;
    }

    @NonNull
    @Override
    public SimpleSelectEntry.Holder createHolder(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) {
        return new SimpleSelectEntry.Holder(textViewIdRes, inflater.inflate(layoutResId, parent, false));
    }

    @Override
    public void bindHolder(@NonNull Markwon markwon, @NonNull SimpleSelectEntry.Holder holder, @NonNull Node node) {
        Spanned spanned = cache.get(node);
        if (spanned == null) {
            spanned = markwon.render(node);
            cache.put(node, spanned);
        }
        markwon.setParsedMarkdown(holder.textView, spanned);
    }

    @Override
    public void clear() {
        cache.clear();
    }

    public static class Holder extends MarkwonAdapter.Holder {

        final TextSelectorView textView;

        protected Holder(@IdRes int textViewIdRes, @NonNull View itemView) {
            super(itemView);

            final TextSelectorView textView;
            if (textViewIdRes == 0) {
                if (!(itemView instanceof TextSelectorView)) {
                    throw new IllegalStateException("TextView is not root of layout " +
                            "(specify TextView ID explicitly): " + itemView);
                }
                textView = (TextSelectorView) itemView;
            } else {
                textView = requireView(textViewIdRes);
            }
            this.textView = textView;
            this.textView.setSpannableFactory(NoCopySpannableFactory.getInstance());

            this.textView.setOnLongClickListener(mLongClickListener);
        }
    }
}