package com.example.zhijiexing.data

/**
 * 知识学习内容：按 HarmonyOS 应用开发者认证的知识范围，
 * 从华为开发者官网的指南中提炼要点，详情页保留官方原文链接。
 */
data class KnowledgeSection(
    val heading: String,
    val body: String,
    val code: String? = null
)

data class KnowledgePoint(
    val id: String,
    val title: String,
    val tag: String,
    val minutes: Int,
    val summary: String,
    val sections: List<KnowledgeSection>,
    val sourceTitle: String,
    val sourceUrl: String
)

val knowledgePoints = listOf(
    KnowledgePoint(
        id = "arkts-basics",
        title = "ArkTS 与声明式 UI",
        tag = "ArkTS",
        minutes = 4,
        summary = "从 @Entry、@Component 到 build()，理解状态如何驱动界面。",
        sections = listOf(
            KnowledgeSection(
                heading = "页面与组件",
                body = "页面入口用 @Entry 标记，自定义组件用 @Component 声明，界面结构写在 build() 中。组件树由声明式代码描述，不需要手动查找和更新控件。"
            ),
            KnowledgeSection(
                heading = "状态驱动刷新",
                body = "@State 修饰的变量变化时，依赖它的 UI 会自动重新渲染。写界面时先想清楚数据怎么变，而不是逐个控件去改。"
            ),
            KnowledgeSection(
                heading = "最小示例",
                body = "计数器把状态和按钮绑定在一起，点击后界面由框架自动刷新。",
                code = "@Entry\n" +
                    "@Component\n" +
                    "struct Counter {\n" +
                    "  @State count: number = 0\n" +
                    "  build() {\n" +
                    "    Column() {\n" +
                    "      Text('点击次数：' + this.count)\n" +
                    "      Button('加一').onClick(() => { this.count++ })\n" +
                    "    }\n" +
                    "  }\n" +
                    "}"
            )
        ),
        sourceTitle = "学习 ArkTS 语言",
        sourceUrl = "https://developer.huawei.com/consumer/cn/doc/harmonyos-guides/learning-arkts"
    ),
    KnowledgePoint(
        id = "state-management",
        title = "状态管理：@State / @Prop / @Link",
        tag = "状态管理",
        minutes = 5,
        summary = "分清组件内状态、父传子单向和父子双向，把数据流理顺。",
        sections = listOf(
            KnowledgeSection(
                heading = "组件内状态",
                body = "@State 是组件私有状态，只在当前组件内修改；它变化时只刷新依赖它的那部分 UI。"
            ),
            KnowledgeSection(
                heading = "父子之间",
                body = "@Prop 做单向复制，父组件更新会同步给子组件；@Link 建立双向引用，子组件修改会回写父组件。"
            ),
            KnowledgeSection(
                heading = "跨层级与嵌套对象",
                body = "@Provide / @Consume 可以跨多层组件共享状态；对象内部属性变化用 @Observed / @ObjectLink 观察。"
            ),
            KnowledgeSection(
                heading = "双向绑定示例",
                body = "子组件通过 @Link 改计数，父组件持有的 count 同步变化。",
                code = "@Component\n" +
                    "struct Stepper {\n" +
                    "  @Link count: number\n" +
                    "  build() {\n" +
                    "    Button('加一').onClick(() => { this.count++ })\n" +
                    "  }\n" +
                    "}"
            )
        ),
        sourceTitle = "状态管理 V1",
        sourceUrl = "https://developer.huawei.com/consumer/cn/doc/harmonyos-guides/arkts-state-management-v1"
    ),
    KnowledgePoint(
        id = "arkui-layout",
        title = "ArkUI 布局：Row / Column / Stack",
        tag = "ArkUI",
        minutes = 5,
        summary = "先选容器再放内容：线性、层叠、弹性与列表各有场景。",
        sections = listOf(
            KnowledgeSection(
                heading = "线性布局",
                body = "Row 水平排列、Column 垂直排列，space 控制子组件间距，justifyContent 与 alignItems 分别控制主轴和交叉轴对齐。"
            ),
            KnowledgeSection(
                heading = "层叠与弹性",
                body = "Stack 让子组件叠放，适合角标和浮层；Flex 按比例分配空间，layoutWeight 可以让子组件占满剩余空间。"
            ),
            KnowledgeSection(
                heading = "长列表",
                body = "大量条目优先使用 List 配合 LazyForEach 按需创建，避免一次性构建全部节点造成卡顿。"
            )
        ),
        sourceTitle = "构建布局",
        sourceUrl = "https://developer.huawei.com/consumer/cn/doc/harmonyos-guides/arkts-build-layout"
    ),
    KnowledgePoint(
        id = "navigation",
        title = "页面导航：Navigation 与路由栈",
        tag = "路由",
        minutes = 4,
        summary = "用 NavPathStack 管理页面进出，逐步替代 Router。",
        sections = listOf(
            KnowledgeSection(
                heading = "推荐方案",
                body = "Navigation 配合 NavDestination 是官方推荐的路由方案，页面通过 NavPathStack 入栈、出栈，参数和返回结果都由路由栈管理。"
            ),
            KnowledgeSection(
                heading = "与 Router 的差异",
                body = "Router 页面栈最多 32 个页面，且不支持一多部署与路由栈操作；新项目应优先选择 Navigation。"
            ),
            KnowledgeSection(
                heading = "常用操作",
                body = "按名称携带参数入栈，返回时弹出栈顶页面。",
                code = "this.pageStack.pushPathByName('detail', id)\n" +
                    "this.pageStack.pop()"
            )
        ),
        sourceTitle = "设置组件导航和页面路由",
        sourceUrl = "https://developer.huawei.com/consumer/cn/doc/harmonyos-guides/arkts-navigation-navigation"
    ),
    KnowledgePoint(
        id = "http-request",
        title = "网络请求：HTTP 数据请求",
        tag = "网络",
        minutes = 6,
        summary = "http.createHttp() 发起请求，声明网络权限并及时销毁实例。",
        sections = listOf(
            KnowledgeSection(
                heading = "基本流程",
                body = "创建 HttpRequest，调用 request() 发起 GET 或 POST，读取 responseCode 与 result，请求结束后调用 destroy() 释放资源。"
            ),
            KnowledgeSection(
                heading = "关键约束",
                body = "一个 httpRequest 对应一个请求任务，不可复用；使用网络前需要在 module.json5 中声明 ohos.permission.INTERNET。"
            ),
            KnowledgeSection(
                heading = "GET 示例",
                body = "请在实际业务中替换 URL，并处理超时与错误码。",
                code = "const req = http.createHttp()\n" +
                    "const res = await req.request(url, {\n" +
                    "  method: http.RequestMethod.GET\n" +
                    "})\n" +
                    "req.destroy()"
            )
        ),
        sourceTitle = "HTTP 数据请求",
        sourceUrl = "https://developer.huawei.com/consumer/cn/doc/harmonyos-guides/http-request"
    ),
    KnowledgePoint(
        id = "preferences",
        title = "数据持久化：用户首选项",
        tag = "数据",
        minutes = 5,
        summary = "轻量配置和登录态用 Preferences，写入后记得 flush。",
        sections = listOf(
            KnowledgeSection(
                heading = "适用场景",
                body = "Preferences 提供 Key-Value 轻量存储，适合主题、字号、登录令牌等小数据；内容全量载入内存，读取速度快。"
            ),
            KnowledgeSection(
                heading = "使用步骤",
                body = "getPreferences 获取实例，put 写入键值，flush 落盘；删除数据使用 delete，而不是直接改内存。"
            ),
            KnowledgeSection(
                heading = "边界",
                body = "首选项不支持多进程并发访问；数据量较大或需要查询时，应改用关系型数据库 RelationalStore。"
            )
        ),
        sourceTitle = "通过用户首选项实现数据持久化",
        sourceUrl = "https://developer.huawei.com/consumer/cn/doc/harmonyos-guides/data-persistence-by-preferences"
    ),
    KnowledgePoint(
        id = "taskpool",
        title = "并发：TaskPool 任务池",
        tag = "并发",
        minutes = 6,
        summary = "把耗时计算交给任务池，主线程只负责界面。",
        sections = listOf(
            KnowledgeSection(
                heading = "运行机制",
                body = "taskpool.execute 把任务提交到队列，系统维护工作线程池并按负载动态扩缩容，开发者不需要手动管理线程生命周期。"
            ),
            KnowledgeSection(
                heading = "使用约束",
                body = "并发函数需要 @Concurrent 标注并写在 .ets 文件中；子线程不能直接操作 UI，结果要回到主线程再更新状态。"
            ),
            KnowledgeSection(
                heading = "提交任务",
                body = "并发函数只做纯计算，入参与返回值需可序列化。",
                code = "@Concurrent\n" +
                    "function parse(raw: string): string {\n" +
                    "  return raw.trim()\n" +
                    "}\n" +
                    "taskpool.execute(parse, raw)"
            )
        ),
        sourceTitle = "TaskPool 简介",
        sourceUrl = "https://developer.huawei.com/consumer/cn/doc/harmonyos-guides/taskpool-introduction"
    ),
    KnowledgePoint(
        id = "uiability",
        title = "应用生命周期：UIAbility",
        tag = "应用模型",
        minutes = 4,
        summary = "掌握 UIAbility 的回调顺序，在正确时机保存和恢复状态。",
        sections = listOf(
            KnowledgeSection(
                heading = "关键回调",
                body = "onCreate 创建实例，onWindowStageCreate 加载页面，onForeground 进入可见，onBackground 退到后台，onWindowStageDestroy 与 onDestroy 负责收尾。"
            ),
            KnowledgeSection(
                heading = "实践",
                body = "退到后台时在 onBackground 暂停计时或记录状态；刷题应用的切后台计数也发生在这个时机。"
            ),
            KnowledgeSection(
                heading = "注意",
                body = "进入后台后不要执行长耗时操作，系统可能随时回收进程；需要保留的数据应提前落盘。"
            )
        ),
        sourceTitle = "UIAbility 生命周期",
        sourceUrl = "https://developer.huawei.com/consumer/cn/doc/harmonyos-guides/uiability-lifecycle"
    ),
    KnowledgePoint(
        id = "component-extension",
        title = "组件复用：@Builder 与 @Styles",
        tag = "组件",
        minutes = 5,
        summary = "把重复的 UI 结构抽成 Builder，把公共样式抽成 Styles。",
        sections = listOf(
            KnowledgeSection(
                heading = "复用 UI 结构",
                body = "@Builder 装饰的函数可以把一段 UI 描述抽出来复用，既可以定义在组件内，也可以全局定义；配合 @BuilderParam 还能让父组件把一段 UI 作为参数传给子组件。"
            ),
            KnowledgeSection(
                heading = "复用样式",
                body = "@Styles 用于抽取组件的通用属性，@Extend 针对某一类组件扩展专属属性，两者都能减少重复的链式写法。"
            ),
            KnowledgeSection(
                heading = "使用边界",
                body = "@Styles 只能作用于通用属性，@Extend 需要绑定具体组件且通常全局定义；带状态的界面片段更适合写成自定义组件，而不是把所有逻辑塞进 Builder。"
            )
        ),
        sourceTitle = "组件扩展",
        sourceUrl = "https://developer.huawei.com/consumer/cn/doc/harmonyos-guides/arkts-extend-components"
    ),
    KnowledgePoint(
        id = "lazy-foreach",
        title = "长列表渲染：ForEach 与 LazyForEach",
        tag = "性能",
        minutes = 6,
        summary = "短列表用 ForEach，海量数据交给 LazyForEach 按需创建。",
        sections = listOf(
            KnowledgeSection(
                heading = "按需渲染",
                body = "LazyForEach 只创建可视区域附近的列表项，滚动时回收并复用组件，适合长列表、图片流和大数据量场景；ForEach 会在首次渲染时构建全部子组件。"
            ),
            KnowledgeSection(
                heading = "数据源",
                body = "使用 LazyForEach 需要数据源实现 IDataSource 接口，提供数据总数、按索引取数和数据变更监听；数据增删后通过监听器通知界面刷新。"
            ),
            KnowledgeSection(
                heading = "唯一键值",
                body = "每个数据项要提供稳定且唯一的键值，键值重复或频繁变化会导致组件复用错乱、列表闪烁。",
                code = "LazyForEach(this.dataSource, (item: Item) => {\n" +
                    "  ListItem() { Text(item.title) }\n" +
                    "}, (item: Item) => item.id)"
            )
        ),
        sourceTitle = "LazyForEach：数据懒加载",
        sourceUrl = "https://developer.huawei.com/consumer/cn/doc/harmonyos-guides/arkts-rendering-control-lazyforeach"
    ),
    KnowledgePoint(
        id = "rdb",
        title = "关系型数据库：RelationalStore",
        tag = "数据",
        minutes = 6,
        summary = "结构化数据用 RDB 存，查询完记得关闭 ResultSet。",
        sections = listOf(
            KnowledgeSection(
                heading = "适用场景",
                body = "RelationalStore 基于 SQLite，支持建表、索引、事务和条件查询，适合错题、记录等结构化且需要检索的数据；轻量配置仍然优先用用户首选项。"
            ),
            KnowledgeSection(
                heading = "基本操作",
                body = "getRdbStore 获取实例后用 executeSql 建表，insert / update / delete 写入，querySql 查询返回 ResultSet，读取完必须调用 close() 释放。"
            ),
            KnowledgeSection(
                heading = "性能建议",
                body = "批量写入放在事务里减少落盘次数；单行数据不宜过大，海量查询可以放到 TaskPool 执行，避免阻塞主线程。",
                code = "const store = await relationalStore.getRdbStore(context, {\n" +
                    "  name: 'exam.db',\n" +
                    "  securityLevel: relationalStore.SecurityLevel.S1\n" +
                    "})\n" +
                    "await store.executeSql('CREATE TABLE IF NOT EXISTS wrong (id INTEGER PRIMARY KEY, title TEXT)')\n" +
                    "const rs = await store.querySql('SELECT id, title FROM wrong')\n" +
                    "rs.close()"
            )
        ),
        sourceTitle = "通过关系型数据库实现数据持久化",
        sourceUrl = "https://developer.huawei.com/consumer/cn/doc/harmonyos-guides/data-persistence-by-rdb-store"
    ),
    KnowledgePoint(
        id = "package-structure",
        title = "工程结构：HAP / HAR / HSP",
        tag = "工程",
        minutes = 5,
        summary = "分清安装包、静态库和动态库，按复用范围拆模块。",
        sections = listOf(
            KnowledgeSection(
                heading = "三种包",
                body = "HAP 是应用安装和运行的基本单元，分为 entry 与 feature；HAR 是静态共享库，编译时被打进使用方；HSP 是动态共享库，运行时多模块共享一份，能减小包体积。"
            ),
            KnowledgeSection(
                heading = "拆分思路",
                body = "通用工具与 UI 组件放 HAR；多个模块共享且体积较大的能力放 HSP；feature HAP 适合按需下载的功能模块。"
            ),
            KnowledgeSection(
                heading = "依赖方式",
                body = "在 oh-package.json5 中声明依赖后即可 import 使用，动态共享库还需要保证对应模块随应用一起安装。",
                code = "// oh-package.json5\n" +
                    "\"dependencies\": {\n" +
                    "  \"common\": \"file:../common\"\n" +
                    "}"
            )
        ),
        sourceTitle = "应用程序包概述",
        sourceUrl = "https://developer.huawei.com/consumer/cn/doc/harmonyos-guides/application-package-overview"
    ),
    KnowledgePoint(
        id = "permissions",
        title = "应用权限：动态申请与校验",
        tag = "安全",
        minutes = 5,
        summary = "敏感权限先声明再弹窗，用户拒绝也要有兜底。",
        sections = listOf(
            KnowledgeSection(
                heading = "两类权限",
                body = "system_grant 权限安装时自动授予；user_grant 权限涉及用户隐私，必须在 module.json5 中声明用途，并在运行时向用户弹窗申请。"
            ),
            KnowledgeSection(
                heading = "申请流程",
                body = "通过 abilityAccessCtrl 的 requestPermissionsFromUser 发起申请，根据返回的 authResults 判断是否授权；每次使用敏感能力前再校验一次，避免中途被收回。"
            ),
            KnowledgeSection(
                heading = "体验细节",
                body = "申请前说明用途，用户拒绝后不要反复弹窗，可引导到设置页手动开启；功能降级时要给出明确提示。",
                code = "const atManager = abilityAccessCtrl.createAtManager()\n" +
                    "const result = await atManager.requestPermissionsFromUser(\n" +
                    "  context, ['ohos.permission.CAMERA']\n" +
                    ")"
            )
        ),
        sourceTitle = "向用户申请授权",
        sourceUrl = "https://developer.huawei.com/consumer/cn/doc/harmonyos-guides/request-user-authorization"
    ),
    KnowledgePoint(
        id = "continuous-task",
        title = "后台任务：长时任务",
        tag = "后台",
        minutes = 6,
        summary = "音频、导航等场景申请长时任务，结束后及时释放。",
        sections = listOf(
            KnowledgeSection(
                heading = "适用场景",
                body = "长时任务让应用退到后台后继续运行，覆盖音频播放、导航、定位、录制等场景；普通耗时计算应交给 TaskPool，短任务可用延迟任务。"
            ),
            KnowledgeSection(
                heading = "申请方式",
                body = "调用 backgroundTaskManager 的 startBackgroundRunning 申请长时任务并指定背景模式，需在 module.json5 声明 backgroundModes 与 KEEP_BACKGROUND_RUNNING 权限。"
            ),
            KnowledgeSection(
                heading = "及时释放",
                body = "任务结束时调用 stopBackgroundRunning；长期占用会被系统清理，也会影响后续申请与上架审核。",
                code = "const manager = backgroundTaskManager\n" +
                    "const mode = manager.BackgroundMode\n" +
                    "  .AUDIO_PLAYBACK\n" +
                    "await manager.startBackgroundRunning(\n" +
                    "  context, mode, wantAgentObj\n" +
                    ")"
            )
        ),
        sourceTitle = "长时任务（ArkTS）",
        sourceUrl = "https://developer.huawei.com/consumer/cn/doc/harmonyos-guides/continuous-task"
    ),
    KnowledgePoint(
        id = "notification",
        title = "本地通知：Notification Kit",
        tag = "通知",
        minutes = 5,
        summary = "用通知提醒复习，点击还能回到指定页面。",
        sections = listOf(
            KnowledgeSection(
                heading = "发布流程",
                body = "先通过 notificationManager.requestEnableNotification 请求通知授权，再构造 NotificationRequest 并调用 publish，通知会展示在通知中心与锁屏。"
            ),
            KnowledgeSection(
                heading = "内容样式",
                body = "基础文本、长文本、多行和图片等样式由 content 决定，标题与正文要简短明确，避免堆砌营销文案。"
            ),
            KnowledgeSection(
                heading = "点击跳转",
                body = "在 NotificationRequest 中绑定 WantAgent，用户点击通知可以拉起应用并直达对应页面；不再需要的通知用 cancel 及时撤销。",
                code = "const BASIC_TEXT =\n" +
                    "  notificationManager.ContentType\n" +
                    "    .NOTIFICATION_CONTENT_BASIC_TEXT\n" +
                    "const request = {\n" +
                    "  id: 1,\n" +
                    "  content: {\n" +
                    "    notificationContentType: BASIC_TEXT,\n" +
                    "    normal: { title: '错题复习', text: '还有 3 道错题' }\n" +
                    "  }\n" +
                    "}\n" +
                    "await notificationManager.publish(request)"
            )
        ),
        sourceTitle = "Notification Kit 简介",
        sourceUrl = "https://developer.huawei.com/consumer/cn/doc/harmonyos-guides/notification-overview"
    ),
    KnowledgePoint(
        id = "hilog",
        title = "日志调试：HiLog",
        tag = "日志",
        minutes = 4,
        summary = "按级别和业务域打日志，敏感参数记得加 public。",
        sections = listOf(
            KnowledgeSection(
                heading = "基本用法",
                body = "通过 @ohos.hilog 按 debug / info / warn / error / fatal 级别输出日志，参数依次是业务域、标签和格式串，方便在 DevEco Studio 的日志面板按模块过滤。"
            ),
            KnowledgeSection(
                heading = "隐私标识",
                body = "格式串中的参数默认按私隐处理，日志里只显示占位符；需要展示的参数要显式写成 %{public}s、%{public}d 等。"
            ),
            KnowledgeSection(
                heading = "排查习惯",
                body = "关键流程打点、异常带上下文；正式版本避免打印账号、令牌等敏感信息，高频日志也会影响性能。",
                code = "hilog.info(0x0000, 'Exam', '加载题目 %{public}d 道', count)"
            )
        ),
        sourceTitle = "使用 HiLog 打印日志（ArkTS）",
        sourceUrl = "https://developer.huawei.com/consumer/cn/doc/harmonyos-guides/hilog-guidelines-arkts"
    )
)

fun knowledgeById(id: String): KnowledgePoint? = knowledgePoints.firstOrNull { it.id == id }
