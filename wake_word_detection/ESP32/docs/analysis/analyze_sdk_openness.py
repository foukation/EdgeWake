import pandas as pd
import os
import sys

sys.stdout.reconfigure(encoding='utf-8')

os.chdir(r'E:\github\ESP32-RTOS-AI-SDK\docs')
files = [f for f in os.listdir('.') if f.endswith('.xlsx')]
df = pd.read_excel(files[0])

print('=' * 80)
print('设备系统与芯片分析')
print('=' * 80)

# 获取关键列
columns_of_interest = ['操作系统', '芯片型号', '编译系统', '编译工具链']

# 统计操作系统
os_counts = df['操作系统'].value_counts()
print('\n--- 操作系统分布 ---')
for os_name, count in os_counts.head(20).items():
    print(f'{os_name}: {count}')

# 统计芯片型号
print('\n--- 芯片型号分布 ---')
chip_counts = df['芯片型号'].value_counts()
for chip, count in chip_counts.head(30).items():
    print(f'{chip}: {count}')

# 统计编译系统
print('\n--- 编译系统分布 ---')
build_counts = df['编译系统'].value_counts()
for build, count in build_counts.head(20).items():
    print(f'{build}: {count}')

# 保存完整数据到 CSV
df[['序号', '产品品牌', '产品名称（包含厂商、型号）', '操作系统', '芯片型号', '编译系统', '编译工具链']].to_csv(
    r'E:\github\ESP32-RTOS-AI-SDK\device_analysis.csv', 
    encoding='utf-8-sig', 
    index=False
)
print('\n已保存到 device_analysis.csv')
