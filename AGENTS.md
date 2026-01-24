# MSST-WebUI 项目上下文文档

## 项目概述

MSST-WebUI 是一个为音乐源分离训练（Music-Source-Separation-Training, MSST）提供 Web 界面的应用程序。该项目集成了多种音频分离模型，包括 MSST 模型和 VR (Vocal Remover) 模型，并提供了预设处理流程、模型安装、以及多种实用工具（如 SOME MIDI 提取器、高级集成模式等）。

### 主要技术栈

- **编程语言**: Python 3.10+
- **深度学习框架**: PyTorch (torch, torchvision, torchaudio >= 2.7.1)
- **Web 框架**: Gradio 4.38.1
- **API 框架**: FastAPI 0.111.0 (用于 API 服务)
- **音频处理**: librosa, soundfile, pedalboard, audiomentations
- **模型架构**: 支持多种模型类型，包括 bs_roformer, mel_band_roformer, segm_models, htdemucs, mdx23c, swin_upernet, bandit, bandit_v2, scnet, scnet_unofficial, torchseg, apollo, bs_mamba2
- **训练框架**: PyTorch Lightning
- **依赖管理**: uv (现代 Python 包管理器)

### 项目结构

```
MSST-WebUI-main/
├── webUI.py                    # WebUI 主入口文件
├── api_service.py              # FastAPI 服务（用于 US3 云存储集成）
├── webui/                      # WebUI 核心模块
│   ├── app.py                  # Gradio 应用主文件
│   ├── msst.py                 # MSST 模型推理界面
│   ├── vr.py                   # VR 模型推理界面
│   ├── preset.py               # 预设处理界面
│   ├── ensemble.py             # 集成模式界面
│   ├── models.py               # 模型管理界面
│   ├── train.py                # 训练界面
│   ├── settings.py             # 设置界面
│   ├── tools.py                # 工具界面
│   └── ui/                     # UI 组件
├── inference/                  # 推理引擎
│   ├── msst_infer.py           # MSST 模型推理类
│   ├── vr_infer.py             # VR 模型推理类
│   ├── preset_infer.py         # 预设推理类
│   └── comfy_infer.py          # ComfyUI 集成推理
├── train/                      # 训练模块
│   ├── train.py                # 训练脚本
│   ├── train_accelerate.py     # 多 GPU 训练脚本
│   └── valid.py                # 验证脚本
├── scripts/                    # 命令行工具
│   ├── msst_cli.py             # MSST 模型 CLI
│   ├── vr_cli.py               # VR 模型 CLI
│   ├── preset_infer_cli.py     # 预设推理 CLI
│   ├── ensemble_cli.py         # 集成推理 CLI
│   ├── ensemble_audio_cli.py   # 音频集成 CLI
│   └── some_cli.py             # SOME MIDI 提取 CLI
├── modules/                    # 模型实现模块
│   ├── bs_roformer/            # BS-Roformer 模型
│   ├── scnet/                  # SCNet 模型
│   ├── bandit/                 # Bandit 模型
│   ├── bandit_v2/              # Bandit v2 模型
│   ├── mdx23c_tfc_tdf_v3.py    # MDX23C 模型
│   ├── demucs4ht.py            # Demucs 4 HT 模型
│   └── ...
├── utils/                      # 工具模块
│   ├── constant.py             # 常量定义
│   ├── dataset.py              # 数据集处理
│   ├── ensemble.py             # 集成算法
│   ├── logger.py               # 日志工具
│   └── utils.py                # 通用工具
├── configs/                    # 配置文件（运行时从 configs_backup 复制）
├── configs_backup/             # 配置文件备份
│   ├── vocal_models/           # 人声模型配置
│   ├── multi_stem_models/      # 多音轨模型配置
│   ├── single_stem_models/     # 单音轨模型配置
│   └── vr_modelparams/         # VR 模型参数
├── pretrain/                   # 预训练模型存储目录
│   ├── vocal_models/
│   ├── multi_stem_models/
│   ├── single_stem_models/
│   └── VR_Models/
├── data/                       # 运行时数据（从 data_backup 复制）
├── data_backup/                # 数据备份
│   ├── webui_config.json       # WebUI 配置
│   ├── language.json           # 多语言配置
│   └── models_info.json        # 模型信息
├── presets/                    # 用户预设
├── tools/                      # 工具和资源
│   ├── SOME/                   # SOME MIDI 提取器
│   ├── SOME_weights/           # SOME 权重
│   ├── themes/                 # 主题文件
│   └── webUI_for_clouds/       # 云平台 WebUI
├── ComfyUI/                    # ComfyUI 集成
├── docs/                       # 文档
└── requirements.txt            # Python 依赖
```

## 构建和运行

### 环境要求

- Python 3.10 或更高版本
- CUDA 12.1+ (如使用 GPU)
- FFmpeg (用于音频处理)

### 安装步骤

1. **克隆仓库**
```bash
git clone https://github.com/SUC-DriverOld/MSST-WebUI.git
cd MSST-WebUI
```

2. **创建 Python 环境并安装依赖**
```bash
# 创建 conda 环境
conda create -n msst python=3.10 -y
conda activate msst

# 安装 PyTorch（根据你的 CUDA 版本调整）
pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu121

# 安装项目依赖
pip install -r requirements.txt --only-binary=samplerate
pip install neuraloperator>=2.0.0 safetensors>=0.7.0

# 修复 librosa 问题
pip uninstall librosa -y
pip install tools/webUI_for_clouds/librosa-0.9.2-py3-none-any.whl
```

3. **使用 uv 安装（推荐）**
```bash
# 安装 uv（如果尚未安装）
pip install uv

# 使用 uv 安装依赖
uv sync
```

### 运行 WebUI

**本地模式**:
```bash
python webUI.py
```

**命令行参数**:
```bash
python webUI.py [选项]

选项:
  -d, --debug                          启用调试模式
  -i IP_ADDRESS, --ip_address IP_ADDRESS  服务器 IP 地址（默认：自动）
  -p PORT, --port PORT                 服务器端口（默认：自动）
  -s, --share                          启用分享链接
  --use_cloud                          使用云平台专用 WebUI
  --language {Auto,zh_CN,zh_TW,en_US,ja_JP,ko_KR}  设置语言
  --model_download_link {Auto,huggingface.co,hf-mirror.com}  设置模型下载源
  --factory_reset                      重置设置并清除缓存
```

**云平台模式**:
```bash
python webUI.py --use_cloud --share --language zh_CN --ip_address 0.0.0.0 --port 7860
```

### 运行 API 服务

项目包含一个 FastAPI 服务，用于与 US3 云存储集成：
```bash
python api_service.py
```

API 服务默认运行在 `http://0.0.0.0:8000`，提供以下端点：
- `POST /process_us3` - 处理 US3 音频分离任务
- `GET /status/{task_id}` - 查询任务状态

### 命令行工具

项目提供了多个 CLI 工具用于批量处理：

**MSST 模型推理**:
```bash
python scripts/msst_cli.py -i input -o results \
  --model_type bs_roformer \
  --model_path pretrain/vocal_models/model_bs_roformer_ep_368_sdr_12.9628.ckpt \
  --config_path configs/vocal_models/model_bs_roformer_ep_368_sdr_12.9628.yaml \
  --output_format wav
```

**VR 模型推理**:
```bash
python scripts/vr_cli.py -i input -o results \
  -m pretrain/VR_Models/1_HP-UVR.pth \
  --batch_size 4 --window_size 512 \
  --aggression 5
```

**预设推理**:
```bash
python scripts/preset_infer_cli.py -p presets/my_preset.json \
  -i input_folder -o output_folder -f wav
```

**集成推理**:
```bash
python scripts/ensemble_cli.py -p ensemble.json \
  -i input -o output -f wav --extract_inst
```

**SOME MIDI 提取**:
```bash
python scripts/some_cli.py -i input_audio.wav -o output.mid
```

## 开发约定

### 代码风格

- **格式化工具**: 使用 Ruff 进行代码格式化和检查
- **行长度**: 200 字符
- **缩进**: 4 空格
- **引号**: 双引号
- **导入顺序**: 使用 isort 规范（已配置在 Ruff 中）

Ruff 配置（pyproject.toml）:
```toml
[tool.ruff]
target-version = "py310"
line-length = 200

[tool.ruff.format]
indent-style = "space"
line-ending = "auto"
quote-style = "double"
skip-magic-trailing-comma = true

[tool.ruff.lint]
select = ["E", "F", "B", "I", "N"]
ignore = ["E501"]
```

### 项目约定

1. **配置文件管理**:
   - `configs/` 和 `data/` 目录在首次运行时从 `configs_backup/` 和 `data_backup/` 复制
   - 用户自定义配置存储在 `configs/` 和 `data/` 中
   - 不要直接修改 `*_backup/` 目录中的文件

2. **模型管理**:
   - 预训练模型存储在 `pretrain/` 目录下，按类型分类
   - 模型配置文件（.yaml）存储在 `configs/` 目录下
   - 模型信息索引在 `data/models_info.json` 中维护

3. **多语言支持**:
   - 语言文件位于 `data/language.json`
   - 使用 `webui.utils.i18n()` 函数进行国际化

4. **日志记录**:
   - 使用 `utils.logger.get_logger()` 获取日志记录器
   - 日志级别可通过 `--debug` 参数控制

5. **设备检测**:
   - 项目自动检测 CUDA/MPS/CPU
   - 使用 `device='auto'` 让系统自动选择最佳设备
   - GPU 设备通过 `device_ids` 参数指定

### 测试

项目目前没有自动化测试套件。手动测试建议：
1. 使用小规模音频文件测试推理功能
2. 验证不同模型类型的推理结果
3. 测试预设流程的完整性
4. 检查音频输出格式和质量

### 贡献指南

1. **代码提交前**:
   - 运行 `ruff check .` 检查代码风格
   - 运行 `ruff format .` 格式化代码
   - 确保所有功能正常工作

2. **添加新模型**:
   - 在 `modules/` 下创建新的模型实现
   - 在 `configs_backup/` 下添加对应的配置文件
   - 更新 `data_backup/models_info.json`
   - 在 `utils/constant.py` 中添加模型类型（如果需要）

3. **添加新功能**:
   - 在 `webui/` 下创建新的界面模块
   - 在 `webui/app.py` 中注册新标签页
   - 更新相关文档

## API 使用

### MSST 推理 API

```python
from inference.msst_infer import MSSeparator
from utils.logger import get_logger

separator = MSSeparator(
    model_type="bs_roformer",
    config_path="configs/vocal_models/model_bs_roformer_ep_368_sdr_12.9628.yaml",
    model_path="pretrain/vocal_models/model_bs_roformer_ep_368_sdr_12.9628.ckpt",
    device='auto',
    device_ids=[0],
    output_format='mp3',
    use_tta=False,
    store_dirs={"vocals": "results/vocals", "instrumental": "results/instrumental"},
    audio_params={"wav_bit_depth": "FLOAT", "flac_bit_depth": "PCM_24", "mp3_bit_rate": "320k"},
    logger=get_logger(),
    debug=True
)

# 处理文件夹
results = separator.process_folder("input")
separator.del_cache()

# 处理单个音频数组
import numpy as np
audio = separator.separate(np.ndarray)
separator.del_cache()
```

### VR 推理 API

```python
from inference.vr_infer import VRSeparator
from utils.logger import get_logger

separator = VRSeparator(
    logger=get_logger(),
    debug=True,
    model_file="pretrain/VR_Models/1_HP-UVR.pth",
    output_dir={"Vocals": "results/Vocals", "Instrumental": "results/instrumental"},
    output_format="mp3",
    use_cpu=False,
    vr_params={
        "batch_size": 2,
        "window_size": 512,
        "aggression": 5,
        "enable_tta": False,
        "enable_post_process": False,
        "post_process_threshold": 0.2,
        "high_end_process": False
    },
    audio_params={"wav_bit_depth": "FLOAT", "flac_bit_depth": "PCM_24", "mp3_bit_rate": "320k"},
)

results = separator.process_folder("input")
separator.del_cache()
```

## 训练和验证

### 训练

```bash
python train/train.py \
  --model_type bs_roformer \
  --config_path configs/vocal_models/model_bs_roformer_ep_368_sdr_12.9628.yaml \
  --data_path /path/to/dataset \
  --dataset_type 1 \
  --valid_path /path/to/validation \
  --device_ids 0 \
  --use_multistft_loss \
  --metrics sdr si_sdr
```

### 验证

```bash
python train/valid.py \
  --model_type bs_roformer \
  --config_path configs/vocal_models/model_bs_roformer_ep_368_sdr_12.9628.yaml \
  --start_check_point pretrain/vocal_models/model_bs_roformer_ep_368_sdr_12.9628.ckpt \
  --valid_path /path/to/validation \
  --store_dir results \
  --device_ids 0 \
  --use_tta
```

## 重要提示

1. **librosa 修复**: 安装依赖后，必须修复 librosa 中的类型错误，或使用提供的修复版本
2. **FFmpeg 要求**: 音频处理需要 FFmpeg，确保系统已安装
3. **GPU 内存**: 某些模型（如 swin_upernet）需要较多 GPU 内存
4. **模型下载**: 首次使用时需要从 Hugging Face 下载模型，可能需要较长时间
5. **云平台**: 在云平台运行时使用 `--use_cloud` 参数以获得更好的性能

## 相关资源

- **GitHub**: https://github.com/SUC-DriverOld/MSST-WebUI
- **Hugging Face**: https://huggingface.co/Sucial/MSST-WebUI
- **MSST 原始项目**: https://github.com/ZFTurbo/Music-Source-Separation-Training
- **UVR 项目**: https://github.com/Anjok07/ultimatevocalremovergui
- **SOME 项目**: https://github.com/openvpi/SOME/
- **中文文档**: https://r1kc63iz15l.feishu.cn/wiki/JSp3wk7zuinvIXkIqSUcCXY1nKc

## 许可证

AGPL-3.0