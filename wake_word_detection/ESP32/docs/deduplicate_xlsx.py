import pandas as pd
import sys
sys.stdout.reconfigure(encoding='utf-8')

# 读取Excel文件
df = pd.read_excel('docs/适配产品信息汇总表V1.xlsx', sheet_name=0)

print('=== 去重前 ===')
print(f'总记录数: {len(df)}')

# 找到关键列
brand_col = '产品品牌'
product_col = '产品名称（包含厂商、型号）'
model_col = '产品型号'
chip_col = '芯片型号'
os_col = '操作系统'

# 定义1: 完全重复 - 品牌+产品名称+产品型号+芯片型号+操作系统 全部相同
key_cols = [brand_col, product_col, model_col, chip_col, os_col]

# 找出重复的记录
duplicates = df[df.duplicated(subset=key_cols, keep=False)]
print(f'\n完全重复的记录数: {len(duplicates)}')

# 显示将被删除的记录
print('\n将被删除的重复记录（保留每组第一条）:')
to_remove = df[df.duplicated(subset=key_cols, keep='first')]
for idx, row in to_remove.iterrows():
    print(f"  序号{row['序号']}: {row[brand_col]} | {row[product_col]} | {row[model_col]}")

# 去重（保留每组第一条）
df_dedup = df.drop_duplicates(subset=key_cols, keep='first')

print(f'\n=== 去重后 ===')
print(f'总记录数: {len(df_dedup)}')
print(f'删除了: {len(df) - len(df_dedup)} 条重复记录')

# 保存为新文件
output_file = 'docs/适配产品信息汇总表V1_去重.xlsx'
df_dedup.to_excel(output_file, index=False)
print(f'\n已保存到: {output_file}')

# 验证
print('\n=== 验证 ===')
df_verify = pd.read_excel(output_file)
print(f'新文件记录数: {len(df_verify)}')
