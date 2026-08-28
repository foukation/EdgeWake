#!/usr/bin/env python3
"""
URL 加密工具
用于生成 C++ 代码中的加密字节数组
使用 XOR 加密（密钥 0x5A）
"""

def xor_encrypt(text, key=0x5A):
    """使用 XOR 加密字符串"""
    return [ord(c) ^ key for c in text]

def generate_cpp_array(url, var_name):
    """生成 C++ constexpr 数组代码"""
    encrypted = xor_encrypt(url)
    hex_values = ', '.join([f"0x{b:02x}" for b in encrypted])
    
    cpp_code = f"""
// 原始: "{url}"
constexpr uint8_t {var_name}[] = {{
    {hex_values}
}};
constexpr size_t {var_name}_LEN = sizeof({var_name});
"""
    return cpp_code

def main():
    """主函数"""
    print("=" * 80)
    print("URL 加密工具 - XOR 加密（密钥 0x5A）")
    print("=" * 80)
    print()
    
    # 定义需要加密的 URL
    urls = {
        "ENCRYPTED_TERMINAL_URL": "https://ivs.chinamobiledevice.com:11443",
        "ENCRYPTED_AIPAAS_URL": "https://aqua-digital.aipaas.com",
        "ENCRYPTED_WSS_URL": "wss://ivs.chinamobiledevice.com:11443",
        "ENCRYPTED_TERMINAL_URL_TEST": "https://ivs.chinamobiledevice.com:11443/ai-admin-beta",
        "ENCRYPTED_WSS_URL_TEST": "wss://ivs.chinamobiledevice.com:11443/ai-admin-beta/app-ws/v2/asr"
    }
    
    print("// 复制以下代码到 native_config.cpp 中")
    print("// ============================================")
    print()
    
    for var_name, url in urls.items():
        print(generate_cpp_array(url, var_name))
    
    print()
    print("=" * 80)
    print("加密完成！")
    print("=" * 80)

if __name__ == "__main__":
    main()

