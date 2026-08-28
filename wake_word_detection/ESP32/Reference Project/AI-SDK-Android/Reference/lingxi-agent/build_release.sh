#!/bin/bash

# 脚本说明：构建Release版本APK
# 使用方法：./build_release.sh

echo "=========================================="
echo "开始构建Release版本APK"
echo "=========================================="

# 允许自定义输出目录：
# 1) 第一个位置参数作为输出目录
# 2) 或使用环境变量 OUTPUT_DIR
# 3) 默认使用 ./release
if [ -n "$1" ]; then
  OUTPUT_DIR="$1"
elif [ -z "$OUTPUT_DIR" ]; then
  OUTPUT_DIR="release"
fi

# 清理之前的构建
echo "1. 清理旧的构建文件..."
./gradlew clean

# 构建Release APK
echo "2. 构建Release APK..."
./gradlew assembleRelease

# 检查构建是否成功
if [ $? -ne 0 ]; then
    echo "❌ 构建失败！请检查错误信息。"
    exit 1
fi

echo "✅ 构建成功！"

# 寻找 release APK（支持多维度变体）
echo ""
echo "3. 查找 Release APK..."
APK_DIR="app/build/outputs/apk"

# 兼容 macOS Bash 3.2：不使用 mapfile/数组，改用 while + -print0
APK_FILES_LIST=""
APK_FOUND=0
FIRST_APK=""
while IFS= read -r -d '' f; do
  if [ $APK_FOUND -eq 0 ]; then FIRST_APK="$f"; fi
  APK_FOUND=$((APK_FOUND+1))
  APK_FILES_LIST+="$f"$'\n'
done < <(find "$APK_DIR" -type f -path "*/release/*.apk" \
  ! -name "*-unsigned.apk" ! -name "*-unaligned.apk" -print0 | sort -z)

if [ $APK_FOUND -eq 0 ]; then
  echo "❌ 未找到 Release APK。请检查输出目录：$APK_DIR"
  find "$APK_DIR" -maxdepth 3 -type d -print 2>/dev/null | sed 's/^/  - /'
  exit 1
fi

# 读取版本号
VERSION_NAME=$(grep -m1 "versionName" app/build.gradle | awk -F'"' '{print $2}')
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# 准备输出目录
mkdir -p "$OUTPUT_DIR"

echo "找到 $APK_FOUND 个 APK："
while IFS= read -r line; do
  [ -z "$line" ] && continue
  echo "  - $line"
done <<< "$APK_FILES_LIST"

echo ""
echo "4. 复制 APK 到目标目录: $OUTPUT_DIR"

COPIED_LIST=""
while IFS= read -r src; do
  [ -z "$src" ] && continue
  base=$(basename "$src")
  dst="$OUTPUT_DIR/$base"
  cp -f "$src" "$dst" || { echo "❌ 复制失败: $src"; exit 1; }
  echo "✅ 已复制: $dst"
  COPIED_LIST+="$dst"$'\n'
done <<< "$APK_FILES_LIST"

echo ""
echo "5. 签名信息(示例展示首个):"
if [ -n "$FIRST_APK" ] && command -v keytool >/dev/null 2>&1; then
  keytool -printcert -jarfile "$FIRST_APK" 2>/dev/null | grep -E "(MD5|SHA1|SHA256):" | head -3 || true
else
  echo "(未安装 keytool 或未找到 APK，跳过签名信息展示)"
fi

echo ""
echo "6. 生成发布文件列表 README"
{
  echo "Release APK 文件列表："
  echo "========================"
  echo "时间: $(date)"
  echo "版本: ${VERSION_NAME}"
  echo
} > "$OUTPUT_DIR/README.txt"

while IFS= read -r item; do
  [ -z "$item" ] && continue
  size=$(du -h "$item" | awk '{print $1}')
  md5val=$(md5 -q "$item" 2>/dev/null || md5sum "$item" | awk '{print $1}')
  {
    echo "文件: $(basename "$item")"
    echo "  大小: $size"
    echo "  MD5 : $md5val"
  } >> "$OUTPUT_DIR/README.txt"
done <<< "$COPIED_LIST"

{
  echo
  echo "包名: com.fxzs.lingxiagent"
  echo "签名: Release签名 (yidiong.jks)"
} >> "$OUTPUT_DIR/README.txt"

echo ""
echo "=========================================="
echo "✨ Release APK 构建完成！"
echo "=========================================="
echo "输出目录: $OUTPUT_DIR"
while IFS= read -r item; do
  [ -z "$item" ] && continue
  echo "  - $item"
done <<< "$COPIED_LIST"
echo "  - $OUTPUT_DIR/README.txt"
echo ""
echo "你可以："
echo "  1. 上传上述 APK 到应用商店"
echo "  2. 分享给测试人员进行测试"
echo "  3. 使用 adb install <APK路径> 安装到设备"
echo ""
