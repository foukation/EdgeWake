import pandas as pd
import sys
sys.stdout.reconfigure(encoding='utf-8')

# 读取去重后的Excel文件
df = pd.read_excel('docs/适配产品信息汇总表V1_去重.xlsx', sheet_name=0)

os_col = '操作系统'
chip_col = '芯片型号'
brand_col = '产品品牌'

print('=' * 80)
print('Android 深度分析')
print('=' * 80)

# 获取所有 Android 的记录
android_mask = df[os_col].astype(str).str.contains('Android|安卓|Andriod|android', case=False, na=False)
android_df = df[android_mask]

print(f'\nAndroid 总数: {len(android_df)}')

print('\n【Android 操作系统字段原始值分布】')
os_values = android_df[os_col].value_counts()
for val, count in os_values.items():
    print(f'  "{val}": {count}')

print('\n' + '=' * 80)
print('【分类分析】')
print('=' * 80)

# 按版本分类
print('\n【按版本分类】')

# 明确版本
android_14 = android_df[android_df[os_col].astype(str).str.contains('14', na=False)]
android_13 = android_df[android_df[os_col].astype(str).str.contains('13', na=False)]
android_15 = android_df[android_df[os_col].astype(str).str.contains('15', na=False)]
android_9 = android_df[android_df[os_col].astype(str).str.contains('9.0|9', na=False)]

# 未指定版本
android_generic = android_df[
    ~android_df[os_col].astype(str).str.contains('14|13|15|9.0|9', na=False)
]

print(f'  Android 14: {len(android_14)}')
print(f'  Android 13: {len(android_13)}')
print(f'  Android 15 (讯飞定制): {len(android_15)}')
print(f'  Android 9.0: {len(android_9)}')
print(f'  版本未指定: {len(android_generic)}')

print('\n【版本未指定的详细列表】')
for idx, row in android_generic.iterrows():
    print(f"  序号{row['序号']}: {row[brand_col]} | OS: {row[os_col]}")

print('\n' + '=' * 80)
print('【结论】')
print('=' * 80)

print(f'''
"Android (通用)" 的含义：

1. **"通用"** 指的是：
   - Excel 中操作系统字段只填写了 "Android"、"安卓"、"Android系统" 等
   - 没有明确指出是哪个版本（如 Android 14、Android 13 等）

2. **数量统计**：
   - 版本未指定: {len(android_generic)} 个
   - Android 14: {len(android_14)} 个
   - Android 13: {len(android_13)} 个
   - Android 15 (讯飞定制): {len(android_15)} 个
   - Android 9.0: {len(android_9)} 个

3. **建议的文档写法**：
   - "Android (版本未指定)" 比 "Android (通用)" 更准确
   - 或者 "Android (未标注版本)"
''')
