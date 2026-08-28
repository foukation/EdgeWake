import pandas as pd
import sys
sys.stdout.reconfigure(encoding='utf-8')

# 读取去重后的Excel文件
df = pd.read_excel('docs/适配产品信息汇总表V1_去重.xlsx', sheet_name=0)

os_col = '操作系统'
chip_col = '芯片型号'
brand_col = '产品品牌'
product_col = '产品名称（包含厂商、型号）'

print('=' * 80)
print('"RTOS (通用/厂商自带)" 深度分析')
print('=' * 80)

# 获取所有 RTOS (通用) 的记录
rtos_generic_mask = (
    df[os_col].astype(str).str.contains('RTOS', case=False, na=False) &
    ~df[os_col].astype(str).str.contains('FreeRTOS|freertos|Freertos|FreeROTS', case=False, na=False) &
    ~df[os_col].astype(str).str.contains('ThreadX|threadx|THREADX|Threadx', case=False, na=False) &
    ~df[os_col].astype(str).str.contains('Zephyr', case=False, na=False) &
    ~df[os_col].astype(str).str.contains('LiteOS|liteos', case=False, na=False) &
    ~df[os_col].astype(str).str.contains('^RTX$', case=True, na=False, regex=True) &
    ~df[os_col].astype(str).str.contains('RT-Thread', case=False, na=False)
)

rtos_generic_df = df[rtos_generic_mask]

print(f'\n总数: {len(rtos_generic_df)}')

print('\n' + '=' * 80)
print('【这32条记录的操作系统字段原始值】')
print('=' * 80)

os_values = rtos_generic_df[os_col].value_counts()
for val, count in os_values.items():
    print(f'  "{val}": {count}')

print('\n' + '=' * 80)
print('【为什么叫"通用/厂商自带"？】')
print('=' * 80)

print('''
这32条记录的特点是：

1. 操作系统字段只填写了 "RTOS" 或类似的模糊描述
2. 没有明确指出是哪种具体的 RTOS（如 FreeRTOS、ThreadX 等）
3. 这些通常是芯片厂商提供的封闭/半封闭 RTOS

让我们看看这些记录的具体情况：
''')

print('\n' + '=' * 80)
print('【按芯片厂商分类详细列表】')
print('=' * 80)

# ASR
print('\n【ASR (翱捷科技) - 12个】')
asr_df = rtos_generic_df[rtos_generic_df[chip_col].astype(str).str.contains('ASR', case=False, na=False)]
for idx, row in asr_df.iterrows():
    print(f"  序号{row['序号']}: {row[brand_col]} | {row[chip_col]} | OS: {row[os_col]}")

# 展锐
print('\n【展锐 (Unisoc) - 6个】')
unisoc_df = rtos_generic_df[rtos_generic_df[chip_col].astype(str).str.contains('展锐|Unisoc|UWS|W217', case=False, na=False)]
for idx, row in unisoc_df.iterrows():
    print(f"  序号{row['序号']}: {row[brand_col]} | {row[chip_col]} | OS: {row[os_col]}")

# 杰理
print('\n【杰理 (JL) - 6个】')
jl_df = rtos_generic_df[rtos_generic_df[chip_col].astype(str).str.contains('JL|AC79|AC69|杰理|AB5656', case=False, na=False)]
for idx, row in jl_df.iterrows():
    print(f"  序号{row['序号']}: {row[brand_col]} | {row[chip_col]} | OS: {row[os_col]}")

# BES
print('\n【恒玄 (BES) - 2个】')
bes_df = rtos_generic_df[rtos_generic_df[chip_col].astype(str).str.contains('BES', case=False, na=False)]
for idx, row in bes_df.iterrows():
    print(f"  序号{row['序号']}: {row[brand_col]} | {row[chip_col]} | OS: {row[os_col]}")

# 其他
print('\n【其他 - 6个】')
other_df = rtos_generic_df[~rtos_generic_df[chip_col].astype(str).str.contains('ASR|展锐|Unisoc|UWS|W217|JL|AC79|AC69|杰理|AB5656|BES', case=False, na=False)]
for idx, row in other_df.iterrows():
    print(f"  序号{row['序号']}: {row[brand_col]} | {row[chip_col]} | OS: {row[os_col]}")

print('\n' + '=' * 80)
print('【结论】')
print('=' * 80)

print('''
"RTOS (通用/厂商自带)" 的含义：

1. **"通用"** 指的是：
   - Excel 中操作系统字段只填写了 "RTOS" 这个通用词
   - 没有具体指明是 FreeRTOS、ThreadX 还是其他

2. **"厂商自带"** 指的是：
   - 这些芯片厂商（ASR、展锐、杰理等）都有自己的封闭 RTOS
   - 例如：ASR 有自己的 RTOS，展锐有 UNC_RTOS/MOCOR，杰理有自己的实时内核
   - 这些 RTOS 通常不开源，是芯片厂商随芯片提供的

3. **与 FreeRTOS/ThreadX 的区别**：
   - FreeRTOS、ThreadX 是明确标注的开源/商业 RTOS
   - "RTOS (通用)" 是没有明确标注具体类型的，通常是厂商封闭系统

4. **命名建议**：
   - 可以改为 "厂商封闭RTOS" 或 "芯片厂商RTOS"
   - 或者 "RTOS (未指定具体类型)"
''')
