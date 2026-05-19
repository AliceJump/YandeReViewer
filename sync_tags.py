#!/usr/bin/env python3
"""
同步yande.re的所有标签到assets资源文件夹

使用方式:
python sync_tags.py
"""

import json
import requests
import sys
import time
from pathlib import Path

API_BASE = "https://yande.re"
TAGS_ENDPOINT = f"{API_BASE}/tag.json"
PAGE_SIZE = 2000
REQUEST_TIMEOUT = 15

def fetch_tags_from_page(page: int) -> list or None:
    """从API获取指定页的标签"""
    try:
        url = f"{TAGS_ENDPOINT}?page={page}&limit={PAGE_SIZE}&order=date"
        print(f"📥 正在获取第{page}页: {url}")
        
        response = requests.get(
            url,
            timeout=REQUEST_TIMEOUT,
            headers={"User-Agent": "YandeReViewerTagSync/1.0"}
        )
        
        if response.status_code != 200:
            print(f"❌ API返回状态码 {response.status_code}")
            return None
        
        tags = response.json()
        return tags
    except Exception as e:
        print(f"❌ 获取第{page}页失败: {str(e)}")
        return None

def main():
    print("🚀 开始同步yande.re标签资源...")
    print("=" * 60)
    
    assets_dir = Path("app/src/main/assets")
    if not assets_dir.exists():
        print(f"❌ 找不到assets目录: {assets_dir.absolute()}")
        sys.exit(1)
    
    all_tags = {}
    max_id = 0
    page = 1
    total_fetched = 0
    
    # 循环获取所有页
    while True:
        tags = fetch_tags_from_page(page)
        if tags is None or len(tags) == 0:
            print(f"✅ 到达最后一页 (第{page}页无数据)")
            break
        
        total_fetched += len(tags)
        print(f"   ✓ 第{page}页: 获取了{len(tags)}个标签")
        
        for tag in tags:
            all_tags[tag["name"]] = tag["type"]
            if tag["id"] > max_id:
                max_id = tag["id"]
        
        page += 1
        
        # 延迟以避免API限流
        time.sleep(0.5)
    
    if not all_tags:
        print("❌ 没有获取到任何标签")
        sys.exit(1)
    
    print()
    print("=" * 60)
    print("📊 统计信息:")
    print(f"   - 总标签数: {len(all_tags)}")
    print(f"   - 最大ID: {max_id}")
    print(f"   - 从{page - 1}页获取")
    
    # 写入tags_name_type.json
    tags_file = assets_dir / "tags_name_type.json"
    try:
        with open(tags_file, 'w', encoding='utf-8') as f:
            json.dump(all_tags, f, ensure_ascii=False, indent=2)
        file_size = tags_file.stat().st_size
        print(f"✅ 标签文件已更新: {tags_file.absolute()}")
        print(f"   文件大小: {file_size / 1024 / 1024:.2f}MB")
    except Exception as e:
        print(f"❌ 写入标签文件失败: {str(e)}")
        sys.exit(1)
    
    # 写入last_id.txt
    last_id_file = assets_dir / "last_id.txt"
    try:
        with open(last_id_file, 'w', encoding='utf-8') as f:
            f.write(str(max_id))
        print(f"✅ last_id文件已更新: {max_id}")
    except Exception as e:
        print(f"❌ 写入last_id文件失败: {str(e)}")
        sys.exit(1)
    
    print()
    print("🎉 标签资源同步完成！")
    print("=" * 60)
    print()
    print("下一步:")
    print("1. 重新编译APK: ./gradlew clean assembleDebug")
    print("2. 安装到设备: ./gradlew installDebug")
    print("3. 应用启动时会自动更新assets中的标签文件到缓存目录")

if __name__ == "__main__":
    main()
