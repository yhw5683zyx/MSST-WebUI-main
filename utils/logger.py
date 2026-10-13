import logging
import os
from colorama import Fore, Style
from datetime import datetime, timedelta
from logging.handlers import TimedRotatingFileHandler
from colorama import init
init(autoreset=True)

MAX_LOG = 100
LOG_RETENTION_DAYS = 7  # 日志保留天数, 超过7天的日志文件会被清理
LOG_DIR = "logs"
LOG_FILE = "service.log"  # 当天活跃日志文件, 每天凌晨0点切割为 service.log.YYYY-MM-DD


class ColorFormatter(logging.Formatter):
    def format(self, record):
        record.pathname = os.path.relpath(record.pathname)
        log_msg = super().format(record)

        if record.levelname == "INFO":
            log_msg = log_msg.replace("INFO", f"{Fore.GREEN}INFO{Style.RESET_ALL}")
        elif record.levelname == "DEBUG":
            log_msg = log_msg.replace("DEBUG", f"{Fore.BLUE}DEBUG{Style.RESET_ALL}")
        elif record.levelname == "WARNING":
            log_msg = log_msg.replace("WARNING", f"{Fore.YELLOW}WARNING{Style.RESET_ALL}")
        elif record.levelname == "ERROR":
            log_msg = log_msg.replace("ERROR", f"{Fore.RED}ERROR{Style.RESET_ALL}")
        elif record.levelname == "CRITICAL":
            log_msg = log_msg.replace("CRITICAL", f"{Fore.MAGENTA}CRITICAL{Style.RESET_ALL}")

        return log_msg


def manage_log_files(log_dir, max_log):
    """清理过期日志文件: 删除修改时间超过 LOG_RETENTION_DAYS 天的日志文件

    覆盖两类文件名:
    - 旧版按启动时间命名: 2026-10-13_14-30-25.log
    - 新版按天切割快照: service.log.2026-10-12
    """
    try:
        if not os.path.isdir(log_dir):
            return

        cutoff_time = datetime.now() - timedelta(days=LOG_RETENTION_DAYS)

        for filename in os.listdir(log_dir):
            if not (filename.endswith(".log") or ".log." in filename):
                continue

            filepath = os.path.join(log_dir, filename)
            if not os.path.isfile(filepath):
                continue

            if datetime.fromtimestamp(os.path.getmtime(filepath)) < cutoff_time:
                try:
                    os.remove(filepath)
                except Exception:
                    pass
    except Exception:
        pass


def set_log_level(logger, level):
    logger.console_handler.setLevel(level)


def get_logger(console_level=logging.INFO, max_log=MAX_LOG):
    logger = logging.getLogger("logger")

    if logger.hasHandlers():
        return logger

    logger.setLevel(logging.DEBUG)

    console_handler = logging.StreamHandler()
    console_handler.setLevel(console_level)
    formatter = ColorFormatter(fmt="%(asctime)s.%(msecs)03d [%(levelname)s] [%(pathname)s:%(lineno)d] %(message)s", datefmt="%H:%M:%S")
    console_handler.setFormatter(formatter)

    os.makedirs(LOG_DIR, exist_ok=True)

    # 按天滚动: 当天日志写入 logs/service.log, 每天凌晨0点切割为 logs/service.log.YYYY-MM-DD
    # backupCount=LOG_RETENTION_DAYS, 滚动时自动删除超过7天的旧快照
    file_handler = TimedRotatingFileHandler(
        filename=os.path.join(LOG_DIR, LOG_FILE),
        when="midnight",
        backupCount=LOG_RETENTION_DAYS,
        encoding="utf-8",
    )
    file_handler.setLevel(logging.DEBUG)
    file_formatter = logging.Formatter(fmt="%(asctime)s.%(msecs)03d [%(levelname)s] [%(pathname)s:%(lineno)d] %(message)s", datefmt="%H:%M:%S")
    file_handler.setFormatter(file_formatter)

    if not logger.hasHandlers():
        logger.addHandler(console_handler)
        logger.addHandler(file_handler)

    # 启动时兜底清理一次过期日志
    manage_log_files(LOG_DIR, max_log)

    logger.console_handler = console_handler

    return logger


if __name__ == "__main__":
    logger = get_logger(console_level=logging.INFO)
    logger.debug("This is a debug message.")
    logger.info("This is an info message.")
    logger.warning("This is a warning message.")
    logger.error("This is an error message.")
    logger.critical("This is a critical message.")
    set_log_level(logger, logging.DEBUG)
    logger.debug("This is a debug message after log level change.")
    logger.info("This is an info message after log level change.")
