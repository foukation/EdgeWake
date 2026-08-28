import pandas as pd
import sys
sys.stdout.reconfigure(encoding='utf-8')

# 读取去重后的Excel文件
df = pd.read_excel('docs/适配产品信息汇总表V1_去重.xlsx', sheet_name=0)

os_col = '操作系统'

print('=' * 80)
print('RTOS 分类深度分析')
print('=' * 80)

# 列出所有操作系统值
print('\n【所有操作系统值分布】')
os_counts = df[os_col].value_counts(dropna=False)
print(os_counts.to_string())

print('\n' + '=' * 80)
print('RTOS 相关记录分类')
print('=' * 80)

# 获取所有包含 RTOS 相关关键字的记录
rtos_related = df[df[os_col].astype(str).str.contains('RTOS|FreeRTOS|freertos|Freertos|ThreadX|threadx|Zephyr|LiteOS|RTX|RT-Thread', case=False, na=False)]

print(f'\nRTOS相关记录总数: {len(rtos_related)}')

# 详细分类
print('\n' + '-' * 60)
print('【分类1】FreeRTOS 系列（应合并统计）')
print('-' * 60)

freertos_patterns = [
    ('FreeRTOS (精确)', df[os_col] == 'FreeRTOS'),
    ('freertos (小写)', df[os_col] == 'freertos'),
    ('Freertos', df[os_col] == 'Freertos'),
    ('FreeRTOS (ESP-IDF 5.4.0)', df[os_col] == 'FreeRTOS (ESP-IDF 5.4.0)'),
    ('FreeRTOS（ESP-IDF 内置，默认 RTOS）', df[os_col] == 'FreeRTOS（ESP-IDF 内置，默认 RTOS）'),
    ('ESP-IDF FreeRTOS', df[os_col] == 'ESP-IDF FreeRTOS'),
    ('FreeRTOS+Linux', df[os_col] == 'FreeRTOS+Linux'),
    ('Qualcomm FreeRTOS', df[os_col] == 'Qualcomm FreeRTOS'),
    ('FreeROTS (拼写错误)', df[os_col] == 'FreeROTS'),
    ('泰凌微实时内核 (裸机)（也可移植运行FreeRTOS）', df[os_col].astype(str).str.contains('也可移植运行FreeRTOS', na=False)),
]

freertos_total = 0
for name, mask in freertos_patterns:
    count = mask.sum()
    if count > 0:
        print(f'  {name}: {count}')
        freertos_total += count

print(f'\n  【FreeRTOS系列合计】: {freertos_total}')

print('\n' + '-' * 60)
print('【分类2】ThreadX 系列（应合并统计）')
print('-' * 60)

threadx_patterns = [
    ('ThreadX (精确)', df[os_col] == 'ThreadX'),
    ('ThreadX V5.1', df[os_col] == 'ThreadX V5.1'),
    ('threadX (小写X)', df[os_col] == 'threadX'),
    ('threadx (全小写)', df[os_col] == 'threadx'),
    ('Threadx', df[os_col] == 'Threadx'),
    ('THREADX (全大写)', df[os_col] == 'THREADX'),
]

threadx_total = 0
for name, mask in threadx_patterns:
    count = mask.sum()
    if count > 0:
        print(f'  {name}: {count}')
        threadx_total += count

print(f'\n  【ThreadX系列合计】: {threadx_total}')

print('\n' + '-' * 60)
print('【分类3】Zephyr 系列（应合并统计）')
print('-' * 60)

zephyr_patterns = [
    ('Zephyr (精确)', df[os_col] == 'Zephyr'),
    ('Zephyr RTOS', df[os_col] == 'Zephyr RTOS'),
]

zephyr_total = 0
for name, mask in zephyr_patterns:
    count = mask.sum()
    if count > 0:
        print(f'  {name}: {count}')
        zephyr_total += count

print(f'\n  【Zephyr系列合计】: {zephyr_total}')

print('\n' + '-' * 60)
print('【分类4】LiteOS 系列（应合并统计）')
print('-' * 60)

liteos_patterns = [
    ('LiteOS-M', df[os_col] == 'LiteOS-M'),
    ('RTOS 海思 LiteOS', df[os_col] == 'RTOS 海思 LiteOS'),
    ('系统Openharmony，内核liteos', df[os_col].astype(str).str.contains('liteos', case=False, na=False)),
]

liteos_total = 0
for name, mask in liteos_patterns:
    count = mask.sum()
    if count > 0:
        print(f'  {name}: {count}')
        liteos_total += count

print(f'\n  【LiteOS系列合计】: {liteos_total}')

print('\n' + '-' * 60)
print('【分类5】其他 RTOS')
print('-' * 60)

other_rtos = [
    ('RTX', df[os_col] == 'RTX'),
    ('RT-Thread', df[os_col] == 'RT-Thread'),
]

other_total = 0
for name, mask in other_rtos:
    count = mask.sum()
    if count > 0:
        print(f'  {name}: {count}')
        other_total += count

print(f'\n  【其他RTOS合计】: {other_total}')

print('\n' + '-' * 60)
print('【分类6】RTOS (通用/厂商自带)')
print('-' * 60)

# 包含 RTOS 但不包含 FreeRTOS/ThreadX/Zephyr/LiteOS/RTX/RT-Thread
rtos_generic_mask = (
    df[os_col].astype(str).str.contains('RTOS', case=False, na=False) &
    ~df[os_col].astype(str).str.contains('FreeRTOS|freertos|Freertos|FreeROTS', case=False, na=False) &
    ~df[os_col].astype(str).str.contains('ThreadX|threadx|THREADX|Threadx', case=False, na=False) &
    ~df[os_col].astype(str).str.contains('Zephyr', case=False, na=False) &
    ~df[os_col].astype(str).str.contains('LiteOS|liteos', case=False, na=False) &
    ~df[os_col].astype(str).str.contains('RTX', case=True, na=False) &
    ~df[os_col].astype(str).str.contains('RT-Thread', case=False, na=False)
)

rtos_generic_df = df[rtos_generic_mask]
print(f'\n  RTOS (通用/厂商自带) 数量: {len(rtos_generic_df)}')

# 列出所有通用RTOS的具体值
print('\n  具体值分布:')
generic_values = rtos_generic_df[os_col].value_counts()
for val, count in generic_values.items():
    print(f'    "{val}": {count}')

print('\n' + '=' * 80)
print('【建议的分类方式】')
print('=' * 80)

print(f'''
| RTOS类型 | 数量 | 说明 |
|----------|------|------|
| RTOS (通用/厂商自带) | {len(rtos_generic_df)} | 芯片厂商封闭RTOS |
| FreeRTOS (所有变体) | {freertos_total} | 包括ESP-IDF、Qualcomm等 |
| ThreadX (所有变体) | {threadx_total} | 包括各种大小写变体 |
| Zephyr (所有变体) | {zephyr_total} | |
| LiteOS (所有变体) | {liteos_total} | 包括海思LiteOS、OpenHarmony |
| RTX | 1 | ARM Keil RTX |
| RT-Thread | 1 | 国产开源RTOS |
| **RTOS类总计** | **{len(rtos_generic_df) + freertos_total + threadx_total + zephyr_total + liteos_total + other_total}** | |
''')

print('\n' + '=' * 80)
print('【问题分析】')
print('=' * 80)

print('''
文档中的分类问题:

1. **FreeRTOS 被拆分成多个条目**:
   - "FreeRTOS" (7)
   - "freertos" (3) 
   - "Freertos" (2)
   - "FreeRTOS (ESP-IDF)" (2)
   - "ESP-IDF FreeRTOS" (1)
   - "FreeRTOS+Linux" (1)
   - "Qualcomm FreeRTOS" (1)
   
   这些应该合并为一个 "FreeRTOS (所有变体)" 条目

2. **ThreadX 被拆分成多个条目**:
   - "ThreadX" (5)
   - "ThreadX V5.1" (2)
   - "threadX/Threadx" (3)
   
   这些应该合并为一个 "ThreadX (所有变体)" 条目

3. **建议的简化分类**:
   - RTOS (通用/厂商自带): 包含所有只写"RTOS"的记录
   - FreeRTOS: 合并所有FreeRTOS变体
   - ThreadX: 合并所有ThreadX变体
   - Zephyr: 合并所有Zephyr变体
   - LiteOS: 合并所有LiteOS变体
   - 其他: RTX, RT-Thread
''')
