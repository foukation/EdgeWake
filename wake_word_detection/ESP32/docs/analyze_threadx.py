import pandas as pd
import sys
sys.stdout.reconfigure(encoding='utf-8')

# 读取去重后的Excel文件
df = pd.read_excel('docs/适配产品信息汇总表V1_去重.xlsx', sheet_name=0)

os_col = '操作系统'
chip_col = '芯片型号'
brand_col = '产品品牌'

print('=' * 80)
print('ThreadX 深度分析')
print('=' * 80)

# 获取所有 ThreadX 的记录
threadx_mask = df[os_col].astype(str).str.contains('ThreadX|threadx|THREADX|Threadx', case=False, na=False)
threadx_df = df[threadx_mask]

print(f'\nThreadX 总数: {len(threadx_df)}')

print('\n【ThreadX 详细列表】')
for idx, row in threadx_df.iterrows():
    print(f"  序号{row['序号']}: {row[brand_col]} | {row[chip_col]} | OS: {row[os_col]}")

print('\n' + '=' * 80)
print('【ThreadX 是什么？】')
print('=' * 80)

print('''
ThreadX 的背景：

1. **ThreadX 是商业 RTOS**
   - 原本由 Express Logic 公司开发
   - 2019年被微软收购
   - 2020年微软将其开源，更名为 Azure RTOS ThreadX

2. **ThreadX 的特点**
   - 高性能、小内存占用
   - 主要用于嵌入式设备
   - 被广泛用于各种芯片平台

3. **与"厂商封闭RTOS"的区别**
   - ThreadX 是一个明确的、有名字的 RTOS
   - "厂商封闭RTOS" 是指那些只写了 "RTOS" 没有具体名字的
   - ThreadX 虽然现在开源了，但在这些产品中可能用的是商业授权版本

4. **为什么 ASR 芯片同时有 "厂商封闭RTOS" 和 "ThreadX"？**
   - ASR 芯片支持多种 RTOS
   - 有些产品选择用 ASR 自带的封闭 RTOS
   - 有些产品选择用 ThreadX
''')

print('\n' + '=' * 80)
print('【分类建议】')
print('=' * 80)

print('''
当前分类方式：

| 分类 | 说明 |
|------|------|
| 厂商封闭RTOS | Excel中只写"RTOS"，没有具体名字，通常是芯片厂商的封闭系统 |
| FreeRTOS | 明确标注为 FreeRTOS 的产品，开源 RTOS |
| ThreadX | 明确标注为 ThreadX 的产品，商业/开源 RTOS |
| Zephyr | 明确标注为 Zephyr 的产品，开源 RTOS |
| LiteOS | 明确标注为 LiteOS 的产品，华为 RTOS |
| RTX | 明确标注为 RTX 的产品，ARM Keil RTOS |
| RT-Thread | 明确标注为 RT-Thread 的产品，国产开源 RTOS |

这个分类是按照 Excel 中填写的操作系统名称来区分的，而不是按照开源/封闭来区分。

如果按开源/封闭来分类：

| 分类 | RTOS类型 | 数量 |
|------|----------|------|
| **封闭/商业** | 厂商封闭RTOS + ThreadX | 32 + 11 = 43 |
| **开源** | FreeRTOS + Zephyr + LiteOS + RT-Thread | 19 + 3 + 3 + 1 = 26 |
| **ARM商业** | RTX | 1 |
''')
