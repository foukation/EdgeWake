import pandas as pd
import sys
sys.stdout.reconfigure(encoding='utf-8')

# 读取去重后的Excel文件
df = pd.read_excel('docs/适配产品信息汇总表V1_去重.xlsx', sheet_name=0)

arch_col = '芯片架构'
chip_col = '芯片型号'

print('=' * 80)
print('RISC-V 架构深度分析')
print('=' * 80)

# 列出所有芯片架构的值
print('\n【所有芯片架构值分布】')
arch_counts = df[arch_col].value_counts(dropna=False)
print(arch_counts.to_string())

print('\n' + '=' * 80)
print('RISC-V 相关记录详细分析')
print('=' * 80)

# 精确匹配 RISC-V
riscv_df = df[df[arch_col].astype(str).str.contains('RISC-V|riscv|risc-v|RISC', case=False, na=False)]

print(f'\n包含 RISC-V/RISC 关键字的记录数: {len(riscv_df)}')

print('\n【RISC-V 相关记录详情】')
for idx, row in riscv_df.iterrows():
    print(f"\n序号{row['序号']}:")
    print(f"  品牌: {row['产品品牌']}")
    print(f"  产品: {row['产品名称（包含厂商、型号）']}")
    print(f"  芯片架构: {row[arch_col]}")
    print(f"  芯片型号: {row[chip_col]}")

# 分析 RISC-V 的不同写法
print('\n' + '=' * 80)
print('RISC-V 不同写法统计')
print('=' * 80)

riscv_patterns = {
    'RISC-V (精确)': 'RISC-V',
    'RISC-V 内核': 'RISC-V 内核',
    'RISC (不含-V)': '^RISC$|^RISC |RISC 32',
    'risc-v (小写)': 'risc-v',
    'Risc-V': 'Risc-V',
    'RISC-V架构': 'RISC-V架构',
    '32 位 RISC-V CPU': '32 位 RISC-V CPU',
    '32-bit RISC CPU': '32-bit RISC CPU',
}

for name, pattern in riscv_patterns.items():
    count = df[arch_col].astype(str).str.contains(pattern, case=False, na=False).sum()
    if count > 0:
        print(f'{name}: {count}')
        # 显示具体值
        matches = df[df[arch_col].astype(str).str.contains(pattern, case=False, na=False)][arch_col].unique()
        for m in matches:
            print(f'  -> "{m}"')

# 区分真正的 RISC-V 和普通 RISC
print('\n' + '=' * 80)
print('区分 RISC-V 和普通 RISC')
print('=' * 80)

# 真正的 RISC-V (包含 -V 或 V)
true_riscv = df[df[arch_col].astype(str).str.contains('RISC-V|riscv|Risc-V|RISC-V', case=False, na=False)]
print(f'\n真正的 RISC-V (包含 -V): {len(true_riscv)}')

# 普通 RISC (不包含 -V)
generic_risc = df[
    (df[arch_col].astype(str).str.contains('RISC', case=False, na=False)) &
    (~df[arch_col].astype(str).str.contains('RISC-V|riscv|Risc-V', case=False, na=False))
]
print(f'普通 RISC (不包含 -V): {len(generic_risc)}')

if len(generic_risc) > 0:
    print('\n【普通 RISC 记录详情】')
    for idx, row in generic_risc.iterrows():
        print(f"  序号{row['序号']}: 架构={row[arch_col]} | 芯片={row[chip_col]}")

# 检查 ARM、RISC-V 混合架构
print('\n' + '=' * 80)
print('混合架构分析')
print('=' * 80)

mixed_arch = df[df[arch_col].astype(str).str.contains('ARM.*RISC|RISC.*ARM', case=False, na=False)]
print(f'\nARM+RISC-V 混合架构: {len(mixed_arch)}')

if len(mixed_arch) > 0:
    print('\n【混合架构记录详情】')
    for idx, row in mixed_arch.iterrows():
        print(f"  序号{row['序号']}: 架构={row[arch_col]} | 芯片={row[chip_col]}")

# 最终统计
print('\n' + '=' * 80)
print('最终 RISC-V 统计')
print('=' * 80)

# 纯 RISC-V
pure_riscv = df[
    (df[arch_col].astype(str).str.contains('RISC-V|riscv|Risc-V', case=False, na=False)) &
    (~df[arch_col].astype(str).str.contains('ARM', case=False, na=False))
]
print(f'\n纯 RISC-V 架构: {len(pure_riscv)}')

# 包含 RISC-V 的混合架构
mixed_riscv = df[
    (df[arch_col].astype(str).str.contains('RISC-V|riscv|Risc-V', case=False, na=False)) &
    (df[arch_col].astype(str).str.contains('ARM', case=False, na=False))
]
print(f'RISC-V + ARM 混合架构: {len(mixed_riscv)}')

# 普通 RISC (可能是杰理等芯片的32-bit RISC)
print(f'普通 RISC (非 RISC-V): {len(generic_risc)}')

print(f'\n【建议统计方式】')
print(f'  方式1 (仅纯 RISC-V): {len(pure_riscv)}')
print(f'  方式2 (包含混合架构): {len(pure_riscv) + len(mixed_riscv)}')
print(f'  方式3 (包含所有 RISC): {len(riscv_df)}')
