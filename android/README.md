# KenyaTrip Android App

中肯双向 AI 旅行规划 App 的 Android MVP（Kotlin + Jetpack Compose）。对应 [docs/04](../docs/04-AI产品与技术架构.md) 中的规划引擎与一键预订 Saga，可在手机上离线跑通完整链路：

1. **一句话需求理解**：本地中英文规则解析（目的地、月份、天数、人数/孩子年龄、预算 CNY/KES/USD、风格、必去地点/项目、出发城市）；可选接入 OpenAI 兼容接口的大模型（通义千问、DeepSeek 等），失败自动回退本地解析。
2. **AI 路线推荐**：在目的地图上做约束搜索（季节评分、风格偏好、每日车程上限、预算、疲劳度），输出 3 套差异化方案及推荐理由。
3. **行程规划**：按天生成时刻表、可预订组件与价格明细；校验器给出签证、疫苗、年龄限制、车程等提醒；支持自然语言/快捷指令改行程（轻松一点、便宜一点、升级酒店、加减天数、加/去某地、加某项目）。
4. **一键预订**：实时核价、锁定库存、一次支付、按优先级向供应商确认；失败时自动替换同档或更高档（差价 15% 以内由平台承担），否则逆序取消并全额退款；幂等键防重复下单。
5. **我的行程**：订单、供应商确认号、出行准备清单（按目的地生成），本地持久化。

入肯（中国游客）以人民币计价，支付方式为微信/支付宝/银联；入华（肯尼亚居民）以肯先令计价，支付方式为 M-Pesa/银行卡。界面支持中文/English，可在设置中切换。

> 演示说明：目的地、酒店、价格均为示例数据（名称带“示例/sample”），供应商与支付为模拟实现，不会产生真实订单或扣款。设置页“演示”区可模拟营地满房（自动替换）、无替代库存（自动退款）、支付失败。正式版本应通过服务端 LLM 网关调用大模型，密钥不保存在手机上。

## 构建与运行

环境要求：JDK 17+、Android SDK（platform 35、build-tools 35）。

```bash
cd android
echo "sdk.dir=$ANDROID_HOME" > local.properties   # 或用 Android Studio 打开 android/ 目录
./gradlew testDebugUnitTest                         # 核心逻辑单元测试
./gradlew assembleDebug                             # 产物 app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

最低支持 Android 8.0（API 26），目标 API 35。

## 代码结构

```
app/src/main/java/com/kenyatrip/app/
  core/                 纯 Kotlin，无 Android 依赖，可单测
    model/              领域模型（需求、地点、行程、可预订组件）
    knowledge/          肯尼亚/中国知识库（地点、交通边、住宿、项目、入境规则）
    planner/            需求解析、路线搜索与打分、行程生成、校验、对话式修改
    pricing/            加价规则与币种换算
    supply/             供应商适配接口与模拟供应网络
    booking/            预订编排（Saga）与模拟支付
    ai/                 OpenAI 兼容的大模型解析器（结构化输出 + 知识库校验）
  data/                 设置与行程本地存储
  ui/                   Compose 界面、导航与 ViewModel
app/src/test/           需求解析、路线规划、行程修改、预订 Saga、大模型解析测试
```

接入真实供应商时，实现 `SupplyNetwork` / `SupplierAdapter`（报价、锁定、确认、取消）与支付网关接口即可替换模拟实现，规划与编排逻辑不变。
