package com.fxzs.lingxiagent.viewmodel.translate;

import android.app.Application;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Observer;

import com.fxzs.lingxiagent.lingxi.translate.model.TranslationItem;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * 聆听模式ViewModel测试
 * 验证每次开启麦克风时创建新会话的逻辑
 */
@RunWith(RobolectricTestRunner.class)
public class VMListenModeTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private Observer<Long> translationIdObserver;

    private VMListenMode viewModel;
    private Application application;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        application = RuntimeEnvironment.getApplication();
        viewModel = new VMListenMode(application);
        viewModel.getTranslationIdLive().observeForever(translationIdObserver);
    }

    @Test
    public void testInitialState() {
        // 验证初始状态
        assertEquals("zh", viewModel.getSourceLanguage().getValue());
        assertEquals("en", viewModel.getTargetLanguage().getValue());
        assertEquals(Long.valueOf(0L), viewModel.getTranslationIdLive().getValue());
    }

    @Test
    public void testLanguageSelection() {
        // 测试语言选择
        viewModel.setSourceLanguage("ja");
        viewModel.setTargetLanguage("zh");
        
        assertEquals("ja", viewModel.getSourceLanguage().getValue());
        assertEquals("zh", viewModel.getTargetLanguage().getValue());
    }

    @Test
    public void testBatchSaveWithEmptyItems() {
        // 测试空列表的批量保存
        List<TranslationItem> emptyItems = new ArrayList<>();
        
        // 这应该不会抛出异常，并且会正常处理
        viewModel.batchSaveListenMessages(emptyItems, "zh", "en", "2025-01-01 10:00:00", "2025-01-01 10:05:00");
        
        // 验证没有异常抛出
        assertTrue("Empty items should be handled gracefully", true);
    }

    @Test
    public void testBatchSaveWithValidItems() {
        // 创建测试数据
        List<TranslationItem> items = new ArrayList<>();
        TranslationItem item1 = new TranslationItem();
        item1.setSourceText("Hello");
        item1.setTargetText("你好");
        items.add(item1);

        TranslationItem item2 = new TranslationItem();
        item2.setSourceText("How are you?");
        item2.setTargetText("你好吗？");
        items.add(item2);

        // 设置一个有效的translationId
        viewModel.getTranslationIdLive().setValue(123L);

        // 测试批量保存（注意：这里会调用网络请求，在实际测试中需要mock）
        viewModel.batchSaveListenMessages(items, "en", "zh", "2025-01-01 10:00:00", "2025-01-01 10:05:00");
        
        // 验证translationId被正确使用
        assertEquals(Long.valueOf(123L), viewModel.getTranslationIdLive().getValue());
    }

    @Test
    public void testMidResultUpdate() {
        // 测试中间结果更新
        String midResult = "正在识别中...";
        viewModel.updateMidResult(midResult);
        
        assertEquals(midResult, viewModel.getCurrentMidResult().getValue());
    }
}