```mermaid
graph TD
    A[应用层] --> B[操作系统抽象层]
    A --> C[百度DuerOS SDK]
    A --> D[设备驱动接口]
    
    B --> E[RTOS内核]
    
    C --> F[DCS模块]
    C --> G[DLP模块]
    C --> H[语音识别模块]
    C --> I[网络通信模块]
    
    D --> J[WiFi模块]
    D --> K[音频模块]
    D --> L[存储模块]
    D --> M[显示模块]
    
    subgraph 应用层
        A
    end
    
    subgraph 系统抽象层
        B
    end
    
    subgraph 百度DuerOS服务
        C
        F
        G
        H
        I
    end
    
    subgraph 硬件驱动
        D
        J
        K
        L
        M
    end
    
    subgraph RTOS系统
        E
    end
    
    style A fill:#FFE4B5,stroke:#333
    style B fill:#98FB98,stroke:#333
    style C fill:#87CEEB,stroke:#333
    style D fill:#DDA0DD,stroke:#333
    style E fill:#FFA07A,stroke:#333