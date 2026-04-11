import { defineStore } from "pinia";
import { loginByPassword } from "@/features/auth/api/auth.api";
import { setStoredUserInfo, getStoredUserInfo } from "@/shared/utils";

export const useAuthStore = defineStore("auth", {
  state: () => ({
    token: localStorage.getItem("xl_token") || "",
    user: getStoredUserInfo()
  }),
  getters: {
    isLoggedIn: (state) => !!state.token
  },
  actions: {
    async login(payload) {
      const result = await loginByPassword(payload);
      const data = result?.data || result || {};
      const token = data.token || "";
      this.token = token;
      this.user = data.userId
        ? { userId: data.userId, username: payload.username || data.username, balance: data.balance }
        : null;
      if (token) localStorage.setItem("xl_token", token);
      if (this.user) setStoredUserInfo(this.user);
      return token;
    },
    logout() {
      this.token = "";
      this.user = null;
      localStorage.removeItem("xl_token");
      setStoredUserInfo(null);
    }
  }
});
