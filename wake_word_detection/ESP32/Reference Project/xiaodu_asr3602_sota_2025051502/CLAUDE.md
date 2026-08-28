# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Baidu DuerOS SDK for an ASR (Automatic Speech Recognition) enabled device, specifically targeting the xiaodu_asr3602_sota platform. The codebase implements a complete voice assistant solution with DuerOS cloud connectivity.

## Codebase Structure

- `gui/lv_watch/lv_apps/xiaodu/` - Main application entry point and core logic
- `product/craneg_modem/baidu_hal/` - Hardware Abstraction Layer for Baidu OS APIs
- `product/craneg_modem/baidu_hal/baidu_hal_os/` - RTOS-specific implementations (threading, mutexes, timers, etc.)

## Key Components

1. **Main Application** (`duerapp.c`): 
   - Entry point in `xiaodu_main()` function
   - Handles DuerOS initialization, authentication, and event loop
   - Manages ASR (Automatic Speech Recognition) functionality
   - Implements DCS (DuerOS Capability Set) interfaces

2. **Hardware Abstraction Layer**:
   - OS adapter implementations in `baidu_hal_os.c`
   - Provides RTOS wrappers for threading, mutexes, semaphores, timers
   - Memory management and system utilities

3. **DuerOS Integration**:
   - Profile configuration and initialization
   - Voice input/output handling
   - DCS capability declarations
   - Connection agent management

## Build System

Uses a custom make-based build system:
- Build configuration in `baidu_hal.mak`
- Dependencies managed through makefiles
- Modular structure with groups and packages

## Key Configuration Points

1. **Client Authentication**: 
   - Client ID and Secret in `duerapp.c` (line 256-257)
   - Must be replaced with valid credentials

2. **Profile Parameters**:
   - Various DuerOS service endpoints and parameters configurable in `duer_init_profile()`

3. **Connection Settings**:
   - SOTA (State Of The Art) ASR proxy settings
   - MQTT event URI configuration