package com.fxzs.lingxiagent.util.media;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * FileTypeDetector单元测试
 */
public class FileTypeDetectorTest {
    
    @Test
    public void testDetectImageFileTypes() {
        // 测试各种图片文件扩展名
        assertEquals(FileTypeDetector.FileType.IMAGE, 
                FileTypeDetector.detectFileType("image.jpg", null));
        assertEquals(FileTypeDetector.FileType.IMAGE, 
                FileTypeDetector.detectFileType("photo.jpeg", null));
        assertEquals(FileTypeDetector.FileType.IMAGE, 
                FileTypeDetector.detectFileType("screenshot.png", null));
        assertEquals(FileTypeDetector.FileType.IMAGE, 
                FileTypeDetector.detectFileType("animation.gif", null));
        assertEquals(FileTypeDetector.FileType.IMAGE, 
                FileTypeDetector.detectFileType("modern.webp", null));
        assertEquals(FileTypeDetector.FileType.IMAGE, 
                FileTypeDetector.detectFileType("bitmap.bmp", null));
    }
    
    @Test
    public void testDetectDocumentFileTypes() {
        // 测试各种文档文件扩展名
        assertEquals(FileTypeDetector.FileType.DOCUMENT, 
                FileTypeDetector.detectFileType("document.pdf", null));
        assertEquals(FileTypeDetector.FileType.DOCUMENT, 
                FileTypeDetector.detectFileType("report.doc", null));
        assertEquals(FileTypeDetector.FileType.DOCUMENT, 
                FileTypeDetector.detectFileType("report.docx", null));
        assertEquals(FileTypeDetector.FileType.DOCUMENT, 
                FileTypeDetector.detectFileType("presentation.ppt", null));
        assertEquals(FileTypeDetector.FileType.DOCUMENT, 
                FileTypeDetector.detectFileType("presentation.pptx", null));
        assertEquals(FileTypeDetector.FileType.DOCUMENT, 
                FileTypeDetector.detectFileType("spreadsheet.xls", null));
        assertEquals(FileTypeDetector.FileType.DOCUMENT, 
                FileTypeDetector.detectFileType("spreadsheet.xlsx", null));
        assertEquals(FileTypeDetector.FileType.DOCUMENT, 
                FileTypeDetector.detectFileType("readme.txt", null));
    }
    
    @Test
    public void testCaseInsensitiveDetection() {
        // 测试大小写混合的文件名
        assertEquals(FileTypeDetector.FileType.IMAGE, 
                FileTypeDetector.detectFileType("Image.JPG", null));
        assertEquals(FileTypeDetector.FileType.IMAGE, 
                FileTypeDetector.detectFileType("PHOTO.PNG", null));
        assertEquals(FileTypeDetector.FileType.DOCUMENT, 
                FileTypeDetector.detectFileType("Document.PDF", null));
        assertEquals(FileTypeDetector.FileType.DOCUMENT, 
                FileTypeDetector.detectFileType("Report.DOCX", null));
    }
    
    @Test
    public void testBoundaryConditions() {
        // 测试边界情况
        assertEquals(FileTypeDetector.FileType.UNKNOWN, 
                FileTypeDetector.detectFileType(null, null));
        assertEquals(FileTypeDetector.FileType.UNKNOWN, 
                FileTypeDetector.detectFileType("", null));
        assertEquals(FileTypeDetector.FileType.UNKNOWN, 
                FileTypeDetector.detectFileType("filename", null)); // 无扩展名
        assertEquals(FileTypeDetector.FileType.UNKNOWN, 
                FileTypeDetector.detectFileType("filename.", null)); // 空扩展名
        assertEquals(FileTypeDetector.FileType.UNKNOWN, 
                FileTypeDetector.detectFileType("file.unknown", null)); // 未知扩展名
    }
    
    @Test
    public void testSpecialCharactersInFileName() {
        // 测试包含特殊字符的文件名
        assertEquals(FileTypeDetector.FileType.IMAGE, 
                FileTypeDetector.detectFileType("file-name_with spaces.jpg", null));
        assertEquals(FileTypeDetector.FileType.DOCUMENT, 
                FileTypeDetector.detectFileType("文档名称.pdf", null));
        assertEquals(FileTypeDetector.FileType.IMAGE, 
                FileTypeDetector.detectFileType("image@2x.png", null));
    }
    
    @Test
    public void testMimeTypeDetection() {
        // 测试通过MIME类型检测
        assertEquals(FileTypeDetector.FileType.IMAGE, 
                FileTypeDetector.detectFileType("unknown", "image/jpeg"));
        assertEquals(FileTypeDetector.FileType.IMAGE, 
                FileTypeDetector.detectFileType("unknown", "image/png"));
        assertEquals(FileTypeDetector.FileType.DOCUMENT, 
                FileTypeDetector.detectFileType("unknown", "application/pdf"));
        assertEquals(FileTypeDetector.FileType.DOCUMENT, 
                FileTypeDetector.detectFileType("unknown", "text/plain"));
    }
    
    @Test
    public void testGetMimeType() {
        // 测试MIME类型获取
        assertEquals("image/jpeg", FileTypeDetector.getMimeType("photo.jpg"));
        assertEquals("image/png", FileTypeDetector.getMimeType("screenshot.png"));
        assertEquals("application/pdf", FileTypeDetector.getMimeType("document.pdf"));
        assertEquals("application/octet-stream", FileTypeDetector.getMimeType("unknown.xyz"));
        assertEquals("application/octet-stream", FileTypeDetector.getMimeType("noextension"));
    }
    
    @Test
    public void testIsImageFile() {
        // 测试图片文件判断
        assertTrue(FileTypeDetector.isImageFile("photo.jpg"));
        assertTrue(FileTypeDetector.isImageFile("image.PNG"));
        assertFalse(FileTypeDetector.isImageFile("document.pdf"));
        assertFalse(FileTypeDetector.isImageFile("unknown.xyz"));
        assertFalse(FileTypeDetector.isImageFile("noextension"));
    }
    
    @Test
    public void testIsDocumentFile() {
        // 测试文档文件判断
        assertTrue(FileTypeDetector.isDocumentFile("document.pdf"));
        assertTrue(FileTypeDetector.isDocumentFile("report.DOCX"));
        assertFalse(FileTypeDetector.isDocumentFile("photo.jpg"));
        assertFalse(FileTypeDetector.isDocumentFile("unknown.xyz"));
        assertFalse(FileTypeDetector.isDocumentFile("noextension"));
    }
}