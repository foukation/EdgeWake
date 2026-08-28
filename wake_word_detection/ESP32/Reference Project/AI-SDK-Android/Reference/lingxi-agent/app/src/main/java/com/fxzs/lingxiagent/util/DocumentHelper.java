package com.fxzs.lingxiagent.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.text.TextUtils;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;
import com.fxzs.lingxiagent.view.chat.ChatAdapter;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;

import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import timber.log.Timber;

/**
 * 文档生成工具类
 * 功能：生成TXT、PDF和Word文档
 */
public class DocumentHelper {

    private Context context;
    private OnDocumentGeneratedListener listener;

    public DocumentHelper(Context context) {
        this.context = context;
    }

    public interface OnDocumentGeneratedListener {
        void onSuccess(String filePath);

        void onFailure(String errorMessage);
    }

    public void setOnDocumentGeneratedListener(OnDocumentGeneratedListener listener) {
        this.listener = listener;
    }

    /**
     * 生成文本文件(.doc)
     *
     * @param text     要保存的文本内容
     * @param fileName 文件名（不带扩展名）
     */
    public void generateTextFile(String text, String fileName, OnDocumentGeneratedListener listener) {
        new Thread(() -> {
            try {
                String con = filterEmojiText(text);
                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                File appDir = new File(dir, context.getPackageName());
                if (!appDir.exists() && !appDir.mkdirs()) {
                    Toast.makeText(context, "无法创建目录", Toast.LENGTH_SHORT).show();
                    if (listener != null) listener.onFailure("无法创建目录");
                    return;
                }
                File file = new File(appDir, fileName + ".doc");
                FileOutputStream fos = new FileOutputStream(file);
                // 使用UTF-8编码写入，确保换行符正确保存
                fos.write(con.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                fos.close();
                if (listener != null) listener.onSuccess(file.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
                if (listener != null) listener.onFailure(e.getMessage());
            }
        }).start();
    }

    /**
     * 创建包含文本和图像的PDF文件
     *
     * @param datas    要添加到PDF中的数据
     * @param fileName 生成的PDF文件名，不包括扩展名
     */
    public void createPdfWithTextAndImage(List<ChatMessage> datas, String fileName, OnDocumentGeneratedListener listener) {
        new Thread(() -> {
            File file = null;
            PdfDocument pdf = null;
            Document document = null;
            try {
                // 验证输入参数
                if (datas == null || datas.isEmpty()) {
                    Timber.tag("DocumentHelper").w( "Export failed: No messages selected");
                    if (listener != null) {
                        listener.onFailure("没有可导出的消息内容");
                    }
                    return;
                }
                
                Timber.tag("DocumentHelper").d( "Starting PDF export with " + datas.size() + " messages");
                
                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                File appDir = new File(dir, context.getPackageName());
                if (!appDir.exists() && !appDir.mkdirs()) {
                    if (listener != null) {
                        listener.onFailure("无法创建目录");
                    }
                    return;
                }
                file = new File(appDir, fileName + ".pdf");
                PdfWriter writer = new PdfWriter(file);
                pdf = new PdfDocument(writer);
                document = new Document(pdf);

                // 使用更安全的字体创建方式
                PdfFont font = createSafePdfFont();
                
                for (ChatMessage data : datas) {
                    if (data == null) continue; // 跳过空消息
                    
                    if (data.getMsgType() == ChatAdapter.TYPE_USER_FILE_IMAGE || data.getMsgType() == ChatAdapter.TYPE_ASSISTANT_IMG) {
                        // 处理图片消息
                        if (data.getImageList() != null && !data.getImageList().isEmpty()) {
                            try {
                                Bitmap image = loadImageSync(data.getImageList().get(0));
                                if (image != null) {
                                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                    image.compress(Bitmap.CompressFormat.PNG, 100, stream);
                                    ImageData imageData = ImageDataFactory.create(stream.toByteArray());
                                    Image pdfImage = new Image(imageData);
                                    pdfImage.setAutoScale(true);
                                    document.add(pdfImage);
                                    stream.close();
                                } else {
                                    // 图片加载失败，添加占位文本
                                    document.add(new Paragraph("[图片加载失败]").setFont(font));
                                }
                            } catch (Exception e) {
                                Timber.tag("DocumentHelper").w( "Failed to add image to PDF: " + e.getMessage());
                                document.add(new Paragraph("[图片处理失败]").setFont(font));
                            }
                        }
                    } else {
                        // 处理文本消息
                        String text = getShareText(data);
                        if (!TextUtils.isEmpty(text)) {
                            addTextToPdf(document, text, font);
                        }
                    }
                }
                
                document.close();
                if (listener != null) {
                    listener.onSuccess(file.getAbsolutePath());
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onFailure("导出异常：" + e.getMessage());
                }
                if (file != null && file.exists()) {
                    file.delete();
                }
                e.printStackTrace();
            } finally {
                // 确保资源被正确关闭
                try {
                    if (document != null) document.close();
                    if (pdf != null) pdf.close();
                } catch (Exception e) {
                    Timber.tag("DocumentHelper").w( "Error closing PDF resources: " + e.getMessage());
                }
            }
        }).start();
    }

    /**
     * 创建安全的PDF字体，使用多重回退机制
     * @return 可用的PDF字体
     * @throws Exception 如果无法创建任何字体
     */
    private PdfFont createSafePdfFont() throws Exception {
        // 尝试多种字体方案，按优先级顺序
        String[] fontOptions = {
            // 首先尝试使用自定义字体（如果存在）
            "assets/fonts/jjyh.ttf",
            // 尝试常见的中文字体
            "STSong-Light",
            // 使用标准字体
            "Helvetica"
        };
        
        String[] encodings = {
            "UniGB-UCS2-H",
            "Identity-H",
            null // 标准编码
        };
        
        // 首先尝试从assets加载自定义字体
        try {
            // 正确的assets路径
            java.io.InputStream fontStream = context.getAssets().open("fonts/jjyh.ttf");
            byte[] fontBytes = new byte[fontStream.available()];
            fontStream.read(fontBytes);
            fontStream.close();
            return PdfFontFactory.createFont(fontBytes, "Identity-H");
        } catch (Exception e) {
            Timber.tag("DocumentHelper").d( "Failed to load custom font from assets: " + e.getMessage());
        }
        
        // 尝试标准字体选项
        for (String fontName : fontOptions) {
            for (String encoding : encodings) {
                try {
                    if (encoding != null) {
                        return PdfFontFactory.createFont(fontName, encoding);
                    } else {
                        return PdfFontFactory.createFont(fontName);
                    }
                } catch (Exception e) {
                    Timber.tag("DocumentHelper").d( 
                        String.format("Failed to create font %s with encoding %s: %s", 
                        fontName, encoding, e.getMessage()));
                }
            }
        }
        
        // 最后的回退方案：使用系统默认字体
        try {
            return PdfFontFactory.createFont();
        } catch (Exception e) {
            throw new Exception("无法创建PDF字体，请检查系统字体配置", e);
        }
    }

    private void addTextToPdf(Document document, String text, PdfFont font) {
        if (TextUtils.isEmpty(text) || document == null || font == null) return;

        try {
            // 清理文本内容，移除可能导致问题的字符
            String cleanText = filterEmojiText(text);
            
            // 按段落分割
            String[] paragraphs = cleanText.split("\n\n");
            for (String paragraph : paragraphs) {
                if (!TextUtils.isEmpty(paragraph)) {
                    // 每个段落内部的换行处理
                    String[] lines = paragraph.split("\n");
                    for (int i = 0; i < lines.length; i++) {
                        if (!TextUtils.isEmpty(lines[i])) {
                            try {
                                // 安全地添加段落，处理可能的字体渲染问题
                                Paragraph p = new Paragraph(lines[i]).setFont(font);
                                document.add(p);
                            } catch (Exception e) {
                                Timber.tag("DocumentHelper").w( "Failed to add paragraph: " + e.getMessage());
                                // 如果字体有问题，尝试不设置字体
                                try {
                                    document.add(new Paragraph(lines[i]));
                                } catch (Exception e2) {
                                    Timber.tag("DocumentHelper").w( "Failed to add paragraph without font: " + e2.getMessage());
                                    // 完全跳过这一行
                                }
                            }
                        }
                        // 如果不是最后一行，添加一个小的段落间距
                        if (i < lines.length - 1) {
                            try {
                                document.add(new Paragraph(" ").setFont(font).setFontSize(4));
                            } catch (Exception e) {
                                // 间距添加失败不是致命错误
                            }
                        }
                    }
                    // 段落之间添加更大的间距
                    try {
                        document.add(new Paragraph(" ").setFont(font).setFontSize(8));
                    } catch (Exception e) {
                        // 间距添加失败不是致命错误
                    }
                }
            }
        } catch (Exception e) {
            Timber.tag("DocumentHelper").e( "Error adding text to PDF: " + e.getMessage());
            // 尝试添加错误信息
            try {
                document.add(new Paragraph("[文本内容处理失败]"));
            } catch (Exception e2) {
                // 如果连这个都失败了，那就放弃
                Timber.tag("DocumentHelper").e( "Critical error in PDF text addition");
            }
        }
    }

    private String filterEmojiText(String text) {
        if (TextUtils.isEmpty(text)) {
            return text;
        }

        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            int codePoint = text.codePointAt(i);
            int charCount = Character.charCount(codePoint);

            // 检查是否为emoji相关字符
            if (isEmojiCharacter(codePoint)) {
                // 跳过emoji字符
                i += charCount;
            } else {
                // 保留非emoji字符
                result.append(Character.toChars(codePoint));
                i += charCount;
            }
        }

        return result.toString()
                // 移除其他可能导致问题的特殊字符
                .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "")
                // 规范化空白字符
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean isEmojiCharacter(int codePoint) {
        return (codePoint >= 0x1F600 && codePoint <= 0x1F64F) ||  // Emoticons
                (codePoint >= 0x1F300 && codePoint <= 0x1F5FF) ||  // Misc Symbols and Pictographs
                (codePoint >= 0x1F680 && codePoint <= 0x1F6FF) ||  // Transport and Map
                (codePoint >= 0x1F1E0 && codePoint <= 0x1F1FF) ||  // Regional country flags
                (codePoint >= 0x2600 && codePoint <= 0x26FF) ||    // Misc symbols
                (codePoint >= 0x2700 && codePoint <= 0x27BF) ||    // Dingbats
                (codePoint >= 0xFE00 && codePoint <= 0xFE0F) ||    // Variation selectors
                (codePoint >= 0x1F900 && codePoint <= 0x1F9FF) ||  // Supplemental Symbols and Pictographs
                (codePoint >= 0x1F3FB && codePoint <= 0x1F3FF);    // Skin tones
    }


    /**
     * 创建包含文本和图片的Word文档(.docx)
     *
     * @param datas    要添加到Word中的数据
     * @param fileName 生成的Word文件名，不包括扩展名
     */
    public void createWordWithTextAndImage(List<ChatMessage> datas, String fileName, OnDocumentGeneratedListener listener) {
        new Thread(() -> {
            FileOutputStream fos = null;
            XWPFDocument document = null;
            File file = null;
            try {
                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                File appDir = new File(dir, context.getPackageName());

                if (!appDir.exists() && !appDir.mkdirs()) {
                    if (listener != null) {
                        listener.onFailure("无法创建目录");
                    }
                    return;
                }

                file = new File(appDir, fileName + ".docx");
                fos = new FileOutputStream(file);

                document = new XWPFDocument();

                for (ChatMessage data : datas) {
                    if (data.getMsgType() == ChatAdapter.TYPE_USER_FILE_IMAGE || data.getMsgType() == ChatAdapter.TYPE_ASSISTANT_IMG) {
                        String imageUrl = data.getImageList().get(0);
                        Bitmap bitmap = loadImageSync(imageUrl);
                        if (bitmap != null) {
                            addImageToDocument(document, bitmap);
                        } else {
                            addTextToDocument(document, "[图片加载失败: " + imageUrl + "]");
                        }
                    } else {
                        addTextToDocument(document, getShareText(data));
                    }
                }

                document.write(fos);
                if (listener != null) {
                    listener.onSuccess(file.getAbsolutePath());
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onFailure("导出异常：" + e.getMessage());
                }
                e.printStackTrace();
            } finally {
                try {
                    if (document != null) document.close();
                    if (fos != null) fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 创建包含文本的Word文档(.docx)
     *
     * @param report    要添加到Word中的数据
     * @param fileName 生成的Word文件名，不包括扩展名
     */
    public void createWordWithText(String report, String fileName) {
        new Thread(() -> {
            FileOutputStream fos = null;
            XWPFDocument document = null;
            File file = null;
            try {
                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                File appDir = new File(dir, context.getPackageName());

                if (!appDir.exists() && !appDir.mkdirs()) {
                    Toast.makeText(context, "无法创建目录", Toast.LENGTH_SHORT);
                    return;
                }

                file = new File(appDir, fileName + ".docx");
                fos = new FileOutputStream(file);

                document = new XWPFDocument();


                addTextToDocument(document, report);
                document.write(fos);
                if (listener != null) {
                    listener.onSuccess(file.getAbsolutePath());
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onFailure("导出异常：" + e.getMessage());
                }
                if (file != null)
                    file.delete();
                e.printStackTrace();
            } finally {
                try {
                    if (document != null) document.close();
                    if (fos != null) fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private Bitmap loadImageSync(String imageUrl) {
        try {
            FutureTarget<Bitmap> future = Glide.with(context)
                    .asBitmap()
                    .load(imageUrl)
                    .submit();

            Bitmap bitmap = future.get();
            future.cancel(false);
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void addTextToDocument(XWPFDocument document, String text) {
        String[] lines = text.split("\n");
        for (String line : lines) {
            XWPFParagraph paragraph = document.createParagraph();
            paragraph.setAlignment(ParagraphAlignment.LEFT);
            XWPFRun run = paragraph.createRun();

            try {
                run.setText(line);
                run.addBreak();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void addImageToDocument(XWPFDocument document, Bitmap bitmap) throws Exception {
        if (bitmap == null) return;

        Bitmap resizedBitmap = resizeBitmap(bitmap, 600, 600);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos);

        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        paragraph.setAlignment(ParagraphAlignment.CENTER);

        run.addPicture(
                new ByteArrayInputStream(bos.toByteArray()),
                XWPFDocument.PICTURE_TYPE_JPEG,
                "image.jpg",
                Units.toEMU(resizedBitmap.getWidth()),
                Units.toEMU(resizedBitmap.getHeight())
        );

        XWPFParagraph emptyParagraph = document.createParagraph();
        emptyParagraph.createRun().addBreak();
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap;
        }

        float ratio = Math.min((float) maxWidth / width, (float) maxHeight / height);
        int newWidth = (int) (width * ratio);
        int newHeight = (int) (height * ratio);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    private String getShareText(ChatMessage data) {
        if (data == null) {
            return "";
        }
        
        String result = "";
        String message = data.getMessage();
        
        // 确保消息内容不为null
        if (message == null) {
            message = "";
        }
        
        try {
            if (data.getMsgType() == ChatAdapter.TYPE_USER) {
                result = "用户：" + "\n" + filterEmojiText(message) + "\n\n";
            } else if (!TextUtils.isEmpty(message)) {
                result = "灵犀：" + "\n" + filterEmojiText(message) + "\n\n";
            }
        } catch (Exception e) {
            Timber.tag("DocumentHelper").w( "Error formatting share text: " + e.getMessage());
            // 回退到基本格式
            result = filterEmojiText(message != null ? message : "") + "\n\n";
        }
        
        return result != null ? result : "";
    }
}