# YandeReViewer

[中文](#中文) | [English](README_en.md)

---

<a name="中文"></a>
## 项目简介

YandeReViewer 是一个 **第三方 Android 客户端**,用于浏览 **yande.re** 公共 API 提供的内容。采用现代 Android 开发实践,提供流畅直观的浏览体验,支持完整的国际化。

**重要提示**: 应用本身 **不托管、不存储、也不分发任何图片内容**。所有内容均来自 yande.re 公共 API。

---

## ✨ 功能特性

### 🖼️ 核心浏览体验
- **无限滚动**: 基于 Paging 3 实现无缝分页,结合 `DiffUtil` 高效 diff 处理
- **标签搜索**: 
  - 支持多标签组合搜索(空格分隔)
  - 实时自动补全(来自 yande.re 标签数据库)
  - 标签智能分类显示(艺术家/版权/角色/通用)
  - 长按标签复制到剪贴板
  - 彩色标签芯片区分不同类型
- **评分筛选**: 通过复选框按 全年龄(Safe)/存疑(Questionable)/限制级(Explicit) 过滤帖子
- **智能缓存**: 多层缓存策略:
  - Coil 图片缓存(内存+磁盘)
  - Paging 3 浏览数据缓存
  - 标签元数据缓存(`TagTypeCache`)优化 API 使用
  - 应用启动时清理过期缓存
- **双浏览模式**:
  - **NORMAL 模式**: 从 yande.re API 无限滚动加载分页结果
  - **FAVORITES 模式**: 仅本地收藏内容,支持标签过滤
- **黑名单系统**: 自动过滤包含黑名单标签的帖子

### 📸 图片查看
- **沉浸式详情**: 
  - 全屏图片查看器,支持双指缩放(基于 `PhotoView` 库)
  - 单击图片进入完全沉浸模式
  - 优雅的共享元素转场动画
  - 自定义返回动画(根据滚动位置智能选择)
- **滑动切换**: 详情页左右滑动在相邻帖子间平滑切换,ViewPager2 支持
- **优化加载**: 
  - 低分辨率预览图作占位符,高清图后台加载
  - 渐变过渡效果,无闪烁
  - 内存缓存键复用避免重复加载
- **并行图片获取**: 自定义 Coil fetcher 将大图分为 4 块并行下载加速
- **交互式标签**: 
  - 点击任意标签(艺术家/版权/角色/通用)立即搜索
  - 长按标签弹出菜单:复制/加入黑名单/收藏标签
  - 艺术家标签显示别名(日文/罗马音)
  - 标签按类型分组显示
- **来源访问**: 
  - URL 来源直接在浏览器打开
  - 文本来源在对话框显示
  - 智能识别 URL 格式并补全协议

### 💾 用户功能
- **本地保存(用户主动)**: 用户明确操作时保存图片到设备存储
  - API 30+: 支持长按拖拽保存手势(1秒触发)
  - API <30: 系统 DownloadManager 集成
  - 详情页可长按图片直接下载
  - 下载完成通知
- **批量操作**: 
  - 长按网格项进入选择模式
  - 多选图片批量保存
  - 批量复制图片链接到剪贴板
  - 选择模式下工具栏显示选中数量
- **收藏管理**: 
  - 通过 SharedPreferences 本地保存收藏帖子
  - 记录收藏时间戳
  - 详情页浮动按钮一键收藏/取消
  - 导航抽屉快速切换收藏视图
  - 收藏视图支持标签过滤
- **标签收藏**: 
  - 收藏常用标签便于快速搜索
  - 标签按类型分组管理
  - 长按标签菜单:复制/移除
  - 点击收藏标签立即搜索
- **黑名单系统**: 
  - 标签黑名单自动过滤不想看的内容
  - 黑名单独立管理页面
  - 本地持久化存储

### 🌐 现代化体验与技术优势
- **国际化(i18n)**: 完整支持英文和中文(`values/` 和 `values-zh-rCN/`)
- **智能更新检测**: 
  - 应用启动时自动检查 GitHub Release
  - 对话框显示版本号和完整更新日志
  - 一键下载安装最新 APK
- **标签元数据同步**: 
  - 应用启动时后台增量同步标签分类
  - 支持艺术家、版权、角色、通用标签分类
  - 本地 JSON 缓存,无需重复下载
  - 预打包初始数据,首次启动即可用
- **Material Design 3**: 
  - 动态配色方案(Android 12+)
  - Jetpack Compose 混合 XML 布局
  - 流畅的 Material You 设计语言
- **响应式动画**: 
  - 共享元素转场动画
  - 基于滚动位置的自定义退出动画
  - 下拉刷新动画
  - 平滑滚动到顶部
- **导航抽屉**: 
  - 收藏标签管理
  - 收藏图片视图
  - 黑名单管理
  - 历史记录(规划中)
- **瀑布流布局**: 两列自适应网格,优化空间利用
- **浮动操作按钮**: 
  - 快速滚动到顶部
  - 手动刷新当前列表

---

## 🛠️ 技术栈

| 组件 | 技术 |
|------|------|
| **语言** | Kotlin |
| **架构** | MVVM + Paging 3 |
| **异步** | Kotlin 协程 + Flow |
| **网络** | Retrofit2 + Gson + OkHttp3 |
| **图片加载** | Coil + 自定义并行 fetcher |
| **UI 框架** | Jetpack Compose + XML 布局 |
| **分页** | Paging 3 (`PagingSource`, `PagingDataAdapter`) |
| **存储** | SharedPreferences + JSON 序列化 |
| **缩放库** | PhotoView |

### 关键组件
- **ViewModels**: `PostViewModel`(分页逻辑)、`UpdateViewModel`(版本检查)
- **PagingSource**: `PostPagingSource` 逐页查询 yande.re API
- **网络客户端**: `YandeApi`(帖子/标签端点)、`GitHubApiClient`(版本发布)
- **自定义 Fetcher**: `ParallelImageFetcher` 优化图片下载
- **标签系统**: `TagTypeCache` + `TagSyncer` 管理元数据

---

## 🚀 快速开始

### 下载预编译 APK
从以下链接下载最新版本 APK:

[查看最新 Release](https://github.com/AliceJump/YandeReViewer/releases)

### 从源码编译

```bash
# 克隆仓库
git clone https://github.com/AliceJump/YandeReViewer.git
cd YandeReViewer

# Debug 构建(使用 release 签名配置)
./gradlew installDebug

# 带 ProGuard 的 Release 构建
./gradlew assembleRelease
```

### 签名配置
- **CI/CD**: 使用环境变量(`SIGNING_KEY_STORE_PATH`、`SIGNING_KEY_ALIAS` 等)
- **本地开发**: 在 `local.properties` 中配置:
  ```properties
  SIGNING_KEY_STORE_PATH=/path/to/keystore.jks
  SIGNING_KEY_ALIAS=your_alias
  SIGNING_KEY_PASSWORD=your_key_password
  SIGNING_STORE_PASSWORD=your_store_password
  ```

---

## ⚖️ 免责声明与版权声明

- 本应用为 **第三方客户端**,与 yande.re **无关联**
- 所有图片及元数据直接来自 **yande.re 公共 API**
- **应用不托管、不存储、不分发任何受版权保护的内容**
- 所有作品及相关内容归其各自版权持有者所有
- 通过应用保存的文件 **仅存储在用户本地设备**
- 关于本仓库的 DMCA 相关问题,请开 issue 或联系仓库所有者

---

## 📄 许可证

本项目采用 **MIT License** 许可。详情见 [LICENSE](LICENSE)
