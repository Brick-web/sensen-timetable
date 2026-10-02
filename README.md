# sensen-timetable

森森课表开源版：本地课表编辑、假期与调休配置、桌面小组件。

## 功能

- 本地课表：点击空白格新增课程，支持按周次（1–20 周）选择上课周
- 假期与调休：在设置中自行配置，不依赖云端
- 学期设置：可配置「第一周周一」日期
- 桌面小组件：时段 / 全天 / 周课表

## 构建

```bash
./gradlew assembleDebug
```

安装包输出：`app/build/outputs/apk/debug/app-debug.apk`

## 与正式版区别

- 应用 ID：`ren.hieu.sensenapp.open`
- 数据保存在本机，无需绑定码

## License

MIT — see [LICENSE](LICENSE)
