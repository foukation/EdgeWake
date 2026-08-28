package com.fxzs.lingxiagent.model.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.SocketFactory;

import okhttp3.CertificatePinner;
import okhttp3.CipherSuite;
import okhttp3.ConnectionSpec;
import okhttp3.TlsVersion;

public class SecurityPolicy {
    private static final NoProxySocketFactory noProxySocketFactory = new NoProxySocketFactory();
    // 证书固定：生产环境的证书指纹
    private static final Map<String, List<String>> PRODUCTION_PINS = new HashMap<String, List<String>>() {{
        put("ivs.chinamobiledevice.com", Arrays.asList(
                "sha256/i7WTqTvh0OioIruIfFR4kMPnBqrS2rdiVPl/s2uC/CY=",// 主证书指纹
                "sha256/SDG5orEv8iX6MNenIAxa8nQFNpROB/6+llsZdXHZNqs=",// 备证书指纹
                "sha256/SJPJjXFetaFBQHq8jPNnBnVA8rhU6LbnnfK9FIjQUPU=" // 备证书指纹
        ));
    }};

    // 证书加固，防止中间人攻击
    public static CertificatePinner getCertificatePinner() {
        CertificatePinner.Builder builder = new CertificatePinner.Builder();

        for (Map.Entry<String, List<String>> entry : PRODUCTION_PINS.entrySet()) {
            for (String pin : entry.getValue()) {
                builder.add(entry.getKey(), pin);
            }
        }

        return builder.build();
    }

    public static NoProxySocketFactory getNoProxySocketFactory() {
        return noProxySocketFactory;
    }

    public static List<ConnectionSpec> getConnectionSpec() {
        ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                .tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_2)
                .cipherSuites(CipherSuite.TLS_AES_128_GCM_SHA256, CipherSuite.TLS_AES_256_GCM_SHA384)
                .build();

        return Collections.singletonList(spec);
    }

    // 自定义 SocketFactory 防止代理
    private static class NoProxySocketFactory extends SocketFactory {
        private final SocketFactory delegate = SocketFactory.getDefault();

        @Override
        public Socket createSocket() throws IOException {
            Socket socket = delegate.createSocket();
            socket.setTcpNoDelay(true);
            socket.setTrafficClass(0x10); // 设置高优先级
            return socket;
        }

        @Override
        public Socket createSocket(String s, int i) {
            return null;
        }

        @Override
        public Socket createSocket(String s, int i, InetAddress address, int i1) {
            return null;
        }

        @Override
        public Socket createSocket(InetAddress address, int i) {
            return null;
        }

        @Override
        public Socket createSocket(InetAddress address, int i, InetAddress address1, int i1) {
            return null;
        }
    }
}
