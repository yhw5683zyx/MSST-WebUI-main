"""
MSST API 服务 - 轻量化版本 (无SDK依赖)
功能: 接收音立方请求,通过预签名URL下载文件,执行音频分离,上传结果到预签名URL
优势: 无需移动云SDK,轻量化,只负责音频分离
"""
import os
import sys
import shutil
import aiohttp
from typing import Optional
from datetime import datetime
from fastapi import FastAPI, BackgroundTasks, HTTPException
from pydantic import BaseModel

# 添加项目根目录到 Python 路径
current_dir = os.path.dirname(os.path.abspath(__file__))
if current_dir not in sys.path:
    sys.path.insert(0, current_dir)

# 导入项目内部模块
from utils.logger import get_logger
from utils.constant import PRESETS
from webui.utils import load_configs
from inference.preset_infer import PresetInfer

# 初始化日志和应用
logger = get_logger()
app = FastAPI(title="MSST Audio Processing API - Lightweight")

# 目录配置
INPUT_DIR = "input"
RESULTS_DIR = "results"
TEMP_DIR = "temp"
os.makedirs(INPUT_DIR, exist_ok=True)
os.makedirs(RESULTS_DIR, exist_ok=True)


class ProcessRequest(BaseModel):
    """处理请求模型"""
    task_id: str
    preset_name: str
    download_url: str  # 音立方生成的下载URL (预签名)
    upload_urls: dict  # 音立方生成的上传URL (预签名), key: 文件名, value: URL
    callback_url: Optional[str] = None


class ProcessResponse(BaseModel):
    """处理响应模型"""
    task_id: str
    status: str
    message: str


async def download_from_url(url: str, local_path: str) -> bool:
    """通过预签名URL下载文件 (无需SDK)"""
    try:
        logger.info(f"从URL下载: {url} -> {local_path}")

        async with aiohttp.ClientSession() as session:
            async with session.get(url) as response:
                if response.status == 200:
                    content = await response.read()
                    with open(local_path, 'wb') as f:
                        f.write(content)
                    logger.info(f"下载成功: {local_path}")
                    return True
                else:
                    logger.error(f"下载失败, HTTP状态码: {response.status}")
                    return False
    except Exception as e:
        logger.error(f"下载失败: {e}")
        return False


async def upload_to_url(local_path: str, upload_url: str) -> bool:
    """上传文件到预签名URL (无需SDK)"""
    try:
        logger.info(f"上传到URL: {local_path} -> {upload_url}")

        # 获取文件大小
        file_size = os.path.getsize(local_path)
        logger.debug(f"文件大小: {file_size} bytes")

        # 读取文件
        with open(local_path, 'rb') as f:
            file_data = f.read()

        # 确定 Content-Type
        content_type = 'audio/wav'
        if local_path.endswith('.mp3'):
            content_type = 'audio/mpeg'
        elif local_path.endswith('.flac'):
            content_type = 'audio/flac'

        headers = {
            'Content-Type': content_type,
            'Content-Length': str(file_size)
        }

        async with aiohttp.ClientSession() as session:
            async with session.put(upload_url, data=file_data, headers=headers) as response:
                response_text = await response.text()
                logger.debug(f"响应状态: {response.status}, 响应内容: {response_text[:200]}")

                if response.status == 200:
                    logger.info(f"上传成功")
                    return True
                else:
                    logger.error(f"上传失败, HTTP状态码: {response.status}, 响应: {response_text}")
                    return False
    except Exception as e:
        logger.error(f"上传失败: {e}")
        import traceback
        logger.debug(traceback.format_exc())
        return False


async def run_inference_task(task_id: str, preset_name: str,
                             download_url: str, upload_urls: dict,
                             callback_url: Optional[str]):
    """后台推理任务: 通过预签名URL下载 -> 执行分离 -> 上传到预签名URL -> 回调通知"""
    task_input_dir = os.path.join(INPUT_DIR, task_id)
    task_output_dir = os.path.join(RESULTS_DIR, task_id)

    # 确定 Preset 配置文件路径
    if not preset_name.endswith(".json"):
        preset_file = preset_name + ".json"
    else:
        preset_file = preset_name
    preset_path = os.path.join(PRESETS, preset_file)

    os.makedirs(task_input_dir, exist_ok=True)
    os.makedirs(task_output_dir, exist_ok=True)

    uploaded_files = []

    try:
        # 1. 通过预签名URL下载音频文件
        logger.info(f"任务 {task_id}: 从预签名URL下载文件")

        # 提取文件扩展名
        file_ext = os.path.splitext(download_url.split('?')[0])[1] if "." in download_url.split('?')[0] else ".mp3"
        local_input_file = os.path.join(task_input_dir, f"input{file_ext}")

        if not await download_from_url(download_url, local_input_file):
            raise Exception(f"从预签名URL下载文件失败: {download_url}")

        # 2. 执行音频分离推理
        if not os.path.exists(preset_path):
            raise FileNotFoundError(f"Preset配置未找到: {preset_path}")

        logger.info(f"任务 {task_id}: 加载Preset配置: {preset_path}")
        preset_data = load_configs(preset_path)

        engine = PresetInfer(preset_data, force_cpu=False, logger=logger)
        logger.info(f"任务 {task_id}: 开始推理...")
        engine.process_folder(task_input_dir, task_output_dir, "wav", extra_output=True)

        # 3. 扫描结果文件并上传到预签名URL
        search_dir = os.path.join(task_output_dir, "extra_output")
        if not os.path.exists(search_dir):
            search_dir = task_output_dir

        for root, dirs, files in os.walk(search_dir):
            for file in files:
                if file.endswith(".wav") or file.endswith(".mp3") or file.endswith(".flac"):
                    local_path = os.path.join(root, file)

                    # 从upload_urls中查找对应的上传URL
                    upload_url = upload_urls.get(file)

                    if upload_url:
                        # 上传到预签名URL
                        if await upload_to_url(local_path, upload_url):
                            uploaded_files.append(file)
                            logger.info(f"上传成功: {file}")
                        else:
                            logger.error(f"上传失败: {file}")
                    else:
                        logger.warning(f"未找到文件 {file} 的上传URL")

        if not uploaded_files:
            raise Exception("分离完成但未成功上传任何结果文件")

        status = "completed"
        message = "Success"

    except Exception as e:
        status = "failed"
        message = str(e)
        logger.error(f"任务 {task_id} 错误: {message}")

    # 4. 回调通知音立方服务
    if callback_url:
        try:
            payload = {
                "task_id": task_id,
                "status": status,
                "uploaded_files": uploaded_files,
                "message": message
            }
            logger.info(f"任务 {task_id}: 发送回调到 {callback_url}")

            async with aiohttp.ClientSession() as session:
                async with session.post(callback_url, json=payload, timeout=aiohttp.ClientTimeout(total=10)) as response:
                    if response.status == 200:
                        logger.info(f"回调成功")
                    else:
                        logger.error(f"回调失败, HTTP状态码: {response.status}")

        except Exception as cb_e:
            logger.error(f"任务 {task_id}: 回调失败: {str(cb_e)}")

    # 清理本地临时文件
    shutil.rmtree(task_input_dir, ignore_errors=True)
    shutil.rmtree(task_output_dir, ignore_errors=True)


@app.post("/process_lightweight", response_model=ProcessResponse)
async def process_audio(request: ProcessRequest, background_tasks: BackgroundTasks):
    """
    轻量化处理接口
    由音立方服务调用,传入预签名URL
    """
    if not request.download_url:
        raise HTTPException(status_code=400, detail="download_url is required")

    if not request.upload_urls:
        raise HTTPException(status_code=400, detail="upload_urls is required")

    # 添加后台任务
    background_tasks.add_task(
        run_inference_task,
        request.task_id,
        request.preset_name,
        request.download_url,
        request.upload_urls,
        request.callback_url
    )

    return ProcessResponse(
        task_id=request.task_id,
        status="accepted",
        message="Task accepted, processing in background"
    )


@app.get("/status/{task_id}")
async def get_status(task_id: str):
    """查询任务状态"""
    task_dir = os.path.join(RESULTS_DIR, task_id)
    if os.path.exists(task_dir):
        return {"task_id": task_id, "status": "processing"}
    return {"task_id": task_id, "status": "completed_or_not_found"}


@app.get("/health")
async def health_check():
    """健康检查"""
    return {
        "status": "healthy",
        "service": "MSST Audio Processing API - Lightweight",
        "features": ["No SDK dependency", "Presigned URL support"],
        "timestamp": datetime.now().isoformat()
    }


@app.get("/")
async def root():
    """根路径"""
    return {
        "service": "MSST Audio Processing API",
        "version": "2.0.0 (Lightweight)",
        "features": [
            "No SDK dependency",
            "Presigned URL support",
            "Lightweight design"
        ],
        "endpoints": {
            "process": "/process_lightweight",
            "status": "/status/{task_id}",
            "health": "/health"
        }
    }


if __name__ == "__main__":
    import uvicorn
    # 启动API服务
    uvicorn.run(app, host="0.0.0.0", port=8000)