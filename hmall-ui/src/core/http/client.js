import axios from "axios";
import { APP_CONFIG } from "@/core/config/env";

const client = axios.create({
  baseURL: APP_CONFIG.apiBaseURL,
  timeout: 600000 // 10分钟，方便后端debug
});

// 请求拦截器：自动携带 token
client.interceptors.request.use((config) => {
  const token = localStorage.getItem("xl_token");
  if (token) config.headers.authorization = token;
  return config;
});

// 响应拦截器：解包 data + 401 自动跳转登录
client.interceptors.response.use(
  (response) => response.data,
  async (error) => {
    const status = error?.response?.status;
    if (status === 401) {
      localStorage.removeItem("xl_token");
      localStorage.removeItem("xl_user");
      // 延迟导入 router 避免循环依赖
      const { default: router } = await import("@/router");
      router.push("/login");
    }
    return Promise.reject(error?.response?.data || error);
  }
);

export default client;
