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
print('Linux 深度分析')
print('=' * 80)

# 获取所有 Linux 的记录
linux_mask = df[os_col].astype(str).str.contains('Linux|LINUX', case=False, na=False)
linux_df = df[linux_mask]

print(f'\nLinux 总数: {len(linux_df)}')

print('\n【Linux 产品详细列表】')
print('-' * 100)
for idx, row in linux_df.iterrows():
    print(f"序号{row['序号']}: {row[brand_col]}")
    print(f"  产品: {row[product_col]}")
    print(f"  芯片: {row[chip_col]}")
    print(f"  OS: {row[os_col]}")
    print()

print('\n' + '=' * 80)
print('【按芯片厂商分类】')
print('=' * 80)

# 国科微 GK
gk_linux = linux_df[linux_df[chip_col].astype(str).str.contains('GK|国科微', case=False, na=False)]
print(f'\n【国科微 (GK)】: {len(gk_linux)}')
for idx, row in gk_linux.iterrows():
    print(f"  - {row[brand_col]} | {row[chip_col]}")

# 安凯 AK
ak_linux = linux_df[linux_df[chip_col].astype(str).str.contains('AK|安凯', case=False, na=False)]
print(f'\n【安凯 (AK)】: {len(ak_linux)}')
for idx, row in ak_linux.iterrows():
    print(f"  - {row[brand_col]} | {row[chip_col]}")

# 全志 Allwinner
allwinner_linux = linux_df[linux_df[chip_col].astype(str).str.contains('V851|V853|Allwinner|全志', case=False, na=False)]
print(f'\n【全志 (Allwinner)】: {len(allwinner_linux)}')
for idx, row in allwinner_linux.iterrows():
    print(f"  - {row[brand_col]} | {row[chip_col]}")

# 星宸 Sigmastar
sigmastar_linux = linux_df[linux_df[chip_col].astype(str).str.contains('Sigmastar|SV822|星宸', case=False, na=False)]
print(f'\n【星宸 (Sigmastar)】: {len(sigmastar_linux)}')
for idx, row in sigmastar_linux.iterrows():
    print(f"  - {row[brand_col]} | {row[chip_col]}")

# 其他
other_linux = linux_df[~linux_df[chip_col].astype(str).str.contains('GK|国科微|AK|安凯|V851|V853|Allwinner|全志|Sigmastar|SV822|星宸', case=False, na=False)]
print(f'\n【其他】: {len(other_linux)}')
for idx, row in other_linux.iterrows():
    print(f"  - {row[brand_col]} | {row[chip_col]}")
