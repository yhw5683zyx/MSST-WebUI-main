import os
import uuid
import shutil
import logging
import requests
from typing import List, Optional
from fastapi import FastAPI, UploadFile, File, BackgroundTasks, HTTPException
from fastapi.responses import FileResponse
from pydantic import BaseModel
from ufile import config, filemanager

# 导入项目内部模块
from utils.logger import get_logger
from utils.constant import PRESETS
from webui.utils import load_configs
from inference.preset_infer import PresetInfer

# 初始化日志和应用
logger = get_logger()
app = FastAPI(title="MSST Audio Processing API")

# 目录配置
INPUT_DIR = "inputs"
RESULTS_DIR = "results"
os.makedirs(INPUT_DIR, exist_ok=True)
os.makedirs(RESULTS_DIR, exist_ok=True)

# --- US3 配置 (建议通过环境变量或在此处直接修改) ---
PUBLIC_KEY = os.environ.get("US3_PUBLIC_KEY", "您的公钥")
PRIVATE_KEY = os.environ.get("US3_PRIVATE_KEY", "您的私钥")
BUCKET = os.environ.get("US3_BUCKET", "您的存储空间名称")
# 根据地域设置后缀 (例如 .cn-bj.ufileos.com)
UPLOAD_SUFFIX = os.environ.get("US3_UPLOAD_SUFFIX", ".cn-bj.ufileos.com") 
DOWNLOAD_SUFFIX = os.environ.get("US3_DOWNLOAD_SUFFIX", ".cn-bj.ufileos.com")

config.set_default(uploadsuffix=UPLOAD_SUFFIX)
config.set_default(downloadsuffix=DOWNLOAD_SUFFIX)
ufile_handler = filemanager.FileManager(PUBLIC_KEY, PRIVATE_KEY)

class ProcessRequest(BaseModel):
    task_id: str
    preset_name: str
    callback_url: Optional[str] = None
    output_format: str = "wav"
    us3_key: Optional[str] = None  # 音立方服务传来的 US3 文件路径 (Key)

def run_inference_task(task_id: str, preset_name: str, output_format: str, callback_url: Optional[str], us3_key: Optional[str] = None):
    """后台推理任务：从 US3 下载 -> 执行分离 -> 上传结果到 US3 -> 回调通知"""
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
    
    us3_result_keys = []
    status = "processing"
    message = ""

    try:
        # 1. 从 US3 下载音频文件
        if us3_key:
            logger.info(f"Task {task_id}: Downloading from US3: {us3_key}")
            # 提取扩展名或默认使用 .mp3
            file_ext = os.path.splitext(us3_key)[1] if "." in us3_key else ".mp3"
            local_input_file = os.path.join(task_input_dir, f"input{file_ext}")
            
            _, resp = ufile_handler.download_file(BUCKET, us3_key, local_input_file)
            if resp.status_code != 200:
                raise Exception(f"Failed to download from US3, status: {resp.status_code}")
        else:
            raise Exception("Missing us3_key in request")

        # 2. 执行音频分离推理
        if not os.path.exists(preset_path):
            raise FileNotFoundError(f"Preset config not found: {preset_path}")

        logger.info(f"Task {task_id}: Loading preset {preset_path}")
        preset_data = load_configs(preset_path)
        
        engine = PresetInfer(preset_data, force_cpu=False, logger=logger)
        logger.info(f"Task {task_id}: Starting inference...")
        # extra_output=True 会将最终结果放入 task_output_dir/extra_output
        engine.process_folder(task_input_dir, task_output_dir, output_format, extra_output=True)
        
        # 3. 扫描结果并将文件上传回 US3
        # 根据 PresetInfer 逻辑，结果文件在 extra_output 目录下
        search_dir = os.path.join(task_output_dir, "extra_output")
        if not os.path.exists(search_dir):
            search_dir = task_output_dir

        for root, dirs, files in os.walk(search_dir):
            for file in files:
                if file.endswith(output_format):
                    local_path = os.path.join(root, file)
                    # 构造在 US3 上的存储路径 (Key)
                    us3_dest_key = f"separated/{task_id}/{file}"
                    
                    logger.info(f"Task {task_id}: Uploading to US3: {us3_dest_key}")
                    ret, resp = ufile_handler.putfile(BUCKET, us3_dest_key, local_path)
                    
                    if resp.status_code == 200:
                        us3_result_keys.append(us3_dest_key)
                    else:
                        logger.error(f"Task {task_id}: Failed to upload {file}, status: {resp.status_code}")

        if not us3_result_keys:
            raise Exception("Separation completed but no results were successfully uploaded.")

        status = "completed"
        message = "Success"
        
    except Exception as e:
        status = "failed"
        message = str(e)
        logger.error(f"Task {task_id} Error: {message}")

    # 4. 回调通知音立方服务 (Java)
    if callback_url:
        try:
            payload = {
                "task_id": task_id,
                "status": status,
                "us3_results": us3_result_keys,  # 返回 US3 的 Key 列表
                "message": message
            }
            logger.info(f"Task {task_id}: Sending callback to {callback_url}")
            requests.post(callback_url, json=payload, timeout=10)
        except Exception as cb_e:
            logger.error(f"Task {task_id}: Callback failed: {str(cb_e)}")

    # 清理本地临时文件
    shutil.rmtree(task_input_dir, ignore_errors=True)
    shutil.rmtree(task_output_dir, ignore_errors=True)

@app.post("/process_us3")
async def process_us3_audio(request: ProcessRequest, background_tasks: BackgroundTasks):
    """
    触发 US3 流程接口
    由音立方服务 (Java) 调用，传入 us3_key 和 callback_url
    """
    if not request.us3_key:
        raise HTTPException(status_code=400, detail="us3_key is required")
    
    background_tasks.add_task(
        run_inference_task, 
        request.task_id, 
        request.preset_name, 
        request.output_format, 
        request.callback_url,
        request.us3_key
    )
    
    return {"status": "accepted", "task_id": request.task_id}

@app.get("/status/{task_id}")
async def get_status(task_id: str):
    """查询任务本地状态"""
    # 由于后台任务完成后会清理目录，此处仅返回存在性
    task_dir = os.path.join(RESULTS_DIR, task_id)
    if os.path.exists(task_dir):
        return {"task_id": task_id, "status": "processing"}
    return {"task_id": task_id, "status": "completed_or_not_found"}

if __name__ == "__main__":
    import uvicorn
    # 启动 API 服务，端口 8000
    uvicorn.run(app, host="0.0.0.0", port=8000)
