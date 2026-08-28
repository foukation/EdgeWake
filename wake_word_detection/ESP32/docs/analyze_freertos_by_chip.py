import pandas as pd
import sys
sys.stdout.reconfigure(encoding='utf-8')

# 读取去重后的Excel文件
df = pd.read_excel('docs/适配产品信息汇总表V1_去重.xlsx', sheet_name=0)

os_col = '操作系统'
chip_col = '芯片型号'

print('=' * 80)
print('FreeRTOS 按芯片平台分布分析')
print('=' * 80)

# 获取所有 FreeRTOS 产品
freertos_mask = df[os_col].astype(str).str.contains('FreeRTOS|freertos|Freertos|FreeROTS', case=False, na=False)
freertos_df = df[freertos_mask]

print(f'\nFreeRTOS 产品总数: {len(freertos_df)}')

# 按芯片厂商分类
print('\n【按芯片厂商分布】')

# ESP32 (乐鑫)
esp32_mask = freertos_df[chip_col].astype(str).str.contains('ESP32|esp32', case=False, na=False)
esp32_count = esp32_mask.sum()
print(f'  ESP32 (乐鑫): {esp32_count}')
if esp32_count > 0:
    for idx, row in freertos_df[esp32_mask].iterrows():
        print(f"    - 序号{row['序号']}: {row['产品品牌']} | {row[chip_col]}")

# 杰理 (JL)
jl_mask = freertos_df[chip_col].astype(str).str.contains('JL|AC79|AC69|杰理', case=False, na=False)
jl_count = jl_mask.sum()
print(f'\n  杰理 (JL): {jl_count}')
if jl_count > 0:
    for idx, row in freertos_df[jl_mask].iterrows():
        print(f"    - 序号{row['序号']}: {row['产品品牌']} | {row[chip_col]}")

# 恒玄 (BES)
bes_mask = freertos_df[chip_col].astype(str).str.contains('BES', case=False, na=False)
bes_count = bes_mask.sum()
print(f'\n  恒玄 (BES): {bes_count}')
if bes_count > 0:
    for idx, row in freertos_df[bes_mask].iterrows():
        print(f"    - 序号{row['序号']}: {row['产品品牌']} | {row[chip_col]}")

# 全志 (Allwinner)
allwinner_mask = freertos_df[chip_col].astype(str).str.contains('V821|V851|V853|全志|Allwinner|XR872', case=False, na=False)
allwinner_count = allwinner_mask.sum()
print(f'\n  全志 (Allwinner): {allwinner_count}')
if allwinner_count > 0:
    for idx, row in freertos_df[allwinner_mask].iterrows():
        print(f"    - 序号{row['序号']}: {row['产品品牌']} | {row[chip_col]}")

# 泰凌微 (Telink)
telink_mask = freertos_df[chip_col].astype(str).str.contains('TLSR|Telink|泰凌', case=False, na=False)
telink_count = telink_mask.sum()
print(f'\n  泰凌微 (Telink): {telink_count}')
if telink_count > 0:
    for idx, row in freertos_df[telink_mask].iterrows():
        print(f"    - 序号{row['序号']}: {row['产品品牌']} | {row[chip_col]}")

# 高通 (Qualcomm)
qcom_mask = freertos_df[chip_col].astype(str).str.contains('高通|Qualcomm|QCC', case=False, na=False)
qcom_count = qcom_mask.sum()
print(f'\n  高通 (Qualcomm): {qcom_count}')
if qcom_count > 0:
    for idx, row in freertos_df[qcom_mask].iterrows():
        print(f"    - 序号{row['序号']}: {row['产品品牌']} | {row[chip_col]}")

# 其他
other_mask = ~(esp32_mask | jl_mask | bes_mask | allwinner_mask | telink_mask | qcom_mask)
other_count = other_mask.sum()
print(f'\n  其他: {other_count}')
if other_count > 0:
    for idx, row in freertos_df[other_mask].iterrows():
        print(f"    - 序号{row['序号']}: {row['产品品牌']} | {row[chip_col]} | OS: {row[os_col]}")

# 汇总
print('\n' + '=' * 80)
print('【汇总】')
print('=' * 80)

total = esp32_count + jl_count + bes_count + allwinner_count + telink_count + qcom_count + other_count
print(f'''
FreeRTOS 产品按芯片平台分布:
  - ESP32 (乐鑫): {esp32_count}
  - 杰理 (JL): {jl_count}
  - 恒玄 (BES): {bes_count}
  - 全志 (Allwinner): {allwinner_count}
  - 泰凌微 (Telink): {telink_count}
  - 高通 (Qualcomm): {qcom_count}
  - 其他: {other_count}
  - 总计: {total}

建议的文档写法:
| FreeRTOS | {len(freertos_df)} | 包括ESP32({esp32_count})、杰理({jl_count})、BES({bes_count})、全志({allwinner_count})、泰凌微({telink_count})、高通({qcom_count})等 |
''')
