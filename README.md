# sensen-timetable

森森课表开源版：本地课表编辑、假期与调休配置、桌面小组件。

## 功能

- 本地课表：点击空白格新增课程，支持按周次（1–20 周）选择上课周
- 假期与调休：在设置中自行配置，不依赖云端
- 学期设置：可配置「第一周周一」日期
- 桌面小组件：时段 / 全天 / 周课表

## 构建

```bash
# 调试包
./gradlew assembleDebug

# 正式包（GitHub Releases 用）
./gradlew assembleRelease
```

- Debug：`app/build/outputs/apk/debug/app-debug.apk`
- Release：`app/build/outputs/apk/release/app-release.apk`（版本见 `app/build.gradle.kts` 的 `versionName`）

## 发布

当前版本：`1.0.1`，`versionCode = 10001`。GitHub Release 标签：`v1.0.1`，附件命名：`sensen-timetable-open-1.0.1.apk`。

应用自动和手动检查更新均独立读取 `Brick-web/sensen-timetable` 的 GitHub 最新稳定 Release，下载该 Release 中已上传的 APK；不再请求正式版 `app_config.php`，也不会继承正式版强制更新或旧缓存中的下载链接。未发布 Release 时不提示更新；网络异常时可手动打开 GitHub Releases。

发布要求：标签使用 `v主版本.次版本.修订版本`（例如 `v1.0.2`），APK 的 `versionCode` 按 `major * 10000 + minor * 100 + patch` 递增，次版本和修订版本均为 0–99。上传 APK 并发布为稳定 Release，草稿和预发布不推送到稳定用户。请继续使用原有签名密钥，以便覆盖安装并保留本地课程数据。

1.0.1 修复了按日期自动切换教学周、小组件跨日/时段刷新及续排、活跃 Glance 会话旧数据问题，并同步课前通知权限检查和未来 7 天提醒续排。安装后打开一次应用；开启课前提醒时按提示允许通知和“闹钟和提醒”。

## 下载页

下载页统一放在 [`docs/download/`](docs/download/)（正式版工程内不再单独维护副本）：

- 正式版（主页，带开场动画）：`index.html`
- 开源版：`open.html` → [GitHub Releases](https://github.com/Brick-web/sensen-timetable/releases/latest)

本地预览：在 `docs/download` 目录用浏览器打开对应 HTML，或 `python -m http.server` 起静态服务。

## 与正式版区别

- 应用 ID：`ren.hieu.sensenapp.open`
- 数据保存在本机，无需绑定码

## License

MIT — see [LICENSE](LICENSE)
