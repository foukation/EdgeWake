import pandas as pd
import os
import sys
import re

sys.stdout.reconfigure(encoding='utf-8')

os.chdir(r'E:\github\ESP32-RTOS-AI-SDK\docs')
files = [f for f in os.listdir('.') if f.endswith('.xlsx')]
df = pd.read_excel(files[0])

print('=' * 60)
print('设备操作系统分布分析（按大类归类）')
print('=' * 60)

# 获取操作系统列
os_column = df['操作系统']
total = len(os_column)

# 定义系统分类函数
def classify_os(os_name):
    if pd.isna(os_name):
        return '未知/其他'
    
    os_str = str(os_name).lower().strip()
    
    # Linux 系统
    if 'linux' in os_str or 'tina' in os_str:
        if 'freertos' in os_str:
            return 'Linux + FreeRTOS 双系统'
        return 'Linux'
    
    # Android 系统
    if 'android' in os_str or '安卓' in os_str or 'andriod' in os_str:
        return 'Android'
    
    # FreeRTOS
    if 'freertos' in os_str or 'esp-idf' in os_str:
        return 'FreeRTOS'
    
    # ThreadX
    if 'threadx' in os_str:
        return 'ThreadX'
    
    # Zephyr
    if 'zephyr' in os_str:
        return 'Zephyr'
    
    # RT-Thread
    if 'rt-thread' in os_str:
        return 'RT-Thread'
    
    # LiteOS
    if 'liteos' in os_str or 'openharmony' in os_str:
        return 'LiteOS/OpenHarmony'
    
    # 其他 RTOS
    if 'rtos' in os_str or 'osal' in os_str or 'rtx' in os_str:
        return '其他 RTOS'
    
    # Windows/Mac
    if 'windows' in os_str or 'mac' in os_str:
        return 'Windows/Mac'
    
    # 无/未知
    if os_str in ['无', '否', '/', 'mcu', 'mcu：无', '模组内置固件', 'java']:
        return '无/裸机'
    
    return '其他'

# 分类统计
categories = {}
for os_name in os_column:
    category = classify_os(os_name)
    categories[category] = categories.get(category, 0) + 1

# 按数量排序
sorted_categories = sorted(categories.items(), key=lambda x: x[1], reverse=True)

print(f'\n总设备数量: {total}')
print('\n' + '=' * 60)
print('操作系统分布（按大类归类）')
print('=' * 60)
print(f'\n{"系统类型":<25} {"设备数量":<10} {"占比":<10}')
print('-' * 45)

results = []
for category, count in sorted_categories:
    percentage = (count / total) * 100
    print(f'{category:<25} {count:<10} {percentage:.1f}%')
    results.append({
        '系统类型': category,
        '设备数量': count,
        '占比': f'{percentage:.1f}%'
    })

# 保存到 CSV
result_df = pd.DataFrame(results)
result_df.to_csv(r'E:\github\ESP32-RTOS-AI-SDK\os_distribution_v2.csv', encoding='utf-8-sig', index=False)

# 计算 RTOS 总数
rtos_total = 0
linux_total = 0
android_total = 0
other_total = 0

for category, count in sorted_categories:
    if category in ['FreeRTOS', 'ThreadX', 'Zephyr', 'RT-Thread', 'LiteOS/OpenHarmony', '其他 RTOS']:
        rtos_total += count
    elif category == 'Linux':
        linux_total += count
    elif category == 'Linux + FreeRTOS 双系统':
        linux_total += count
    elif category == 'Android':
        android_total += count
    else:
        other_total += count

print('\n' + '=' * 60)
print('系统大类汇总')
print('=' * 60)
print(f'\n{"系统大类":<20} {"设备数量":<10} {"占比":<10}')
print('-' * 40)
print(f'{"RTOS 系统":<20} {rtos_total:<10} {rtos_total/total*100:.1f}%')
print(f'{"Linux 系统":<20} {linux_total:<10} {linux_total/total*100:.1f}%')
print(f'{"Android 系统":<20} {android_total:<10} {android_total/total*100:.1f}%')
print(f'{"其他/未知":<20} {other_total:<10} {other_total/total*100:.1f}%')

print('\n已保存到 os_distribution_v2.csv')
