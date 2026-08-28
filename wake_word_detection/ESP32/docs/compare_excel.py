import pandas as pd
import sys
sys.stdout.reconfigure(encoding='utf-8')

# 读取原始Excel文件（未去重）
df_original = pd.read_excel('docs/适配产品信息汇总表V1.xlsx', sheet_name=0)

# 读取去重后的Excel文件
df_dedup = pd.read_excel('docs/适配产品信息汇总表V1_去重.xlsx', sheet_name=0)

os_col = '操作系统'

print('=' * 80)
print('对比：原始Excel vs 去重Excel')
print('=' * 80)

print(f'\n原始Excel (V1.xlsx) 总记录数: {len(df_original)}')
print(f'去重Excel (V1_去重.xlsx) 总记录数: {len(df_dedup)}')

print('\n' + '=' * 80)
print('FreeRTOS 相关数据对比')
print('=' * 80)

# 原始Excel中的FreeRTOS分布
print('\n【原始Excel (V1.xlsx)】')
orig_counts = df_original[os_col].value_counts()
freertos_keys = ['FreeRTOS', 'freertos', 'Freertos', 'FreeRTOS (ESP-IDF 5.4.0)', 
                 'FreeRTOS（ESP-IDF 内置，默认 RTOS）', 'ESP-IDF FreeRTOS', 
                 'FreeRTOS+Linux', 'Qualcomm FreeRTOS', 'FreeROTS',
                 '泰凌微实时内核 (裸机)（也可移植运行FreeRTOS）']

for val in freertos_keys:
    if val in orig_counts.index:
        print(f'  "{val}": {orig_counts[val]}')

# 去重Excel中的FreeRTOS分布
print('\n【去重Excel (V1_去重.xlsx)】')
dedup_counts = df_dedup[os_col].value_counts()
for val in freertos_keys:
    if val in dedup_counts.index:
        print(f'  "{val}": {dedup_counts[val]}')

# 文档中的数据
print('\n【文档中列出的数据】')
doc_data = [
    ('FreeRTOS', 7),
    ('freertos (小写)', 3),
    ('Freertos', 2),
    ('FreeRTOS (ESP-IDF)', 2),
    ('ESP-IDF FreeRTOS', 1),
    ('FreeRTOS+Linux', 1),
    ('Qualcomm FreeRTOS', 1),
]
for name, count in doc_data:
    print(f'  {name}: {count}')

print('\n' + '=' * 80)
print('结论')
print('=' * 80)

# 检查文档数据与哪个Excel匹配
print('\n文档中的数据来源分析:')
print(f'  文档中 FreeRTOS = 7')
print(f'  原始Excel中 FreeRTOS = {orig_counts.get("FreeRTOS", 0)}')
print(f'  去重Excel中 FreeRTOS = {dedup_counts.get("FreeRTOS", 0)}')

print(f'\n  文档中 freertos = 3')
print(f'  原始Excel中 freertos = {orig_counts.get("freertos", 0)}')
print(f'  去重Excel中 freertos = {dedup_counts.get("freertos", 0)}')

print(f'\n  文档中 Freertos = 2')
print(f'  原始Excel中 Freertos = {orig_counts.get("Freertos", 0)}')
print(f'  去重Excel中 Freertos = {dedup_counts.get("Freertos", 0)}')

# 判断数据来源
if orig_counts.get("FreeRTOS", 0) == 7 and orig_counts.get("freertos", 0) == 3:
    print('\n【结论】文档数据来自 原始Excel (V1.xlsx)')
elif dedup_counts.get("FreeRTOS", 0) == 7 and dedup_counts.get("freertos", 0) == 3:
    print('\n【结论】文档数据来自 去重Excel (V1_去重.xlsx)')
else:
    print('\n【结论】文档数据与两个Excel都不完全匹配')
