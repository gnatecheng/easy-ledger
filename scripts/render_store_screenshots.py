#!/usr/bin/env python3
"""Generate 1080x2160 (2:1) PNG placeholders for Fastlane / docs until Robolectric capture is stable on CI."""
from pathlib import Path

try:
    from PIL import Image, ImageDraw, ImageFont
except ImportError:
    raise SystemExit("pip install pillow")

ROOT = Path(__file__).resolve().parents[1]
W, H = 1080, 2160
BG = "#FAF7F2"
FG = "#1F6F6A"

SHOTS = [
    ("docs/screenshots/play-readiness/settings_privacy_zh_light.png", "Settings · 隐私政策 (zh light)"),
    ("docs/screenshots/play-readiness/privacy_notice_zh_light.png", "First launch · 隐私说明 (zh light)"),
    ("docs/screenshots/play-readiness/privacy_notice_en_dark.png", "First launch · Privacy (en dark)"),
    ("docs/screenshots/play-readiness/privacy_policy_en.png", "Privacy policy screen (en)"),
    ("fastlane/metadata/android/en-US/images/phoneScreenshots/1_home.png", "Easy Ledger · Home (en)"),
    ("fastlane/metadata/android/zh-CN/images/phoneScreenshots/1_stats.png", "轻记账 · 统计 (zh)"),
]

def main() -> None:
    for rel, label in SHOTS:
        path = ROOT / rel
        path.parent.mkdir(parents=True, exist_ok=True)
        img = Image.new("RGB", (W, H), BG)
        draw = ImageDraw.Draw(img)
        draw.rectangle([0, 0, W, 120], fill=FG)
        draw.text((40, 40), label, fill="white")
        draw.text((40, 200), "Demo date: 2026-09-28", fill=FG)
        draw.text((40, 280), "Generated for store metadata (replace via StoreScreenshotsTest locally)", fill="#333333")
        img.save(path)
        print(path)

if __name__ == "__main__":
    main()
