<template>
  <section class="login-shell fade-up">
    <div class="login-scene">
      <div class="scene-copy">
        <p class="scene-kicker">LOGIN / XIAOLOU MALL</p>
        <h1>把购物体验<br />切换到你的频道</h1>
        <p class="scene-desc">
          欢迎来到小楼商城，各种好物任你挑选
        </p>
      </div>

      <div
        ref="artboardRef"
        class="artboard"
        aria-hidden="true"
        :style="artboardTransformStyle"
        @mousemove="handlePointerMove"
        @mouseleave="resetEyes"
      >
        <div class="orbit orbit-a"></div>
        <div class="orbit orbit-b"></div>
        <div class="shape tower violet">
          <span class="eye">
            <span class="pupil" :style="pupilStyle('violetLeft')"></span>
          </span>
          <span class="eye">
            <span class="pupil" :style="pupilStyle('violetRight')"></span>
          </span>
        </div>
        <div class="shape tower dark">
          <span class="eye eye-line">
            <span class="line-pupil" :style="pupilStyle('darkLeft')"></span>
          </span>
          <span class="eye eye-line">
            <span class="line-pupil" :style="pupilStyle('darkRight')"></span>
          </span>
        </div>
        <div class="shape dome coral">
          <span class="eye">
            <span class="pupil" :style="pupilStyle('coralLeft')"></span>
          </span>
          <span class="eye">
            <span class="pupil" :style="pupilStyle('coralRight')"></span>
          </span>
        </div>
        <div class="shape dome yellow">
          <span class="eye">
            <span class="pupil" :style="pupilStyle('yellowLeft')"></span>
          </span>
          <span class="eye">
            <span class="pupil" :style="pupilStyle('yellowRight')"></span>
          </span>
          <span class="mouth"></span>
        </div>
      </div>
    </div>

    <div class="login-panel">
      <div class="panel-head">
        <span class="panel-mark">✦</span>
        <p>账号密码登录</p>
      </div>

      <div
        class="login card"
        :style="panelTransformStyle"
        @mousemove="handlePanelMove"
        @mouseleave="resetPanelTilt"
      >
        <div class="login-header">
          <p class="welcome">欢迎回来</p>
          <h2>登录小楼商城</h2>
          <p class="subtitle">输入你的账号信息，继续浏览商品与订单。</p>
        </div>

        <div class="field-group">
          <label for="username">用户名</label>
          <input id="username" v-model="form.username" placeholder="请输入用户名" @keyup.enter="handleLogin" />
        </div>

        <div class="field-group">
          <label for="password">密码</label>
          <input id="password" v-model="form.password" type="password" placeholder="请输入密码" @keyup.enter="handleLogin" />
        </div>

        <button class="btn-primary login-btn" :disabled="submitting" @click="handleLogin">
          {{ submitting ? '登录中...' : '立即登录' }}
        </button>

        <p class="tip" :class="{ error: isError, success: !isError && tip.includes('成功') }">{{ tip }}</p>
        <p class="switch">还没有账号？<RouterLink to="/register">立即注册</RouterLink></p>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, reactive, ref } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useAuthStore } from "@/features/auth/store/auth.store";

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const tip = ref("请输入账号密码登录");
const isError = ref(false);
const submitting = ref(false);
const artboardRef = ref(null);
const form = reactive({
  username: "",
  password: ""
});

const eyeOrigins = {
  violetLeft: { x: 0.34, y: 0.29 },
  violetRight: { x: 0.47, y: 0.29 },
  darkLeft: { x: 0.56, y: 0.41 },
  darkRight: { x: 0.64, y: 0.41 },
  coralLeft: { x: 0.23, y: 0.73 },
  coralRight: { x: 0.33, y: 0.73 },
  yellowLeft: { x: 0.67, y: 0.57 },
  yellowRight: { x: 0.75, y: 0.57 }
};

const eyeOffsets = reactive(
  Object.keys(eyeOrigins).reduce((acc, key) => {
    acc[key] = { x: 0, y: 0 };
    return acc;
  }, {})
);
const sceneTilt = reactive({ x: 0, y: 0 });
const panelTilt = reactive({ x: 0, y: 0 });

const moveEye = (key, pointerX, pointerY) => {
  const origin = eyeOrigins[key];
  const board = artboardRef.value;
  if (!origin || !board) return;

  const rect = board.getBoundingClientRect();
  const centerX = rect.width * origin.x;
  const centerY = rect.height * origin.y;
  const deltaX = pointerX - centerX;
  const deltaY = pointerY - centerY;
  const distance = Math.max(Math.hypot(deltaX, deltaY), 1);
  const maxMove = key.startsWith("dark") ? 4 : 7;

  eyeOffsets[key].x = (deltaX / distance) * maxMove;
  eyeOffsets[key].y = (deltaY / distance) * maxMove;
};

const handlePointerMove = (event) => {
  const board = artboardRef.value;
  if (!board) return;
  const rect = board.getBoundingClientRect();
  const pointerX = event.clientX - rect.left;
  const pointerY = event.clientY - rect.top;
  const percentX = pointerX / rect.width - 0.5;
  const percentY = pointerY / rect.height - 0.5;

  sceneTilt.x = percentY * -9;
  sceneTilt.y = percentX * 11;

  Object.keys(eyeOrigins).forEach((key) => {
    moveEye(key, pointerX, pointerY);
  });
};

const resetEyes = () => {
  Object.keys(eyeOffsets).forEach((key) => {
    eyeOffsets[key].x = 0;
    eyeOffsets[key].y = 0;
  });
  sceneTilt.x = 0;
  sceneTilt.y = 0;
};

const pupilStyle = (key) => {
  const offset = eyeOffsets[key];
  return {
    transform: `translate(${offset.x}px, ${offset.y}px)`
  };
};

const handlePanelMove = (event) => {
  const panel = event.currentTarget;
  const rect = panel.getBoundingClientRect();
  const percentX = event.clientX / rect.width - rect.left / rect.width - 0.5;
  const percentY = event.clientY / rect.height - rect.top / rect.height - 0.5;
  panelTilt.x = percentY * -7;
  panelTilt.y = percentX * 7;
};

const resetPanelTilt = () => {
  panelTilt.x = 0;
  panelTilt.y = 0;
};

const artboardTransformStyle = computed(() => ({
  transform: `perspective(1200px) rotateX(${sceneTilt.x}deg) rotateY(${sceneTilt.y}deg) translateZ(0)`
}));

const panelTransformStyle = computed(() => ({
  transform: `perspective(1200px) rotateX(${panelTilt.x}deg) rotateY(${panelTilt.y}deg) translate3d(${panelTilt.y * 0.8}px, ${panelTilt.x * -0.6}px, 0)`
}));

const handleLogin = async () => {
  if (!form.username || !form.password) {
    tip.value = "请输入用户名和密码";
    isError.value = true;
    return;
  }
  submitting.value = true;
  isError.value = false;
  try {
    const token = await authStore.login({ username: form.username, password: form.password });
    if (token) {
      tip.value = "登录成功，正在跳转...";
      const redirect = route.query.redirect || "/";
      setTimeout(() => router.push(redirect), 400);
      return;
    }
    tip.value = "登录响应成功，但未返回 token，请核对后端字段。";
    isError.value = true;
  } catch {
    tip.value = "登录失败，请检查账号密码或后端服务。";
    isError.value = true;
  } finally {
    submitting.value = false;
  }
};
</script>

<style scoped>
.login-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1.08fr 0.92fr;
  max-width: 1190px;
  margin: 0 auto;
  padding: 28px 0 48px;
  gap: 20px;
  background: transparent;
}

.login-scene {
  position: relative;
  overflow: hidden;
  padding: 48px clamp(28px, 5vw, 60px);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  border-radius: 20px;
  background: linear-gradient(135deg, #ff9f1c 0%, #ff5000 100%);
  color: #fff;
}

.scene-copy {
  position: relative;
  z-index: 2;
  max-width: 520px;
}

.scene-kicker {
  margin: 0 0 16px;
  color: rgba(255, 255, 255, 0.82);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 2px;
}

.scene-copy h1 {
  margin: 0;
  font: 700 clamp(40px, 5vw, 62px) / 1.05 var(--font-display);
  color: #fff;
  text-wrap: balance;
}

.scene-desc {
  margin: 22px 0 0;
  max-width: 420px;
  color: rgba(255, 255, 255, 0.88);
  font-size: 16px;
  line-height: 1.8;
}

.scene-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 28px;
}

.scene-tags span {
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.24);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
}

.artboard {
  position: relative;
  height: 420px;
  margin-top: 30px;
  transform-style: preserve-3d;
  transition: transform 0.16s ease-out;
  opacity: 0.92;
}

.orbit {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.18);
  animation: rotateOrbit 16s linear infinite;
}

.orbit-a {
  width: 360px;
  height: 360px;
  left: 14%;
  top: 24px;
}

.orbit-b {
  width: 440px;
  height: 440px;
  left: 4%;
  top: -12px;
  animation-direction: reverse;
  animation-duration: 24s;
}

.shape {
  position: absolute;
  bottom: 0;
  box-shadow: 0 18px 30px rgba(120, 32, 0, 0.16);
  transform-style: preserve-3d;
}

.tower {
  width: 140px;
  border-radius: 24px;
}

.dome {
  border-radius: 110px 110px 0 0;
}

.violet {
  left: 22%;
  height: 300px;
  background: linear-gradient(180deg, #ffd67c 0%, #ffbb34 100%);
  transform: rotate(3deg) translateZ(28px);
  animation: floatYViolet 5.2s ease-in-out infinite;
}

.dark {
  left: 46%;
  height: 212px;
  width: 124px;
  background: linear-gradient(180deg, #ff8d45 0%, #ff5a1f 100%);
  transform: translateZ(14px);
  animation: floatYDark 4.6s ease-in-out infinite 0.8s;
}

.coral {
  left: 12%;
  width: 240px;
  height: 186px;
  background: linear-gradient(180deg, #fff0c4 0%, #ffd46e 100%);
  transform: translateZ(36px);
  animation: floatYCoral 5.8s ease-in-out infinite 0.5s;
}

.yellow {
  left: 58%;
  width: 156px;
  height: 214px;
  background: linear-gradient(180deg, #fff 0%, #ffe1bf 100%);
  transform: translateZ(48px);
  animation: floatYYellow 5s ease-in-out infinite 1.2s;
}

.eye,
.mouth {
  position: absolute;
  background: #8d3e0f;
}

.eye {
  top: 42px;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.eye:first-child {
  left: 46px;
}

.eye:last-child {
  left: 84px;
}

.eye-line {
  width: 18px;
  height: 3px;
  border-radius: 999px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pupil {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #fff;
  transition: transform 0.12s ease-out;
}

.line-pupil {
  width: 9px;
  height: 2px;
  border-radius: 999px;
  background: #fff;
  transition: transform 0.12s ease-out;
}

.mouth {
  left: 46px;
  top: 108px;
  width: 66px;
  height: 4px;
  border-radius: 999px;
}

.login-panel {
  display: grid;
  align-items: center;
  justify-items: center;
  padding: 12px 0;
}

.panel-head {
  display: none;
}

.login {
  width: min(460px, 100%);
  padding: 36px 34px 30px;
  display: grid;
  gap: 18px;
  background: #fff;
  transform-style: preserve-3d;
  transition: transform 0.14s ease-out, box-shadow 0.18s ease-out;
  border-radius: 20px;
}

.login-header {
  display: grid;
  gap: 8px;
}

.welcome {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 1.8px;
  color: var(--brand);
}

.login-header h2 {
  margin: 0;
  font: 700 clamp(30px, 3.2vw, 38px) / 1.1 var(--font-display);
  color: var(--text-main);
}

.subtitle {
  margin: 0;
  color: var(--text-sub);
  font-size: 14px;
}

.field-group {
  display: grid;
  gap: 8px;
}

.field-group label {
  color: var(--text-main);
  font-size: 13px;
  font-weight: 700;
}

.field-group input {
  height: 48px;
  padding: 0 16px;
}

.login-btn {
  margin-top: 6px;
  min-height: 48px;
  font-size: 15px;
}

.tip {
  margin: 0;
  min-height: 20px;
  color: var(--text-sub);
  font-size: 13px;
}

.tip.error {
  color: #c0392b;
}

.tip.success {
  color: #1f8f63;
}

.switch {
  margin: 0;
  text-align: center;
  color: var(--text-sub);
  font-size: 13px;
}

.switch a {
  color: var(--brand);
  font-weight: 700;
}

.switch a:hover {
  text-decoration: underline;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@keyframes floatYViolet {
  0%,
  100% {
    transform: rotate(3deg) translateY(0) translateZ(28px);
  }
  50% {
    transform: rotate(3deg) translateY(-10px) translateZ(28px);
  }
}

@keyframes floatYDark {
  0%,
  100% {
    transform: translateY(0) translateZ(14px);
  }
  50% {
    transform: translateY(-10px) translateZ(14px);
  }
}

@keyframes floatYCoral {
  0%,
  100% {
    transform: translateY(0) translateZ(36px);
  }
  50% {
    transform: translateY(-10px) translateZ(36px);
  }
}

@keyframes floatYYellow {
  0%,
  100% {
    transform: translateY(0) translateZ(48px);
  }
  50% {
    transform: translateY(-10px) translateZ(48px);
  }
}

@keyframes rotateOrbit {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 1080px) {
  .login-shell {
    grid-template-columns: 1fr;
    padding: 20px 0 36px;
  }

  .login-scene {
    min-height: 420px;
  }

  .artboard {
    height: 320px;
  }

  .orbit-a {
    width: 300px;
    height: 300px;
  }

  .orbit-b {
    width: 360px;
    height: 360px;
  }
}

@media (max-width: 680px) {
  .login-shell {
    min-height: 100vh;
    gap: 14px;
  }

  .login-scene {
    padding: 28px 18px 18px;
    min-height: 360px;
  }

  .scene-copy h1 {
    font-size: 38px;
  }

  .scene-desc {
    font-size: 14px;
    line-height: 1.7;
  }

  .scene-tags {
    margin-top: 18px;
  }

  .artboard {
    height: 250px;
  }

  .violet {
    left: 20%;
    width: 102px;
    height: 210px;
  }

  .dark {
    left: 46%;
    width: 92px;
    height: 154px;
  }

  .coral {
    left: 10%;
    width: 176px;
    height: 132px;
  }

  .yellow {
    left: 58%;
    width: 112px;
    height: 158px;
  }

  .eye {
    top: 32px;
  }

  .eye:first-child {
    left: 32px;
  }

  .eye:last-child {
    left: 60px;
  }

  .mouth {
    left: 30px;
    top: 86px;
    width: 50px;
  }

  .login-panel {
    padding: 0 0 24px;
  }

  .login {
    padding: 24px 18px 20px;
    border-radius: 16px;
  }
}
</style>
