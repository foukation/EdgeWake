package com.fxzs.lingxiagent.util;

import android.content.Context;
import android.graphics.Picture;
import android.graphics.Rect;
import android.graphics.drawable.PictureDrawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.SimpleResource;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.module.AppGlideModule;
import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;

import java.io.IOException;
import java.io.InputStream;


@GlideModule
public class SVGModule extends AppGlideModule {

    @Override
    public void registerComponents(Context context, Glide glide, Registry registry) {
        registry.register(SVG.class, PictureDrawable.class, new SvgDrawableTranscoder())
                .append(InputStream.class, SVG.class, new SvgDecoder());
    }

    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }

    private static class SvgDrawableTranscoder implements ResourceTranscoder<SVG, PictureDrawable> {

        private final PictureDrawableUtils mPictureDrawableUtils = new PictureDrawableUtils();

        @Override
        public Resource<PictureDrawable> transcode(Resource<SVG> resource, Options options) {
            SVG svg = resource.get();
            Picture picture = svg.renderToPicture();
            PictureDrawable drawable = mPictureDrawableUtils.pictureDrawableFromPicture(picture);
            return new SimpleResource<>(drawable);
        }
    }

    private static class SvgDecoder implements ResourceDecoder<InputStream, SVG> {

        private final SVGParser mSvgParser = new SVGParser();

        @Override
        public boolean handles(@NonNull InputStream source, @NonNull Options options) throws IOException {
            // 只处理 SVG 格式的图片
            return true;
        }

        @Nullable
        @Override
        public Resource<SVG> decode(@NonNull InputStream source, int width, int height, @NonNull Options options)
                throws IOException {
            SVG svg = null;
            try {
                svg = mSvgParser.parseFromInputStream(source);
            } catch (SVGParseException e) {
                e.printStackTrace();
            }
            return new SimpleResource<>(svg);
        }
    }

    private static class PictureDrawableUtils {

        private static final float MAX_SCALE_FACTOR = 1.0f;

        private final Rect mTmpRect = new Rect();

        PictureDrawable pictureDrawableFromPicture(Picture picture) {
            PictureDrawable drawable = new PictureDrawable(picture);
            drawable.setBounds(0, 0, picture.getWidth(), picture.getHeight());
            return drawable;
        }

        // 计算图片的缩放比例
        float getScaleFactor(int srcWidth, int srcHeight, int targetWidth, int targetHeight) {
            return Math.min(MAX_SCALE_FACTOR, Math.min((float) targetWidth / (float) srcWidth,
                    (float) targetHeight / (float) srcHeight));
        }

        // 计算图片的目标矩形区域
        Rect computeTargetRect(int srcWidth, int srcHeight, int targetWidth, int targetHeight) {
            float scaleFactor = getScaleFactor(srcWidth, srcHeight, targetWidth, targetHeight);
            int scaledWidth = (int) (scaleFactor * srcWidth);
            int scaledHeight = (int) (scaleFactor * srcHeight);
            int left = (targetWidth - scaledWidth) / 2;
            int top = (targetHeight - scaledHeight) / 2;
            mTmpRect.set(left, top, left + scaledWidth, top + scaledHeight);
            return mTmpRect;
        }
    }

    private static class SVGParser {

        private SVG parseFromInputStream(InputStream inputStream)
                throws SVGParseException{
            try {
                SVG svg = SVG.getFromInputStream(inputStream);
                // 强制转换为指定宽度和高度的尺寸，以便后续的图片处理
                svg.setDocumentWidth(PictureDrawableUtils.MAX_SCALE_FACTOR);
                svg.setDocumentHeight(PictureDrawableUtils.MAX_SCALE_FACTOR);
                return svg;
            } catch (SVGParseException ex) {
                throw ex;
            }
        }
    }
}
