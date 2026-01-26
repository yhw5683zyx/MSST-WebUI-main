"""
MSST Audio Processing API - 方案2: 直接文件上传/下载
不依赖 US3 云存储,使用本地文件传输
"""

import os
import uuid
import shutil
import logging
import requests
from typing import List, Optional
from fastapi import FastAPI, UploadFile, File, BackgroundTasks, HTTPException, Form
from fastapi.responses import FileResponse, JSONResponse
from pydantic import BaseModel

# 导入项目内部模块
from utils.logger import get_logger
from utils.constant import PRESETS, TEMP_PATH
from webui.utils import load_configs
from inference.preset_infer import PresetInfer

# 初始化日志和应用
logger = get_logger()
app = FastAPI(title="MSST Audio Processing API (Direct File Transfer)")

# 目录配置
INPUT_DIR = "inputs"
RESULTS_DIR = "results"
os.makedirs(INPUT_DIR, exist_ok=True)
os.makedirs(RESULTS_DIR, exist_ok=True)

# 任务状态存储 (生产环境建议使用 Redis)
task_status = {}


class UploadResponse(BaseModel):
    task_id: str
    status: str
    message: str


class TaskStatusResponse(BaseModel):
    task_id: str
    status: str  # processing, completed, failed
    message: Optional[str] = None
    results: Optional[List[str]] = None


def run_inference_task(
    task_id: str,
    preset_name: str,
    output_format: str,
    callback_url: Optional[str],
    input_filename: str
):
    """
    后台推理任务：读取本地文件 -> 执行分离 -> 保存到本地 -> 回调通知
    """
    task_input_dir = os.path.join(INPUT_DIR, task_id)
    task_output_dir = os.path.join(RESULTS_DIR, task_id)
    
    # 确定 Preset 配置文件路径
    if not preset_name.endswith(".yaml"):
        preset_file = preset_name + ".yaml"
    else:
        preset_file = preset_name
    preset_path = os.path.join(PRESETS, preset_file)
    
    os.makedirs(task_input_dir, exist_ok=True)
    os.makedirs(task_output_dir, exist_ok=True)
    
    result_files = []
    status = "processing"
    message = ""

    try:
        # 1. 验证输入文件存在
        input_file_path = os.path.join(task_input_dir, input_filename)
        if not os.path.exists(input_file_path):
            raise FileNotFoundError(f"Input file not found: {input_file_path}")

        logger.info(f"Task {task_id}: Input file: {input_file_path}")

        # 2. 验证 Preset 配置文件
        if not os.path.exists(preset_path):
            raise FileNotFoundError(f"Preset config not found: {preset_path}")

        logger.info(f"Task {task_id}: Loading preset {preset_path}")
        preset_data = load_configs(preset_path)
        
        # 3. 执行音频分离推理
        engine = PresetInfer(preset_data, force_cpu=False, logger=logger)
        logger.info(f"Task {task_id}: Starting inference...")
        
        # extra_output=True 会将最终结果放入 task_output_dir/extra_output
        engine.process_folder(task_input_dir, task_output_dir, output_format, extra_output=True)
        
        # 4. 扫描结果文件
        # 根据 PresetInfer 逻辑，结果文件在 extra_output 目录下
        search_dir = os.path.join(task_output_dir, "extra_output")
        if not os.path.exists(search_dir):
            search_dir = task_output_dir

        for root, dirs, files in os.walk(search_dir):
            for file in files:
                if file.endswith(output_format):
                    local_path = os.path.join(root, file)
                    # 返回相对路径,用于下载接口
                    relative_path = os.path.relpath(local_path, RESULTS_DIR)
                    result_files.append(relative_path)
                    logger.info(f"Task {task_id}: Result file: {relative_path}")

        if not result_files:
            raise Exception("Separation completed but no result files found.")

        status = "completed"
        message = "Success"
        
    except Exception as e:
        status = "failed"
        message = str(e)
        logger.error(f"Task {task_id} Error: {message}")
        # 清理失败的输出目录
        if os.path.exists(task_output_dir):
            shutil.rmtree(task_output_dir, ignore_errors=True)

    # 5. 更新任务状态
    task_status[task_id] = {
        "status": status,
        "message": message,
        "results": result_files if status == "completed" else []
    }

    # 6. 回调通知音立方服务 (Java)
    if callback_url:
        try:
            # 构造下载链接
            download_urls = [
                f"http://msst-server:8000/download/{task_id}/{os.path.basename(f)}"
                for f in result_files
            ]
            
            payload = {
                "task_id": task_id,
                "status": status,
                "results": result_files,
                "download_urls": download_urls,
                "message": message
            }
            logger.info(f"Task {task_id}: Sending callback to {callback_url}")
            logger.info(f"Task {task_id}: Callback payload: {payload}")
            requests.post(callback_url, json=payload, timeout=10)
        except Exception as cb_e:
            logger.error(f"Task {task_id}: Callback failed: {str(cb_e)}")

    # 注意: 不清理输入文件,让调用方决定何时清理
    # 输出文件也不清理,供音立方下载


@app.post("/upload", response_model=UploadResponse)
async def upload_audio(
    file: UploadFile = File(...),
    task_id: Optional[str] = Form(None),
    preset_name: str = Form("my_preset"),
    output_format: str = Form("wav"),
    callback_url: Optional[str] = Form(None),
    background_tasks: BackgroundTasks = BackgroundTasks()
):
    """
    上传音频文件并启动分离任务
    
    Args:
        file: 上传的音频文件
        task_id: 任务ID (可选,如果不提供则自动生成)
        preset_name: Preset 配置文件名
        output_format: 输出格式 (wav, mp3, flac)
        callback_url: 完成后的回调地址
    
    Returns:
        task_id: 任务ID
        status: 状态
        message: 消息
    """
    # 生成或使用提供的 task_id
    if not task_id:
        task_id = str(uuid.uuid4())
    
    # 创建任务目录
    task_input_dir = os.path.join(INPUT_DIR, task_id)
    os.makedirs(task_input_dir, exist_ok=True)
    
    # 保存上传的文件
    file_path = os.path.join(task_input_dir, file.filename)
    
    try:
        with open(file_path, "wb") as f:
            shutil.copyfileobj(file.file, f)
        
        logger.info(f"File uploaded: {file.filename} -> {file_path}")
        
        # 初始化任务状态
        task_status[task_id] = {
            "status": "processing",
            "message": "Task started",
            "results": []
        }
        
        # 启动后台推理任务
        background_tasks.add_task(
            run_inference_task,
            task_id,
            preset_name,
            output_format,
            callback_url,
            file.filename
        )
        
        return UploadResponse(
            task_id=task_id,
            status="accepted",
            message=f"File uploaded successfully, task started"
        )
        
    except Exception as e:
        logger.error(f"Upload failed: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Upload failed: {str(e)}")


@app.get("/status/{task_id}", response_model=TaskStatusResponse)
async def get_task_status(task_id: str):
    """
    查询任务状态
    
    Args:
        task_id: 任务ID
    
    Returns:
        task_id: 任务ID
        status: 状态 (processing, completed, failed)
        message: 消息
        results: 结果文件列表 (仅 completed 状态)
    """
    if task_id not in task_status:
        raise HTTPException(status_code=404, detail="Task not found")
    
    status_info = task_status[task_id]
    
    return TaskStatusResponse(
        task_id=task_id,
        status=status_info["status"],
        message=status_info.get("message"),
        results=status_info.get("results")
    )


@app.get("/download/{task_id}/{filename}")
async def download_result(task_id: str, filename: str):
    """
    下载分离结果文件
    
    Args:
        task_id: 任务ID
        filename: 文件名
    
    Returns:
        FileResponse: 文件下载响应
    """
    # 查找文件
    task_output_dir = os.path.join(RESULTS_DIR, task_id)
    
    # 在 extra_output 目录或根目录中查找
    possible_paths = [
        os.path.join(task_output_dir, "extra_output", filename),
        os.path.join(task_output_dir, filename)
    ]
    
    file_path = None
    for path in possible_paths:
        if os.path.exists(path):
            file_path = path
            break
    
    if not file_path:
        raise HTTPException(status_code=404, detail="File not found")
    
    logger.info(f"Downloading: {file_path}")
    
    return FileResponse(
        file_path,
        filename=filename,
        media_type='application/octet-stream'
    )


@app.get("/results/{task_id}")
async def list_results(task_id: str):
    """
    获取任务的所有结果文件列表
    
    Args:
        task_id: 任务ID
    
    Returns:
        results: 结果文件列表
    """
    if task_id not in task_status:
        raise HTTPException(status_code=404, detail="Task not found")
    
    status_info = task_status[task_id]
    
    if status_info["status"] != "completed":
        raise HTTPException(status_code=400, detail="Task not completed yet")
    
    return {
        "task_id": task_id,
        "results": status_info["results"],
        "download_urls": [
            f"http://msst-server:8000/download/{task_id}/{os.path.basename(f)}"
            for f in status_info["results"]
        ]
    }


@app.delete("/task/{task_id}")
async def cleanup_task(task_id: str):
    """
    清理任务文件 (输入和输出)
    
    Args:
        task_id: 任务ID
    
    Returns:
        message: 消息
    """
    task_input_dir = os.path.join(INPUT_DIR, task_id)
    task_output_dir = os.path.join(RESULTS_DIR, task_id)
    
    # 删除输入和输出目录
    shutil.rmtree(task_input_dir, ignore_errors=True)
    shutil.rmtree(task_output_dir, ignore_errors=True)
    
    # 删除任务状态
    if task_id in task_status:
        del task_status[task_id]
    
    logger.info(f"Task {task_id} cleaned up")
    
    return {"message": f"Task {task_id} cleaned up successfully"}


@app.get("/health")
async def health_check():
    """健康检查接口"""
    return {
        "status": "healthy",
        "service": "MSST Audio Processing API (Direct File Transfer)",
        "version": "2.0"
    }


if __name__ == "__main__":
    import uvicorn
    # 启动 API 服务，端口 8000
    uvicorn.run(app, host="0.0.0.0", port=8000)