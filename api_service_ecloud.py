"""
MSST API 服务 - 基于移动云(EOS)存储
功能: 接收音立方请求,从移动云下载文件,执行音频分离,上传结果,生成预签名URL
"""
import os
import uuid
import shutil
import logging
import requests
from typing import List, Optional
from datetime import datetime, timedelta
from fastapi import FastAPI, UploadFile, File, BackgroundTasks, HTTPException
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from boto3.session import Session
from botocore.exceptions import ClientError

# 导入项目内部模块
from utils.logger import get_logger
from utils.constant import PRESETS
from webui.utils import load_configs
from inference.preset_infer import PresetInfer

# 初始化日志和应用
logger = get_logger()
app = FastAPI(title="MSST Audio Processing API - ECloud")

# 目录配置
INPUT_DIR = "inputs"
RESULTS_DIR = "results"
os.makedirs(INPUT_DIR, exist_ok=True)
os.makedirs(RESULTS_DIR, exist_ok=True)

# --- 移动云(EOS)配置 ---
# 建议通过环境变量设置
EOS_ENDPOINT = os.environ.get("EOS_ENDPOINT", "https://eos-wuxi-1.cmecloud.cn")
EOS_ACCESS_KEY = os.environ.get("EOS_ACCESS_KEY", "<your-access-key>")
EOS_SECRET_KEY = os.environ.get("EOS_SECRET_KEY", "<your-secret-access-key>")
EOS_BUCKET = os.environ.get("EOS_BUCKET", "<your-bucket-name>")
# 预签名URL有效期(秒)
PRESIGNED_URL_EXPIRE_SECONDS = 3600  # 1小时

# 初始化移动云客户端
session = Session(EOS_ACCESS_KEY, EOS_SECRET_KEY)
s3_client = session.client('s3', endpoint_url=EOS_ENDPOINT)


class ProcessRequest(BaseModel):
    """处理请求模型"""
    task_id: str
    preset_name: str
    ecloud_key: str  # 移动云对象Key
    callback_url: Optional[str] = None
    output_format: str = "wav"


class ProcessResponse(BaseModel):
    """处理响应模型"""
    task_id: str
    status: str
    message: str


def download_from_ecloud(bucket_name: str, key: str, local_path: str) -> bool:
    """从移动云下载文件"""
    try:
        logger.info(f"从移动云下载: {bucket_name}/{key} -> {local_path}")
        s3_client.download_file(bucket_name, key, local_path)
        logger.info(f"下载成功: {local_path}")
        return True
    except Exception as e:
        logger.error(f"下载失败: {e}")
        return False


def upload_to_ecloud(bucket_name: str, key: str, local_path: str, content_type: str = None) -> bool:
    """上传文件到移动云"""
    try:
        logger.info(f"上传到移动云: {local_path} -> {bucket_name}/{key}")
        
        extra_args = {}
        if content_type:
            extra_args['ContentType'] = content_type
        
        s3_client.upload_file(
            local_path, 
            bucket_name, 
            key, 
            ExtraArgs=extra_args
        )
        logger.info(f"上传成功: {key}")
        return True
    except Exception as e:
        logger.error(f"上传失败: {e}")
        return False


def generate_presigned_url(bucket_name: str, key: str, expire_seconds: int = 3600) -> Optional[str]:
    """生成预签名URL"""
    try:
        url = s3_client.generate_presigned_url(
            ClientMethod='get_object',
            Params={'Bucket': bucket_name, 'Key': key},
            ExpiresIn=expire_seconds,
            HttpMethod='GET'
        )
        logger.info(f"生成预签名URL: {key} (有效期{expire_seconds}秒)")
        return url
    except Exception as e:
        logger.error(f"生成预签名URL失败: {e}")
        return None


def run_inference_task(task_id: str, preset_name: str, output_format: str, 
                       callback_url: Optional[str], ecloud_key: str):
    """后台推理任务: 从移动云下载 -> 执行分离 -> 上传结果 -> 生成预签名URL -> 回调通知"""
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
    
    result_urls = []
    result_keys = []
    status = "processing"
    message = ""

    try:
        # 1. 从移动云下载音频文件
        logger.info(f"任务 {task_id}: 从移动云下载文件: {ecloud_key}")
        
        # 提取文件扩展名
        file_ext = os.path.splitext(ecloud_key)[1] if "." in ecloud_key else ".mp3"
        local_input_file = os.path.join(task_input_dir, f"input{file_ext}")
        
        if not download_from_ecloud(EOS_BUCKET, ecloud_key, local_input_file):
            raise Exception(f"从移动云下载文件失败: {ecloud_key}")

        # 2. 执行音频分离推理
        if not os.path.exists(preset_path):
            raise FileNotFoundError(f"Preset配置未找到: {preset_path}")

        logger.info(f"任务 {task_id}: 加载Preset配置: {preset_path}")
        preset_data = load_configs(preset_path)
        
        engine = PresetInfer(preset_data, force_cpu=False, logger=logger)
        logger.info(f"任务 {task_id}: 开始推理...")
        engine.process_folder(task_input_dir, task_output_dir, output_format, extra_output=True)
        
        # 3. 扫描结果文件并上传到移动云
        search_dir = os.path.join(task_output_dir, "extra_output")
        if not os.path.exists(search_dir):
            search_dir = task_output_dir

        for root, dirs, files in os.walk(search_dir):
            for file in files:
                if file.endswith(output_format):
                    local_path = os.path.join(root, file)
                    # 构造移动云存储路径
                    ecloud_dest_key = f"separated/{task_id}/{file}"
                    
                    # 根据文件类型设置content-type
                    content_type = None
                    if output_format == "wav":
                        content_type = "audio/wav"
                    elif output_format == "mp3":
                        content_type = "audio/mpeg"
                    elif output_format == "flac":
                        content_type = "audio/flac"
                    
                    # 上传到移动云
                    if upload_to_ecloud(EOS_BUCKET, ecloud_dest_key, local_path, content_type):
                        result_keys.append(ecloud_dest_key)
                        
                        # 生成预签名URL
                        presigned_url = generate_presigned_url(
                            EOS_BUCKET, 
                            ecloud_dest_key, 
                            PRESIGNED_URL_EXPIRE_SECONDS
                        )
                        
                        if presigned_url:
                            result_urls.append(presigned_url)
                    else:
                        logger.error(f"上传失败: {file}")

        if not result_urls:
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
                "ecloud_keys": result_keys,  # 移动云对象Key列表
                "download_urls": result_urls,  # 预签名URL列表(可直接下载)
                "url_expire_seconds": PRESIGNED_URL_EXPIRE_SECONDS,  # URL有效期
                "message": message
            }
            logger.info(f"任务 {task_id}: 发送回调到 {callback_url}")
            requests.post(callback_url, json=payload, timeout=10)
        except Exception as cb_e:
            logger.error(f"任务 {task_id}: 回调失败: {str(cb_e)}")

    # 清理本地临时文件
    shutil.rmtree(task_input_dir, ignore_errors=True)
    shutil.rmtree(task_output_dir, ignore_errors=True)


@app.post("/process_ecloud", response_model=ProcessResponse)
async def process_ecloud_audio(request: ProcessRequest, background_tasks: BackgroundTasks):
    """
    处理移动云流程接口
    由音立方服务调用,传入 ecloud_key 和 callback_url
    """
    if not request.ecloud_key:
        raise HTTPException(status_code=400, detail="ecloud_key is required")
    
    # 添加后台任务
    background_tasks.add_task(
        run_inference_task,
        request.task_id,
        request.preset_name,
        request.output_format,
        request.callback_url,
        request.ecloud_key
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
        "service": "MSST Audio Processing API",
        "storage": "ECloud",
        "timestamp": datetime.now().isoformat()
    }


@app.get("/")
async def root():
    """根路径"""
    return {
        "service": "MSST Audio Processing API",
        "version": "1.0.0",
        "storage": "ECloud",
        "endpoints": {
            "process": "/process_ecloud",
            "status": "/status/{task_id}",
            "health": "/health"
        }
    }


if __name__ == "__main__":
    import uvicorn
    # 启动API服务
    uvicorn.run(app, host="0.0.0.0", port=8000)