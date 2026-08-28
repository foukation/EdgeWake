import pandas as pd
import os
import sys

sys.stdout.reconfigure(encoding='utf-8')

os.chdir(r'E:\github\ESP32-RTOS-AI-SDK\docs')
files = [f for f in os.listdir('.') if f.endswith('.xlsx')]
df = pd.read_excel(files[0])

print('=' * 60)
print('设备操作系统分布分析')
print('=' * 60)

# 获取操作系统列
os_column = df['操作系统']

# 统计操作系统分布
os_counts = os_column.value_counts()
total = len(os_column)

print(f'\n总设备数量: {total}')
print('\n--- 操作系统分布 ---\n')

# 创建结果列表
results = []
for os_name, count in os_counts.items():
    percentage = (count / total) * 100
    results.append({
        '操作系统': os_name,
        '设备数量': count,
        '占比': f'{percentage:.1f}%'
    })
    print(f'{os_name}: {count} 台 ({percentage:.1f}%)')

# 保存到 CSV
result_df = pd.DataFrame(results)
result_df.to_csv(r'E:\github\ESP32-RTOS-AI-SDK\os_distribution.csv', encoding='utf-8-sig', index=False)
print('\n已保存到 os_distribution.csv')

# 按大类分组
print('\n--- 按系统大类分组 ---\n')

linux_count = 0
rtos_count = 0
other_count = 0

for os_name, count in os_counts.items():
    os_lower = str(os_name).lower()
    if 'linux' in os_lower or 'tina' in os_lower:
        linux_count += count
    elif 'rtos' in os_lower or 'freertos' in os_lower or 'zephyr' in os_lower or 'osal' in os_lower:
        rtos_count += count
    else:
        other_count += count

print(f'Linux 系统: {linux_count} 台 ({linux_count/total*100:.1f}%)')
print(f'RTOS 系统: {rtos_count} 台 ({rtos_count/total*100:.1f}%)')
print(f'其他/未知: {other_count} 台 ({other_count/total*100:.1f}%)')
