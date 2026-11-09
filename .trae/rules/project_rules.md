# 方壶美业加盟管控中台 — 项目规则

需求文档：`docs/方壶美业加盟管控中台_需求说明与技术开发文档_V1.0.docx`（字段、接口、状态机、建表脚本以文档为准，开发前先查文档对应章节，不要凭空设计）。

## 技术栈（固定，不得更换）

- 后端：Java 17 + Spring Boot 3.5.x + Sa-Token + MyBatis-Plus 3.5.x
- 数据库：PostgreSQL 16（本地开发 profile 用 H2 PG 兼容模式）；金额一律 `numeric(12,2)`/`BigDecimal`，禁止浮点
- 缓存：Redis 7（dev 环境可降级为内存实现）
- 对象存储：MinIO（dev 环境先本地文件存储占位）
- 前端：Vben Admin 5（vue-vben-admin monorepo，使用 apps/web-antd）+ Vue 3 + Vite + TS + Pinia + Ant Design Vue + Tailwind CSS + ECharts
- 部署：Docker Compose（单机私有化）

## 后端包结构（com.fangu.middle）

`common` 统一响应/异常/分页 | `auth` 登录鉴权 | `datascope` 数据权限注解+拦截器 | `audit` 审计注解+AOP | `org` 等级/加盟商/门店 | `material` 物料/分级定价 | `sale` 销货单/状态机 | `stock` 入库/库存/流水 | `report` 对账/导出/大盘 | `system` 账号/角色

模块间只允许上层依赖下层，禁止跨模块直接访问对方 Mapper。

## 五条核心业务规则（任何实现不得违反）

1. **定价解析**纯函数：直营→`direct_price`(DIRECT)；加盟→等级价(LEVEL)→缺失回退基准价(BASE)；解析结果≤0 拦截建单
2. **价格锁定**：明细创建时写 price 快照，查询不联查定价表，调价不回溯历史
3. **库存唯一**：`(store_id, material_id)` 唯一索引；库存累加用原子自增 SQL，先插后捕获唯一冲突重试一次
4. **单据不可删除**：仅 PENDING→CONFIRMED / PENDING→CANCELLED 状态机，条件更新 `WHERE status='PENDING'` 兜底并发
5. **变更留痕**：等级/价格/单据操作经 @Audit 切面记录 old_value/new_value

## 数据权限

- 三种范围：ALL（总部）/ FRANCHISEE（`franchisee_id`）/ STORE（`store_id`）
- 由 @DataScope 注解 + MyBatis 拦截器在 SQL 层追加条件，业务代码不拼接过滤条件
- 越权返回 403 并写审计日志；前端菜单显隐不算安全边界

## 接口规范

- 前缀 `/api/v1`，响应固定 `{code, msg, data, requestId}`；分页参数 `pageNum/pageSize`，返回 `total/records`
- 错误码：200 成功 / 401 未登录 / 403 越权 / 40001 参数 / 40002 门店不可用 / 40003 价格未维护 / 40004 状态不允许 / 40005 并发冲突 / 500 服务异常
- 编码生成：加盟商 `F+4位流水`、门店 `S+4位流水`、物料 `M+4位流水`、单号 `SO+yyyyMMdd+4位当日流水`

## 枚举字典（前后端与 DB 一致）

store_type: JOIN/DIRECT | franchisee.status: NORMAL/FROZEN/TERMINATED | level.status: ENABLED/DISABLED | order.status: PENDING/CONFIRMED/CANCELLED | price_source: DIRECT/LEVEL/BASE | flow_type: SALE_IN

## 前端约定（Vben Admin 5）

- 工程结构（已确认采用 monorepo，勿再套娃新建子项目）：`frontend/` 即 vue-vben-admin 仓库本体；**所有业务开发只写 `frontend/apps/web-antd/src/`**（api/ router/routes/modules/ store/ views/）；`packages/`、`internal/` 是 vben 框架底层库，不要改动；其他 apps 已删除
- 页面在 `apps/web-antd/src/views/{dashboard,org,material,sale,stock,report,system}`，接口封装在 `src/api/`，路由模块在 `src/router/routes/modules/`（accessMode=frontend，meta.authority 按 roleCode 控制）
- 请求适配：后端成功码为 `200`、提示字段为 `msg`；令牌放 `Authorization` 头（**无 Bearer 前缀**）；分页参数 `pageNum/pageSize`，返回 `total/records`
- 金额展示统一走格式化函数（两位小数），页面内不做四舍五入
- 入库确认提交前生成幂等号 `requestNo`，按钮点击即 loading
- 成本价仅总部可见，加盟商/门店侧不渲染

## 开发节奏

小步迭代：每个模块 后端接口 + 前端页面 + 自测 一起交付；先跑通 M1（组织与权限）主链路，再推进 M2/M3。测试账号与初始化数据放 `db/seed.sql`。
