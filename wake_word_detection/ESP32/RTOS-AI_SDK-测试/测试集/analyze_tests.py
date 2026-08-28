#!/usr/bin/env python
# -*- coding: utf-8 -*-
import pandas as pd
import sys
import os

sys.stdout.reconfigure(encoding='utf-8')

os.chdir(r'E:\github\ESP32-RTOS-AI-SDK\RTOS-AI_SDK-测试\测试集')

# 1. 大模型问答测试集
print('='*60)
print('1. 大模型问答测试集.xlsx')
print('='*60)
df1 = pd.read_excel(r'语音助手/大模型问答测试集.xlsx')
print(f'总行数: {len(df1)}')
print(f'列名: {list(df1.columns)}')
print('\n前5行预览:')
print(df1.head(5))
print('\n数据分类统计:')
print(df1.iloc[:, 0].value_counts().head(10))

# 2. 文本翻译测试用例
print('\n' + '='*60)
print('2. 文本翻译/12月11号测试用例.xlsx')
print('='*60)
df2 = pd.read_excel(r'文本翻译/12月11号测试用例.xlsx')
print(f'总行数: {len(df2)}')
print(f'列名: {list(df2.columns)}')
print('\n前3行预览:')
print(df2.head(3))
if '目标语言' in df2.columns:
    print('\n目标语言分布:')
    print(df2['目标语言'].value_counts())

# 3. 流式摘要测试
print('\n' + '='*60)
print('3. 文本摘要/智能摘要API（流式）.xlsx')
print('='*60)
df3 = pd.read_excel(r'文本摘要/智能摘要API（流式）.xlsx')
print(f'总行数: {len(df3)}')
print(f'列名: {list(df3.columns)}')
print('\n全部数据:')
print(df3)

# 4. 非流式摘要测试
print('\n' + '='*60)
print('4. 文本摘要/智能摘要API（非流式）.xlsx')
print('='*60)
df4 = pd.read_excel(r'文本摘要/智能摘要API（非流式）.xlsx')
print(f'总行数: {len(df4)}')
print(f'列名: {list(df4.columns)}')
print('\n全部数据:')
print(df4)
