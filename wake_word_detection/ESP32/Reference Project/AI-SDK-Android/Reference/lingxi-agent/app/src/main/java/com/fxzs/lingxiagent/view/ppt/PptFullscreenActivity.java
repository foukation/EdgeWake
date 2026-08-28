package com.fxzs.lingxiagent.view.ppt;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.ppt.dto.PptSlide;

import java.util.ArrayList;
import java.util.List;

/**
 * Fullscreen activity for viewing PPT slides with zoom support
 */
public class PptFullscreenActivity extends AppCompatActivity {
    
    private static final String EXTRA_SLIDES = "extra_slides";
    private static final String EXTRA_CURRENT_INDEX = "extra_current_index";
    private static final String EXTRA_PPT_TITLE = "extra_ppt_title";
    
    private ViewPager2 slideViewPager;
    private ImageButton backButton;
    private TextView slideCounter;
    private View controlsOverlay;
    
    private FullscreenSlideAdapter slideAdapter;

    private List<PptSlide> slides;
    private int currentIndex;
    private String pptTitle;
    
    private boolean controlsVisible = true;
    private Runnable hideControlsRunnable = this::hideControls;
    
    public static void start(Context context, List<PptSlide> slides, int currentIndex, String pptTitle) {
        Intent intent = new Intent(context, PptFullscreenActivity.class);
        intent.putExtra(EXTRA_SLIDES, new ArrayList<>(slides));
        intent.putExtra(EXTRA_CURRENT_INDEX, currentIndex);
        intent.putExtra(EXTRA_PPT_TITLE, pptTitle);
        context.startActivity(intent);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Enable fullscreen mode
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        
        setContentView(R.layout.activity_ppt_fullscreen);
        
        // Get data from intent
        slides = (List<PptSlide>) getIntent().getSerializableExtra(EXTRA_SLIDES);
        currentIndex = getIntent().getIntExtra(EXTRA_CURRENT_INDEX, 0);
        pptTitle = getIntent().getStringExtra(EXTRA_PPT_TITLE);
        
        if (slides == null) {
            finish();
            return;
        }
        
        initializeViews();
        setupViewPager();
        setupZoomHelper();
        
        // Auto-hide controls after 3 seconds
        scheduleHideControls();
    }
    
    private void initializeViews() {
        slideViewPager = findViewById(R.id.slide_view_pager);
        backButton = findViewById(R.id.back_button);
        // slideCounter = findViewById(R.id.page_number_text); // 已移除此UI元素
        controlsOverlay = findViewById(R.id.controls_overlay);
        
        backButton.setOnClickListener(v -> finish());
        
        // Update slide counter
        updateSlideCounter(currentIndex);
        
        // Set up click listener to toggle controls
        slideViewPager.setOnClickListener(v -> toggleControls());
    }
    
    private void setupViewPager() {
        slideAdapter = new FullscreenSlideAdapter();
        slideAdapter.setSlides(slides);
        slideViewPager.setAdapter(slideAdapter);
        slideViewPager.setCurrentItem(currentIndex, false);
        
        slideViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentIndex = position;
                updateSlideCounter(position);
                
                // Show controls briefly when changing slides
                showControls();
                scheduleHideControls();
            }
        });
    }
    
    // Simple adapter for fullscreen slides
    private static class FullscreenSlideAdapter extends RecyclerView.Adapter<FullscreenSlideAdapter.FullscreenSlideViewHolder> {
        private List<PptSlide> slides = new ArrayList<>();
        
        void setSlides(List<PptSlide> slides) {
            this.slides = slides != null ? slides : new ArrayList<>();
            notifyDataSetChanged();
        }
        
        @NonNull
        @Override
        public FullscreenSlideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ppt_slide, parent, false);
            return new FullscreenSlideViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull FullscreenSlideViewHolder holder, int position) {
            if (position < slides.size()) {
                holder.bind(slides.get(position));
            }
        }
        
        @Override
        public int getItemCount() {
            return slides.size();
        }
        
        static class FullscreenSlideViewHolder extends RecyclerView.ViewHolder {
            TextView slideTitle;
            TextView slideContent;
            
            FullscreenSlideViewHolder(@NonNull View itemView) {
                super(itemView);
                slideTitle = itemView.findViewById(R.id.slide_title);
                slideContent = itemView.findViewById(R.id.slide_content);
            }
            
            void bind(PptSlide slide) {
                if (slide == null) return;
                
                slideTitle.setText(slide.getTitle());
                
                if (slide.getContent() != null && !slide.getContent().trim().isEmpty()) {
                    slideContent.setVisibility(View.VISIBLE);
                    slideContent.setText(slide.getContent());
                } else {
                    slideContent.setVisibility(View.GONE);
                }
                
                // Set styling based on slide type
                switch (slide.getType()) {
                    case COVER:
                        slideTitle.setTextSize(36);
                        break;
                    case SECTION:
                        slideTitle.setTextSize(32);
                        break;
                    case CONTENT:
                        slideTitle.setTextSize(28);
                        break;
                }
            }
        }
    }
    
    private void setupZoomHelper() {
        // Simplified zoom helper setup
        // The actual zoom functionality is handled by PhotoView in the slide items
    }
    
    private void updateSlideCounter(int position) {
        String counterText = (position + 1) + " / " + slides.size();
        slideCounter.setText(counterText);
    }
    
    private void toggleControls() {
        if (controlsVisible) {
            hideControls();
        } else {
            showControls();
            scheduleHideControls();
        }
    }
    
    private void showControls() {
        controlsOverlay.setVisibility(View.VISIBLE);
        controlsOverlay.animate()
            .alpha(1.0f)
            .setDuration(200)
            .start();
        controlsVisible = true;
    }
    
    private void hideControls() {
        controlsOverlay.animate()
            .alpha(0.0f)
            .setDuration(200)
            .withEndAction(() -> {
                controlsOverlay.setVisibility(View.GONE);
                controlsVisible = false;
            })
            .start();
    }
    
    private void scheduleHideControls() {
        // Remove any pending hide operations
        controlsOverlay.removeCallbacks(hideControlsRunnable);
        // Schedule new hide operation
        controlsOverlay.postDelayed(hideControlsRunnable, 3000);
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (controlsOverlay != null) {
            controlsOverlay.removeCallbacks(hideControlsRunnable);
        }
    }
}