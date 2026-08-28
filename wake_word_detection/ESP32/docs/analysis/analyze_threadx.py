import pandas as pd
import os
import sys

sys.stdout.reconfigure(encoding='utf-8')

os.chdir(r'E:\github\ESP32-RTOS-AI-SDK\docs')
files = [f for f in os.listdir('.') if f.endswith('.xlsx')]
df = pd.read_excel(files[0])

print('=' * 80)
print('ThreadX 设备分析')
print('=' * 80)

# 筛选 ThreadX 设备
threadx_keywords = ['threadx', 'ThreadX', 'THREADX', 'Threadx']
threadx_pattern = '|'.join(threadx_keywords)
threadx_devices = df[df['操作系统'].str.contains(threadx_pattern, case=False, na=False)]

print(f'\nThreadX 设备数量: {len(threadx_devices)}')
print('\n--- ThreadX 设备详细信息 ---\n')

# 输出关键信息
columns = ['序号', '产品品牌', '产品名称（包含厂商、型号）', '操作系统', '芯片型号', '编译系统', '编译工具链']
for idx, row in threadx_devices.iterrows():
    print(f"设备 {row['序号']}:")
    print(f"  品牌: {row['产品品牌']}")
    print(f"  产品: {row['产品名称（包含厂商、型号）']}")
    print(f"  系统: {row['操作系统']}")
    print(f"  芯片: {row['芯片型号']}")
    print(f"  编译系统: {row['编译系统']}")
    print(f"  工具链: {row['编译工具链']}")
    print()

# 统计芯片分布
print('\n--- ThreadX 设备芯片分布 ---\n')
chip_counts = threadx_devices['芯片型号'].value_counts()
for chip, count in chip_counts.items():
    print(f'{chip}: {count} 台')

# 保存到 CSV
threadx_devices[columns].to_csv(
    r'E:\github\ESP32-RTOS-AI-SDK\threadx_devices.csv', 
    encoding='utf-8-sig', 
    index=False
)
print('\n已保存到 threadx_devices.csv')
