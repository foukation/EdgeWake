import pandas as pd
import json
import sys
sys.stdout.reconfigure(encoding='utf-8')

# 读取去重后的Excel文件
df = pd.read_excel('docs/适配产品信息汇总表V1_去重.xlsx', sheet_name=0)

# 基本信息
print('=== 基本信息 ===')
print(f'总行数: {len(df)}')
print(f'总列数: {len(df.columns)}')
print()

# 列名
print('=== 所有列名 ===')
for i, col in enumerate(df.columns):
    print(f'{i+1}. {col}')
print()

# 操作系统分布
print('=== 操作系统分布 ===')
os_col = None
for col in df.columns:
    if '操作系统' in str(col) or 'OS' in str(col).upper():
        os_col = col
        break

if os_col:
    print(f'操作系统列: {os_col}')
    os_counts = df[os_col].value_counts(dropna=False)
    print(os_counts.to_string())
print()

# 芯片架构分布
print('=== 芯片架构分布 ===')
arch_col = None
for col in df.columns:
    if '架构' in str(col) or 'arch' in str(col).lower():
        arch_col = col
        break

if arch_col:
    print(f'芯片架构列: {arch_col}')
    arch_counts = df[arch_col].value_counts(dropna=False)
    print(arch_counts.to_string())
print()

# 芯片型号分布
print('=== 芯片型号分布 (前30) ===')
chip_col = None
for col in df.columns:
    if '芯片型号' in str(col) or '主控' in str(col):
        chip_col = col
        break

if chip_col:
    print(f'芯片型号列: {chip_col}')
    chip_counts = df[chip_col].value_counts(dropna=False).head(30)
    print(chip_counts.to_string())
print()

# 接入方案分布
print('=== 接入方案分布 ===')
plan_col = None
for col in df.columns:
    if '接入方案' in str(col) or '适配方案' in str(col):
        plan_col = col
        break

if plan_col:
    print(f'接入方案列: {plan_col}')
    plan_counts = df[plan_col].value_counts(dropna=False)
    print(plan_counts.to_string())
print()

# 蓝牙支持分布
print('=== 蓝牙支持分布 ===')
bt_col = None
for col in df.columns:
    if '蓝牙' in str(col) or 'bluetooth' in str(col).lower():
        bt_col = col
        break

if bt_col:
    print(f'蓝牙列: {bt_col}')
    bt_counts = df[bt_col].value_counts(dropna=False)
    print(bt_counts.to_string())
print()

# WIFI支持分布
print('=== WIFI支持分布 ===')
wifi_col = None
for col in df.columns:
    if 'wifi' in str(col).lower() or 'WIFI' in str(col):
        wifi_col = col
        break

if wifi_col:
    print(f'WIFI列: {wifi_col}')
    wifi_counts = df[wifi_col].value_counts(dropna=False)
    print(wifi_counts.to_string())
print()

# ESP32相关产品
print('=== ESP32相关产品 ===')
if chip_col:
    esp32_df = df[df[chip_col].astype(str).str.contains('ESP32|esp32', case=False, na=False)]
    print(f'ESP32产品数量: {len(esp32_df)}')
    if len(esp32_df) > 0:
        # 找到品牌和产品名称列
        brand_col = None
        product_col = None
        for col in df.columns:
            if '品牌' in str(col):
                brand_col = col
            if '产品名称' in str(col) or '产品' in str(col):
                product_col = col
        
        if brand_col and product_col:
            print(esp32_df[[brand_col, product_col, chip_col]].to_string())
print()

# FreeRTOS相关产品
print('=== FreeRTOS相关产品 ===')
if os_col:
    freertos_df = df[df[os_col].astype(str).str.contains('FreeRTOS|freertos|Freertos', case=False, na=False)]
    print(f'FreeRTOS产品数量: {len(freertos_df)}')
print()

# 统计各类RTOS
print('=== RTOS详细分类 ===')
if os_col:
    rtos_keywords = ['RTOS', 'FreeRTOS', 'ThreadX', 'Zephyr', 'LiteOS', 'RTX', 'RT-Thread']
    for kw in rtos_keywords:
        count = df[os_col].astype(str).str.contains(kw, case=False, na=False).sum()
        print(f'{kw}: {count}')

print()

# 深度分析：按芯片厂商分类
print('=== 按芯片厂商分类 ===')
if chip_col:
    # ASR (翱捷)
    asr_count = df[chip_col].astype(str).str.contains('ASR', case=False, na=False).sum()
    print(f'ASR (翱捷): {asr_count}')
    
    # 展锐
    unisoc_count = df[chip_col].astype(str).str.contains('展锐|UWS|W217|T760|T606|UMS', case=False, na=False).sum()
    print(f'展锐 (Unisoc): {unisoc_count}')
    
    # 联发科
    mtk_count = df[chip_col].astype(str).str.contains('MT|MTK|联发科', case=False, na=False).sum()
    print(f'联发科 (MTK): {mtk_count}')
    
    # 泰凌微
    telink_count = df[chip_col].astype(str).str.contains('TLSR|Telink|泰凌', case=False, na=False).sum()
    print(f'泰凌微 (Telink): {telink_count}')
    
    # 乐鑫
    esp_count = df[chip_col].astype(str).str.contains('ESP32|esp32|乐鑫', case=False, na=False).sum()
    print(f'乐鑫 (ESP): {esp_count}')
    
    # 杰理
    jl_count = df[chip_col].astype(str).str.contains('JL|AC79|AB56|杰理', case=False, na=False).sum()
    print(f'杰理 (JL): {jl_count}')
    
    # 瑞芯微
    rk_count = df[chip_col].astype(str).str.contains('RK35|瑞芯', case=False, na=False).sum()
    print(f'瑞芯微 (RK): {rk_count}')
    
    # 恒玄
    bes_count = df[chip_col].astype(str).str.contains('BES', case=False, na=False).sum()
    print(f'恒玄 (BES): {bes_count}')
    
    # 国科微
    gk_count = df[chip_col].astype(str).str.contains('GK72', case=False, na=False).sum()
    print(f'国科微 (GK): {gk_count}')
    
    # 全志
    allwinner_count = df[chip_col].astype(str).str.contains('V821|V851|V853|全志|Allwinner', case=False, na=False).sum()
    print(f'全志 (Allwinner): {allwinner_count}')
    
    # 安凯
    ankai_count = df[chip_col].astype(str).str.contains('AK39', case=False, na=False).sum()
    print(f'安凯 (Ankai): {ankai_count}')
    
    # 炬芯
    actions_count = df[chip_col].astype(str).str.contains('ATS', case=False, na=False).sum()
    print(f'炬芯 (Actions): {actions_count}')
    
    # 高通
    qcom_count = df[chip_col].astype(str).str.contains('高通|Qualcomm|QCC', case=False, na=False).sum()
    print(f'高通 (Qualcomm): {qcom_count}')
    
    # 富芮坤
    fr_count = df[chip_col].astype(str).str.contains('FR80', case=False, na=False).sum()
    print(f'富芮坤 (FR): {fr_count}')
    
    # 思澈
    sifli_count = df[chip_col].astype(str).str.contains('思澈', case=False, na=False).sum()
    print(f'思澈: {sifli_count}')

print()

# 深度分析：芯片架构
print('=== 芯片架构深度分析 ===')
arch_col2 = None
for col in df.columns:
    if '芯片架构' in str(col):
        arch_col2 = col
        break

if arch_col2:
    print(f'芯片架构列: {arch_col2}')
    arch_counts2 = df[arch_col2].value_counts(dropna=False)
    print(arch_counts2.to_string())

print()

# 统计ARM、RISC-V、Xtensa
print('=== 架构类型统计 ===')
if arch_col2:
    arm_count = df[arch_col2].astype(str).str.contains('ARM|arm|Cortex|cortex', case=False, na=False).sum()
    print(f'ARM类: {arm_count}')
    
    riscv_count = df[arch_col2].astype(str).str.contains('RISC-V|riscv|risc-v', case=False, na=False).sum()
    print(f'RISC-V类: {riscv_count}')
    
    xtensa_count = df[arch_col2].astype(str).str.contains('Xtensa|xtensa|LX7', case=False, na=False).sum()
    print(f'Xtensa类: {xtensa_count}')
    
    x86_count = df[arch_col2].astype(str).str.contains('X86|x86', case=False, na=False).sum()
    print(f'X86类: {x86_count}')

print()

# 统计Android详细版本
print('=== Android详细分布 ===')
if os_col:
    android_df = df[df[os_col].astype(str).str.contains('Android|安卓|android|Andriod', case=False, na=False)]
    print(f'Android类产品总数: {len(android_df)}')
    android_os = android_df[os_col].value_counts()
    print(android_os.to_string())

print()

# Linux详细分布
print('=== Linux详细分布 ===')
if os_col:
    linux_df = df[df[os_col].astype(str).str.contains('Linux|linux|LINUX', case=False, na=False)]
    print(f'Linux类产品总数: {len(linux_df)}')

print()

# 统计"无"操作系统的产品
print('=== 无/裸机/其他操作系统 ===')
if os_col:
    none_os_keywords = ['无', '否', 'NaN', 'mcu', 'MCU', '裸机']
    none_count = 0
    for kw in none_os_keywords:
        if kw == 'NaN':
            none_count += df[os_col].isna().sum()
        else:
            none_count += (df[os_col].astype(str) == kw).sum()
    print(f'无/裸机/其他: {none_count}')

print()

# 统计Windows/macOS
print('=== Windows/macOS分布 ===')
if os_col:
    win_mac_df = df[df[os_col].astype(str).str.contains('windows|Windows|macos|Mac', case=False, na=False)]
    print(f'Windows/macOS产品总数: {len(win_mac_df)}')

print()
print('=' * 80)
print('深度分析：RTOS (通用) 产品详细列表')
print('=' * 80)

# 找出操作系统字段仅为"RTOS"的产品
if os_col:
    # 精确匹配 "RTOS" 或包含 "RTOS" 但不包含 FreeRTOS/ThreadX 等
    rtos_only_mask = (
        (df[os_col].astype(str).str.strip() == 'RTOS') |
        (df[os_col].astype(str).str.contains('RTOS，方案提供商', na=False)) |
        (df[os_col].astype(str).str.contains('RTOS（微克）', na=False)) |
        (df[os_col].astype(str).str.contains('RTOS系统', na=False)) |
        (df[os_col].astype(str).str.contains('RTOS 翱捷', na=False)) |
        (df[os_col].astype(str).str.contains('RTOS 广和通', na=False)) |
        (df[os_col].astype(str).str.contains('自研RTOS', na=False)) |
        (df[os_col].astype(str).str.contains('UNC_RTOS|UNC RTOS', na=False))
    )
    
    rtos_only_df = df[rtos_only_mask]
    print(f'\nRTOS (通用/厂商自带) 产品数量: {len(rtos_only_df)}')
    
    # 显示详细信息
    brand_col = None
    product_col = None
    for col in df.columns:
        if '品牌' in str(col):
            brand_col = col
        if '产品名称' in str(col):
            product_col = col
    
    if brand_col and product_col and chip_col:
        print('\n详细列表:')
        for idx, row in rtos_only_df.iterrows():
            print(f"序号{row['序号']}: {row[brand_col]} | {row[product_col]} | 芯片: {row[chip_col]} | OS: {row[os_col]}")

print()
print('=' * 80)
print('深度分析：FreeRTOS 产品详细列表')
print('=' * 80)

if os_col:
    freertos_mask = df[os_col].astype(str).str.contains('FreeRTOS|freertos|Freertos|FreeROTS', case=False, na=False)
    freertos_df = df[freertos_mask]
    print(f'\nFreeRTOS 产品数量: {len(freertos_df)}')
    
    if brand_col and product_col and chip_col:
        print('\n详细列表:')
        for idx, row in freertos_df.iterrows():
            print(f"序号{row['序号']}: {row[brand_col]} | {row[product_col]} | 芯片: {row[chip_col]} | OS: {row[os_col]}")

print()
print('=' * 80)
print('深度分析：ThreadX 产品详细列表')
print('=' * 80)

if os_col:
    threadx_mask = df[os_col].astype(str).str.contains('ThreadX|threadx|THREADX|Threadx', case=False, na=False)
    threadx_df = df[threadx_mask]
    print(f'\nThreadX 产品数量: {len(threadx_df)}')
    
    if brand_col and product_col and chip_col:
        print('\n详细列表:')
        for idx, row in threadx_df.iterrows():
            print(f"序号{row['序号']}: {row[brand_col]} | {row[product_col]} | 芯片: {row[chip_col]} | OS: {row[os_col]}")

print()
print('=' * 80)
print('深度分析：按芯片厂商的RTOS产品分布')
print('=' * 80)

# ASR芯片的操作系统分布
if chip_col and os_col:
    print('\n--- ASR (翱捷) 芯片产品的操作系统分布 ---')
    asr_df = df[df[chip_col].astype(str).str.contains('ASR', case=False, na=False)]
    print(f'ASR芯片产品总数: {len(asr_df)}')
    asr_os = asr_df[os_col].value_counts(dropna=False)
    print(asr_os.to_string())
    
    print('\n--- 展锐 (Unisoc) 芯片产品的操作系统分布 ---')
    unisoc_df = df[df[chip_col].astype(str).str.contains('展锐|UWS|W217|T760|T606|UMS', case=False, na=False)]
    print(f'展锐芯片产品总数: {len(unisoc_df)}')
    unisoc_os = unisoc_df[os_col].value_counts(dropna=False)
    print(unisoc_os.to_string())
    
    print('\n--- 杰理 (JL) 芯片产品的操作系统分布 ---')
    jl_df = df[df[chip_col].astype(str).str.contains('JL|AC79|AB56|杰理', case=False, na=False)]
    print(f'杰理芯片产品总数: {len(jl_df)}')
    jl_os = jl_df[os_col].value_counts(dropna=False)
    print(jl_os.to_string())
    
    print('\n--- 恒玄 (BES) 芯片产品的操作系统分布 ---')
    bes_df = df[df[chip_col].astype(str).str.contains('BES', case=False, na=False)]
    print(f'恒玄芯片产品总数: {len(bes_df)}')
    bes_os = bes_df[os_col].value_counts(dropna=False)
    print(bes_os.to_string())
    
    print('\n--- 乐鑫 (ESP) 芯片产品的操作系统分布 ---')
    esp_df = df[df[chip_col].astype(str).str.contains('ESP32|esp32', case=False, na=False)]
    print(f'乐鑫芯片产品总数: {len(esp_df)}')
    esp_os = esp_df[os_col].value_counts(dropna=False)
    print(esp_os.to_string())

print()
print('=' * 80)
print('深度分析：重复记录定义与分类')
print('=' * 80)

# 找到产品型号列
model_col = None
for col in df.columns:
    if '产品型号' in str(col):
        model_col = col
        break

print('\n### 重复记录定义标准 ###')
print('=' * 60)

# 定义1: 完全重复 - 品牌+产品名称+产品型号+芯片型号+操作系统 全部相同
print('\n【定义1】完全重复（所有关键字段相同）:')
print('  标准: 品牌 + 产品名称 + 产品型号 + 芯片型号 + 操作系统 全部相同')

key_cols = [brand_col, product_col, model_col, chip_col, os_col]
key_cols = [c for c in key_cols if c is not None]

# 创建复合键
df['composite_key_full'] = df[key_cols].astype(str).agg('|'.join, axis=1)
full_dup = df[df.duplicated(subset=key_cols, keep=False)]
full_dup_groups = full_dup.groupby('composite_key_full').size()
full_dup_count = (full_dup_groups > 1).sum()

print(f'  完全重复的记录组数: {full_dup_count}')
print(f'  涉及的记录总数: {len(full_dup)}')

if len(full_dup) > 0:
    print('\n  完全重复记录详情:')
    for key, group in full_dup.groupby('composite_key_full'):
        if len(group) > 1:
            print(f'\n  --- 重复组 (共{len(group)}条) ---')
            for idx, row in group.iterrows():
                print(f"    序号{row['序号']}: {row[brand_col]} | {row[product_col]} | 型号:{row[model_col]} | 芯片:{row[chip_col]} | OS:{row[os_col]}")

# 定义2: 品牌+产品名称+产品型号 相同（不考虑芯片和OS）
print('\n' + '=' * 60)
print('\n【定义2】产品型号重复（品牌+产品名称+型号相同，芯片/OS可能不同）:')
print('  标准: 品牌 + 产品名称 + 产品型号 相同')

key_cols_2 = [brand_col, product_col, model_col]
key_cols_2 = [c for c in key_cols_2 if c is not None]

df['composite_key_model'] = df[key_cols_2].astype(str).agg('|'.join, axis=1)
model_dup = df[df.duplicated(subset=key_cols_2, keep=False)]
model_dup_groups = model_dup.groupby('composite_key_model').size()
model_dup_count = (model_dup_groups > 1).sum()

print(f'  产品型号重复的记录组数: {model_dup_count}')
print(f'  涉及的记录总数: {len(model_dup)}')

if len(model_dup) > 0:
    print('\n  产品型号重复记录详情:')
    for key, group in model_dup.groupby('composite_key_model'):
        if len(group) > 1:
            # 检查芯片和OS是否有差异
            chips = group[chip_col].astype(str).unique()
            oses = group[os_col].astype(str).unique()
            has_diff = len(chips) > 1 or len(oses) > 1
            diff_mark = ' [芯片/OS有差异]' if has_diff else ' [完全相同]'
            
            print(f'\n  --- 重复组 (共{len(group)}条){diff_mark} ---')
            for idx, row in group.iterrows():
                print(f"    序号{row['序号']}: {row[brand_col]} | {row[product_col]} | 型号:{row[model_col]} | 芯片:{row[chip_col]} | OS:{row[os_col]}")

# 定义3: 品牌+产品名称 相同（不同型号视为不同产品）
print('\n' + '=' * 60)
print('\n【定义3】产品系列（品牌+产品名称相同，型号不同）:')
print('  标准: 品牌 + 产品名称 相同，但产品型号不同')
print('  说明: 这些是同一产品线的不同型号，不应视为重复')

key_cols_3 = [brand_col, product_col]
key_cols_3 = [c for c in key_cols_3 if c is not None]

df['composite_key_series'] = df[key_cols_3].astype(str).agg('|'.join, axis=1)
series_dup = df[df.duplicated(subset=key_cols_3, keep=False)]
series_dup_groups = series_dup.groupby('composite_key_series').size()
series_dup_count = (series_dup_groups > 1).sum()

print(f'  产品系列组数: {series_dup_count}')
print(f'  涉及的记录总数: {len(series_dup)}')

if len(series_dup) > 0:
    print('\n  产品系列详情（按型号分类）:')
    for key, group in series_dup.groupby('composite_key_series'):
        if len(group) > 1:
            models = group[model_col].astype(str).unique()
            is_series = len(models) > 1  # 有多个不同型号
            
            if is_series:
                print(f'\n  --- 产品系列 (共{len(group)}个型号) ---')
                for idx, row in group.iterrows():
                    print(f"    序号{row['序号']}: {row[brand_col]} | {row[product_col]} | 型号:{row[model_col]} | 芯片:{row[chip_col]}")

# 定义4: 仅芯片型号相同（不同品牌/产品使用相同芯片）
print('\n' + '=' * 60)
print('\n【定义4】芯片型号统计（使用相同芯片的不同产品）:')
print('  标准: 芯片型号相同')

chip_counts = df[chip_col].value_counts()
multi_chip = chip_counts[chip_counts > 1]
print(f'  被多个产品使用的芯片型号数: {len(multi_chip)}')
print('\n  使用次数>=3的芯片型号:')
for chip, count in multi_chip[multi_chip >= 3].items():
    print(f"    {chip}: {count}个产品")

# 汇总统计
print('\n' + '=' * 80)
print('### 重复记录汇总统计 ###')
print('=' * 80)

print(f'\n总记录数: {len(df)}')
print(f'\n按不同定义的重复情况:')
print(f'  【定义1】完全重复: {full_dup_count}组, {len(full_dup)}条记录')
print(f'  【定义2】型号重复: {model_dup_count}组, {len(model_dup)}条记录')
print(f'  【定义3】产品系列: {series_dup_count}组, {len(series_dup)}条记录')

# 计算去重后的记录数
unique_full = len(df) - len(full_dup) + full_dup_count
unique_model = len(df) - len(model_dup) + model_dup_count
unique_series = len(df) - len(series_dup) + series_dup_count

print(f'\n去重后的记录数:')
print(f'  按【定义1】去重: {unique_full}条唯一记录')
print(f'  按【定义2】去重: {unique_model}条唯一记录')
print(f'  按【定义3】去重: {unique_series}条唯一记录')

# 清理临时列
df.drop(columns=['composite_key_full', 'composite_key_model', 'composite_key_series'], inplace=True, errors='ignore')

print()
print('=' * 80)
print('深度分析：接入方案与操作系统的关系')
print('=' * 80)

plan_col = None
for col in df.columns:
    if '接入方案' in str(col):
        plan_col = col
        break

if plan_col and os_col:
    # 轻量适配-非Android 的操作系统分布
    light_non_android = df[df[plan_col].astype(str).str.contains('轻量适配-非Android', case=False, na=False)]
    print(f'\n"轻量适配-非Android" 产品数量: {len(light_non_android)}')
    print('操作系统分布:')
    print(light_non_android[os_col].value_counts(dropna=False).to_string())

print()
print('=' * 80)
print('深度分析：数据质量问题')
print('=' * 80)

# 检查空值
print('\n各列空值统计:')
null_counts = df.isnull().sum()
for col, count in null_counts.items():
    if count > 0 and 'Unnamed' not in str(col):
        print(f"  {col}: {count} 个空值")

# 检查操作系统字段的异常值
print('\n操作系统字段异常值:')
if os_col:
    os_values = df[os_col].astype(str).unique()
    abnormal = []
    for v in os_values:
        if '1900' in str(v) or v == '/' or v == '-' or v == '否':
            abnormal.append(v)
    if abnormal:
        print(f"  异常值: {abnormal}")

print()
print('=' * 80)
print('深度分析：产品类型推断')
print('=' * 80)

if product_col:
    # 按关键词分类产品
    categories = {
        '智能手表/手环': ['手表', '手环', 'Watch', 'watch'],
        '耳机/TWS': ['耳机', 'TWS', 'Earbuds', 'earbuds', 'Clip'],
        '智能音箱/机器人': ['音箱', '机器人', 'Robot', 'robot', 'Speaker'],
        '智能眼镜': ['眼镜', 'Glasses', 'glasses'],
        '智能台灯': ['台灯', 'Lamp', 'lamp'],
        '学生卡/电话手表': ['学生证', '学生卡', '儿童手表', '儿童电话'],
        'AI玩具': ['玩具', 'Toy', 'toy', '故事机', '小熊'],
        '学习机/教育平板': ['学习机', '学习平板', '教育'],
        '云电脑/云平板': ['云电脑', '云平板', 'Cloud'],
        '宠物设备': ['宠物', '喂食器'],
        '录音设备': ['录音', '录音卡'],
    }
    
    for cat_name, keywords in categories.items():
        count = 0
        for kw in keywords:
            count += df[product_col].astype(str).str.contains(kw, case=False, na=False).sum()
        # 去重（粗略）
        print(f"  {cat_name}: ~{count} 个产品")

print()
print('=' * 80)
print('深度分析：ESP32 产品完整信息')
print('=' * 80)

if chip_col:
    esp_df = df[df[chip_col].astype(str).str.contains('ESP32|esp32', case=False, na=False)]
    
    # 获取所有相关列
    mem_col = None
    storage_col = None
    bt_col = None
    wifi_col = None
    
    for col in df.columns:
        if '内存' in str(col) and 'RAM' in str(col):
            mem_col = col
        if '存储' in str(col) and 'ROM' in str(col):
            storage_col = col
        if '蓝牙' in str(col):
            bt_col = col
        if 'WIFI' in str(col):
            wifi_col = col
    
    print(f'\nESP32 产品完整信息 ({len(esp_df)} 个):')
    for idx, row in esp_df.iterrows():
        print(f"\n序号{row['序号']}:")
        print(f"  品牌: {row[brand_col]}")
        print(f"  产品: {row[product_col]}")
        print(f"  芯片: {row[chip_col]}")
        print(f"  操作系统: {row[os_col]}")
        if mem_col:
            print(f"  内存: {row[mem_col]}")
        if storage_col:
            print(f"  存储: {row[storage_col]}")
        if bt_col:
            print(f"  蓝牙: {row[bt_col]}")
        if wifi_col:
            print(f"  WIFI: {row[wifi_col]}")
