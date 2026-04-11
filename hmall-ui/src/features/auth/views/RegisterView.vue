<template>
  <section class="register-wrap fade-up">
    <div class="register card">
      <div class="reg-header">
        <span class="reg-icon">🎉</span>
        <h1>注册小楼商城</h1>
      </div>
      <p class="subtitle">创建账号，开启品质购物之旅</p>
      <input v-model="form.username" placeholder="请输入用户名（2-20个字符）" @keyup.enter="focusNext('password')" />
      <input ref="passwordRef" v-model="form.password" type="password" placeholder="请输入密码（4-32个字符）" @keyup.enter="focusNext('phone')" />
      <input ref="phoneRef" v-model="form.phone" placeholder="手机号（选填）" @keyup.enter="handleRegister" />
      <button class="btn-primary" :disabled="submitting" @click="handleRegister">
        {{ submitting ? '注册中...' : '立即注册' }}
      </button>
      <p :class="{ error: isError, success: isSuccess }">{{ tip }}</p>
      <p class="switch">已有账号？<RouterLink to="/login">去登录</RouterLink></p>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useAuthStore } from "@/features/auth/store/auth.store";
import { registerByPassword } from "@/features/auth/api/register.api";
import { setStoredUserInfo } from "@/shared/utils";

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const tip = ref("请填写注册信息");
const isError = ref(false);
const isSuccess = ref(false);
const submitting = ref(false);
const form = reactive({
  username: "",
  password: "",
  phone: ""
});

const passwordRef = ref(null);
const phoneRef = ref(null);

const focusNext = (refName) => {
  if (refName === 'password') passwordRef.value?.focus();
  else if (refName === 'phone') phoneRef.value?.focus();
};

const handleRegister = async () => {
  if (!form.username || !form.password) {
    tip.value = "请输入用户名和密码";
    isError.value = true;
    isSuccess.value = false;
    return;
  }
  if (form.username.length < 2 || form.username.length > 20) {
    tip.value = "用户名长度为2-20个字符";
    isError.value = true;
    isSuccess.value = false;
    return;
  }
  if (form.password.length < 4) {
    tip.value = "密码长度至少4个字符";
    isError.value = true;
    isSuccess.value = false;
    return;
  }
  if (form.phone && !/^1[3-9]\d{9}$/.test(form.phone)) {
    tip.value = "手机号格式不正确";
    isError.value = true;
    isSuccess.value = false;
    return;
  }

  submitting.value = true;
  isError.value = false;
  isSuccess.value = false;
  try {
    const result = await registerByPassword(form);
    const data = result?.data || result || {};
    const token = data.token || "";
    if (token) {
      authStore.token = token;
      authStore.user = data.userId
        ? { userId: data.userId, username: data.username, balance: data.balance }
        : null;
      localStorage.setItem("xl_token", token);
      if (authStore.user) setStoredUserInfo(authStore.user);
      tip.value = "注册成功，正在跳转...";
      isSuccess.value = true;
      const redirect = route.query.redirect || "/";
      setTimeout(() => router.push(redirect), 400);
      return;
    }
    tip.value = "注册响应成功，但未返回 token。";
    isError.value = true;
  } catch (e) {
    const msg = e?.response?.data?.message || e?.message || "";
    tip.value = msg.includes("已存在") ? "用户名已存在，请换一个" : "注册失败，请稍后重试";
    isError.value = true;
  } finally {
    submitting.value = false;
  }
};
</script>

<style scoped>
.register-wrap {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 32px 20px;
  background: linear-gradient(180deg, #fff7f1 0%, #f5f5f5 100%);
}

.register {
  position: relative;
  width: min(440px, 92vw);
  padding: 34px 32px 30px;
  display: grid;
  gap: 14px;
  border-radius: 18px;
}

.register::before {
  content: "";
  position: absolute;
  inset: 0 0 auto;
  height: 6px;
  border-radius: 18px 18px 0 0;
  background: var(--bg-warm);
}

.reg-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.reg-icon {
  font-size: 28px;
}

h1 {
  margin: 0;
  font: 700 28px var(--font-display);
  color: var(--text-main);
}

.subtitle {
  margin: 0 0 4px;
  color: var(--text-sub);
  font-size: 14px;
}

input {
  height: 46px;
  padding: 0 14px;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

p {
  margin: 0;
  color: var(--text-sub);
  font-size: 13px;
}

p.error {
  color: #c0392b;
}

p.success {
  color: #27ae60;
}

.switch {
  text-align: center;
  margin-top: 4px;
}

.switch a {
  color: var(--brand);
  font-weight: 600;
}

.switch a:hover {
  text-decoration: underline;
}
</style>
