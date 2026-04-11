# 小楼商城前端（Vue3）

## 项目说明
- 项目目录：`hmall-ui`
- 技术栈：Vue 3 + Vite + Vue Router + Pinia + Axios
- Node 版本：已按 `>=22.13.1` 适配
- 参考素材：来自 `前端资料/html/hmall-portal`，图片已复制到 `public/legacy-img`

## 启动
```bash
cd hmall-ui
npm install
npm run dev
```

## 构建
```bash
npm run build
npm run preview
```

## 解耦结构
- `src/core`: 基础能力层（环境配置、HTTP 客户端）
- `src/features`: 业务模块层（home/search/cart/auth/product/checkout）
- `src/shared`: 通用组件和全局样式
- `src/layouts`: 页面布局
- `src/router`: 路由聚合

## 后端联调
- 默认通过 Vite 代理：`/api -> http://127.0.0.1:8080`
- 若你的网关地址不同，修改 `.env.development` 中 `VITE_PROXY_TARGET`
- 统一 API 前缀在 `src/core/config/env.js` 控制

## 后续扩展建议
- 按 `features/order`, `features/pay`, `features/user` 新增模块
- 每个模块内部保持 `api + store + views + components` 结构
- 公共逻辑尽量下沉到 `core` 和 `shared`
