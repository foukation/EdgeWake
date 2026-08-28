import pandas as pd
import sys
sys.stdout.reconfigure(encoding='utf-8')

# 读取原始Excel文件
df_original = pd.read_excel('docs/适配产品信息汇总表V1.xlsx', sheet_name=0)

os_col = '操作系统'

print('=' * 80)
print('原始 Excel (V1.xlsx) 完整操作系统分布')
print('=' * 80)

print(f'\n总记录数: {len(df_original)}')

# 完整的操作系统分布
print('\n【完整操作系统分布】')
os_counts = df_original[os_col].value_counts(dropna=False)
print(os_counts.to_string())

print('\n' + '=' * 80)
print('与文档 2.2 节对比')
print('=' * 80)

# 文档中 2.2 节的数据
doc_22_data = {
    'RTOS (通用/未指定)': 31,
    'FreeRTOS': 7,
    'freertos (小写)': 3,
    'Freertos': 2,
    'FreeRTOS (ESP-IDF)': 2,
    'ESP-IDF FreeRTOS': 1,
    'FreeRTOS+Linux': 1,
    'Qualcomm FreeRTOS': 1,
    'ThreadX': 5,
    'ThreadX V5.1': 2,
    'threadX/Threadx': 3,
    'Zephyr': 2,
    'Zephyr RTOS': 1,
    'LiteOS-M': 1,
    'RTOS 海思 LiteOS': 1,
    'RTX': 1,
    'RT-Thread': 1,
}

print('\n对比结果:')
print(f'{"文档条目":<40} {"文档值":<8} {"Excel值":<8} {"匹配"}')
print('-' * 70)

# 映射文档条目到Excel中的实际值
mapping = {
    'RTOS (通用/未指定)': 'RTOS',
    'FreeRTOS': 'FreeRTOS',
    'freertos (小写)': 'freertos',
    'Freertos': 'Freertos',
    'FreeRTOS (ESP-IDF)': 'FreeRTOS (ESP-IDF 5.4.0)',
    'ESP-IDF FreeRTOS': 'ESP-IDF FreeRTOS',
    'FreeRTOS+Linux': 'FreeRTOS+Linux',
    'Qualcomm FreeRTOS': 'Qualcomm FreeRTOS',
    'ThreadX': 'ThreadX',
    'ThreadX V5.1': 'ThreadX V5.1',
    'Zephyr': 'Zephyr',
    'Zephyr RTOS': 'Zephyr RTOS',
    'LiteOS-M': 'LiteOS-M',
    'RTOS 海思 LiteOS': 'RTOS 海思 LiteOS',
    'RTX': 'RTX',
    'RT-Thread': 'RT-Thread',
}

for doc_key, doc_val in doc_22_data.items():
    excel_key = mapping.get(doc_key, doc_key)
    excel_val = os_counts.get(excel_key, 0)
    match = '✓' if doc_val == excel_val else '✗'
    print(f'{doc_key:<40} {doc_val:<8} {excel_val:<8} {match}')

# 特殊处理 threadX/Threadx
print('\n【特殊情况: threadX/Threadx】')
threadx_variants = ['threadX', 'Threadx', 'threadx', 'THREADX']
for v in threadx_variants:
    if v in os_counts.index:
        print(f'  "{v}": {os_counts[v]}')

# 统计 RTOS (通用) 的实际数量
print('\n【RTOS (通用) 详细分析】')
rtos_generic_mask = (
    df_original[os_col].astype(str).str.contains('RTOS', case=False, na=False) &
    ~df_original[os_col].astype(str).str.contains('FreeRTOS|freertos|Freertos', case=False, na=False) &
    ~df_original[os_col].astype(str).str.contains('ThreadX|threadx|THREADX|Threadx', case=False, na=False) &
    ~df_original[os_col].astype(str).str.contains('Zephyr', case=False, na=False) &
    ~df_original[os_col].astype(str).str.contains('LiteOS|liteos', case=False, na=False) &
    ~df_original[os_col].astype(str).str.contains('^RTX$', case=True, na=False, regex=True) &
    ~df_original[os_col].astype(str).str.contains('RT-Thread', case=False, na=False)
)
rtos_generic_count = rtos_generic_mask.sum()
print(f'  RTOS (通用/厂商自带) 实际数量: {rtos_generic_count}')
print(f'  文档中的数量: 31')

# 列出所有 RTOS (通用) 的具体值
print('\n  具体值分布:')
rtos_generic_df = df_original[rtos_generic_mask]
rtos_generic_values = rtos_generic_df[os_col].value_counts()
for val, count in rtos_generic_values.items():
    print(f'    "{val}": {count}')
