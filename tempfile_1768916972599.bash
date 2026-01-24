python scripts/msst_cli.py \
  --model_type mdx23c \
  --config_path configs/multi_stem_models/config_file.yaml \
  --model_path pretrain/model.ckpt \
  --input_folder input/ \
  --output_folder results/ \
  --output_format wav \
  --device cuda \
  --device_ids 0
