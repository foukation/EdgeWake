import pandas as pd
import sys
sys.stdout.reconfigure(encoding='utf-8')

# 读取去重后的Excel文件
df = pd.read_excel('docs/适配产品信息汇总表V1_去重.xlsx', sheet_name=0)

os_col = '操作系统'
chip_col = '芯片型号'
brand_col = '产品品牌'

print('=' * 80)
print('文档 2.2 节 RTOS 分类深度分析')
print('=' * 80)

# ============================================================
# FreeRTOS 分析
# ============================================================
print('\n' + '=' * 60)
print('【FreeRTOS 分析】')
print('=' * 60)

freertos_mask = df[os_col].astype(str).str.contains('FreeRTOS|freertos|Freertos|FreeROTS', case=False, na=False)
freertos_df = df[freertos_mask]
print(f'FreeRTOS 总数: {len(freertos_df)}')

# 按芯片厂商分类
esp32_mask = freertos_df[chip_col].astype(str).str.contains('ESP32|esp32', case=False, na=False)
jl_mask = freertos_df[chip_col].astype(str).str.contains('JL|AC79|AC69|杰理|AC6951', case=False, na=False)
bes_mask = freertos_df[chip_col].astype(str).str.contains('BES', case=False, na=False)
allwinner_mask = freertos_df[chip_col].astype(str).str.contains('V821|V851|V853|全志|Allwinner|XR872', case=False, na=False)
telink_mask = freertos_df[chip_col].astype(str).str.contains('TLSR|Telink|泰凌', case=False, na=False)
qcom_mask = freertos_df[chip_col].astype(str).str.contains('高通|Qualcomm|QCC', case=False, na=False)
other_mask = ~(esp32_mask | jl_mask | bes_mask | allwinner_mask | telink_mask | qcom_mask)

print(f'''
按芯片平台分布:
  - ESP32 (乐鑫): {esp32_mask.sum()}
  - 杰理 (JL): {jl_mask.sum()}
  - 全志 (Allwinner): {allwinner_mask.sum()}
  - 恒玄 (BES): {bes_mask.sum()}
  - 泰凌微 (Telink): {telink_mask.sum()}
  - 高通 (Qualcomm): {qcom_mask.sum()}
  - 其他: {other_mask.sum()}
''')

# ============================================================
# ThreadX 分析
# ============================================================
print('\n' + '=' * 60)
print('【ThreadX 分析】')
print('=' * 60)

threadx_mask = df[os_col].astype(str).str.contains('ThreadX|threadx|THREADX|Threadx', case=False, na=False)
threadx_df = df[threadx_mask]
print(f'ThreadX 总数: {len(threadx_df)}')

# 按芯片厂商分类
asr_threadx = threadx_df[threadx_df[chip_col].astype(str).str.contains('ASR', case=False, na=False)]
unisoc_threadx = threadx_df[threadx_df[chip_col].astype(str).str.contains('展锐|Unisoc|UWS|W217', case=False, na=False)]
other_threadx = threadx_df[~threadx_df[chip_col].astype(str).str.contains('ASR|展锐|Unisoc|UWS|W217', case=False, na=False)]

print(f'''
按芯片平台分布:
  - ASR (翱捷): {len(asr_threadx)}
  - 展锐 (Unisoc): {len(unisoc_threadx)}
  - 其他: {len(other_threadx)}
''')

print('详细列表:')
for idx, row in threadx_df.iterrows():
    print(f"  - 序号{row['序号']}: {row[brand_col]} | {row[chip_col]} | {row[os_col]}")

# ============================================================
# Zephyr 分析
# ============================================================
print('\n' + '=' * 60)
print('【Zephyr 分析】')
print('=' * 60)

zephyr_mask = df[os_col].astype(str).str.contains('Zephyr', case=False, na=False)
zephyr_df = df[zephyr_mask]
print(f'Zephyr 总数: {len(zephyr_df)}')

print('详细列表:')
for idx, row in zephyr_df.iterrows():
    print(f"  - 序号{row['序号']}: {row[brand_col]} | {row[chip_col]} | {row[os_col]}")

# ============================================================
# LiteOS 分析
# ============================================================
print('\n' + '=' * 60)
print('【LiteOS 分析】')
print('=' * 60)

liteos_mask = df[os_col].astype(str).str.contains('LiteOS|liteos|OpenHarmony|Openharmony', case=False, na=False)
liteos_df = df[liteos_mask]
print(f'LiteOS 总数: {len(liteos_df)}')

print('详细列表:')
for idx, row in liteos_df.iterrows():
    print(f"  - 序号{row['序号']}: {row[brand_col]} | {row[chip_col]} | {row[os_col]}")

# ============================================================
# 其他 RTOS 分析
# ============================================================
print('\n' + '=' * 60)
print('【其他 RTOS】')
print('=' * 60)

rtx_mask = df[os_col] == 'RTX'
rtthread_mask = df[os_col] == 'RT-Thread'

print(f'RTX: {rtx_mask.sum()}')
print(f'RT-Thread: {rtthread_mask.sum()}')

# ============================================================
# RTOS (通用) 分析
# ============================================================
print('\n' + '=' * 60)
print('【RTOS (通用/厂商自带)】')
print('=' * 60)

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
print(f'RTOS (通用) 总数: {len(rtos_generic_df)}')

# 按芯片厂商分类
asr_generic = rtos_generic_df[rtos_generic_df[chip_col].astype(str).str.contains('ASR', case=False, na=False)]
unisoc_generic = rtos_generic_df[rtos_generic_df[chip_col].astype(str).str.contains('展锐|Unisoc|UWS|W217', case=False, na=False)]
jl_generic = rtos_generic_df[rtos_generic_df[chip_col].astype(str).str.contains('JL|AC79|AC69|杰理|AB5656', case=False, na=False)]
bes_generic = rtos_generic_df[rtos_generic_df[chip_col].astype(str).str.contains('BES', case=False, na=False)]
other_generic = rtos_generic_df[~rtos_generic_df[chip_col].astype(str).str.contains('ASR|展锐|Unisoc|UWS|W217|JL|AC79|AC69|杰理|AB5656|BES', case=False, na=False)]

print(f'''
按芯片平台分布:
  - ASR (翱捷): {len(asr_generic)}
  - 展锐 (Unisoc): {len(unisoc_generic)}
  - 杰理 (JL): {len(jl_generic)}
  - 恒玄 (BES): {len(bes_generic)}
  - 其他: {len(other_generic)}
''')

# ============================================================
# 汇总
# ============================================================
print('\n' + '=' * 80)
print('【建议的 2.2 节分类方式】')
print('=' * 80)

total_rtos = len(freertos_df) + len(threadx_df) + len(zephyr_df) + len(liteos_df) + rtx_mask.sum() + rtthread_mask.sum() + len(rtos_generic_df)

print(f'''
| RTOS类型 | 数量 | 备注 |
|----------|------|------|
| **RTOS (通用/厂商自带)** | **{len(rtos_generic_df)}** | ASR({len(asr_generic)}), 展锐({len(unisoc_generic)}), 杰理({len(jl_generic)}), BES({len(bes_generic)}), 其他({len(other_generic)}) |
| **FreeRTOS** | **{len(freertos_df)}** | ESP32({esp32_mask.sum()}), 杰理({jl_mask.sum()}), 全志({allwinner_mask.sum()}), BES({bes_mask.sum()}), 泰凌微({telink_mask.sum()}), 高通({qcom_mask.sum()}), 其他({other_mask.sum()}) |
| **ThreadX** | **{len(threadx_df)}** | ASR({len(asr_threadx)}), 展锐({len(unisoc_threadx)}), 其他({len(other_threadx)}) |
| **Zephyr** | **{len(zephyr_df)}** | 炬芯ATS3085 |
| **LiteOS** | **{len(liteos_df)}** | 华为LiteOS/OpenHarmony |
| **RTX** | **{rtx_mask.sum()}** | ARM Keil RTX |
| **RT-Thread** | **{rtthread_mask.sum()}** | 国产开源RTOS |
| **RTOS类总计** | **{total_rtos}** | |
''')

print('\n' + '=' * 80)
print('【问题分析】')
print('=' * 80)

print('''
当前文档 2.2 节的问题:

1. **FreeRTOS 被按大小写拆分**
   - 文档列出: FreeRTOS(7), freertos(3), Freertos(2), FreeRTOS(ESP-IDF)(2), ESP-IDF FreeRTOS(1)...
   - 应该合并为: FreeRTOS(19)，并按芯片平台区分

2. **ThreadX 被按大小写拆分**
   - 文档列出: ThreadX(5), ThreadX V5.1(2), threadX/Threadx(3)
   - 应该合并为: ThreadX(10)，并按芯片平台区分

3. **Zephyr 被拆分**
   - 文档列出: Zephyr(2), Zephyr RTOS(1)
   - 应该合并为: Zephyr(3)

4. **LiteOS 被拆分**
   - 文档列出: LiteOS-M(1), RTOS 海思 LiteOS(1)
   - 遗漏了: 系统Openharmony，内核liteos
   - 应该合并为: LiteOS(5)，包括OpenHarmony

5. **分类标准不统一**
   - 有的按原始写法拆分（FreeRTOS/freertos/Freertos）
   - 有的合并了（threadX/Threadx = 3）
   - 标准应该统一：按RTOS类型合并，按芯片平台区分
''')
