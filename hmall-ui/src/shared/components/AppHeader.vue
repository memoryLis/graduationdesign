<template>
  <header class="header-wrap">
    <div class="container header-row">
      <RouterLink class="logo" to="/">
        <span class="logo-icon">🏪</span>
        <span>小楼商城</span>
      </RouterLink>

      <nav class="nav-links">
        <RouterLink to="/">首页</RouterLink>
        <RouterLink to="/search">发现好物</RouterLink>
        <RouterLink to="/cart">购物车</RouterLink>
        <RouterLink v-if="authStore.isLoggedIn" to="/profile">个人信息</RouterLink>
        <RouterLink v-if="authStore.isLoggedIn" to="/my-orders">我的订单</RouterLink>
        <RouterLink v-if="authStore.isLoggedIn" to="/favorites">我的收藏</RouterLink>
      </nav>

      <div class="right-zone" v-if="authStore.isLoggedIn">
        <RouterLink v-if="isAdmin" class="admin-link-right" to="/admin/items">⚙</RouterLink>
        <RouterLink v-if="isAdmin" class="admin-link-right" to="/admin/users">👥</RouterLink>
        <RouterLink v-if="isAdmin" class="admin-link-right" to="/admin/orders">📦</RouterLink>
        <RouterLink class="profile-chip" to="/profile">
          <span class="avatar-text">{{ avatarText }}</span>
          <span class="profile-link">{{ authStore.user?.username || '用户' }}</span>
        </RouterLink>
        <button class="ghost" @click="handleLogout">退出</button>
      </div>

      <div class="right-zone" v-else>
        <RouterLink class="ghost" to="/login">登录</RouterLink>
        <RouterLink class="btn-primary btn-sm" to="/register">注册</RouterLink>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "@/features/auth/store/auth.store";
import { isAdminUser } from "@/shared/utils";

const authStore = useAuthStore();
const router = useRouter();

const isAdmin = computed(() => {
  return isAdminUser(authStore.user);
});

const avatarText = computed(() => {
  const username = String(authStore.user?.username || "用户").trim();
  if (!username) return "用户";
  if (/^[A-Za-z0-9]/.test(username)) {
    return username.slice(0, 1).toUpperCase();
  }
  return username.slice(0, 2);
});

const handleLogout = () => {
  authStore.logout();
  router.push("/");
};
</script>

<style scoped>
.header-wrap {
  position: sticky;
  top: 0;
  z-index: 30;
  background: rgba(255, 255, 255, 0.96);
  border-bottom: 1px solid var(--border);
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);
}

.header-wrap::before {
  content: "";
  position: absolute;
  inset: 0 0 auto;
  height: 4px;
  background: linear-gradient(90deg, #ff9000 0%, #ff5000 100%);
}

.header-row {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 20px;
  min-height: 78px;
}

.logo {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font: 700 clamp(26px, 2.4vw, 30px) / 1 var(--font-display);
  color: var(--brand);
  white-space: nowrap;
}

.logo-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 12px;
  background: var(--bg-warm);
  color: #fff;
  font-size: 20px;
}

.nav-links {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  gap: 4px;
  min-width: 0;
}

.nav-links a {
  position: relative;
  padding: 10px 14px;
  color: var(--text-sub);
  font-weight: 600;
  white-space: nowrap;
  transition: color 0.2s ease, background 0.2s ease;
}

.nav-links a:hover {
  color: var(--brand);
  background: #fff7f2;
  border-radius: 20px;
}

.nav-links .router-link-active {
  color: var(--brand);
  font-weight: 700;
  background: #fff2e8;
  border-radius: 20px;
}

.nav-links .router-link-active::after {
  content: "";
  position: absolute;
  left: 14px;
  right: 14px;
  bottom: 6px;
  height: 2px;
  border-radius: 999px;
  background: var(--brand);
}

.admin-link-right {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: linear-gradient(135deg, #fef9e7 0%, #fdf3d0 100%);
  border: 1px solid #e8d5a3;
  font-size: 15px;
  text-decoration: none;
  transition: all 0.25s ease;
  cursor: pointer;
  position: relative;
}

.admin-link-right:hover {
  background: linear-gradient(135deg, #fef3c7 0%, #fbebaf 100%);
  border-color: #c9a96e;
  box-shadow: 0 2px 12px rgba(201, 169, 110, 0.35);
  transform: translateY(-1px);
}

.admin-link-right.router-link-active {
  background: linear-gradient(135deg, #f9e8a0 0%, #f5db85 100%);
  border-color: #b8943d;
  box-shadow: 0 0 0 3px rgba(201, 169, 110, 0.22);
}

.right-zone {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.profile-chip {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 5px 10px 5px 5px;
  border-radius: 22px;
  background: #fffaf7;
  border: 1px solid #ffd9c7;
  transition: background 0.2s ease, border-color 0.2s ease;
}

.profile-chip:hover,
.profile-chip.router-link-active {
  background: #fff3eb;
  border-color: #ffbf9e;
}

.avatar-text {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-warm);
  color: #fff;
  font-size: 13px;
  font-weight: 800;
}

.profile-link {
  color: var(--text-main);
  font-weight: 700;
  font-size: 14px;
}

.ghost {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 38px;
  padding: 0 16px;
  border-radius: 20px;
  border: 1px solid #ffcfb8;
  color: var(--brand);
  background: #fff7f2;
  cursor: pointer;
  font: inherit;
  font-size: 14px;
  font-weight: 600;
  transition: background 0.2s ease, border-color 0.2s ease;
}

.ghost:hover {
  background: #fff1e8;
  border-color: #ffb98e;
}

.btn-sm {
  min-height: 38px;
  padding: 0 16px;
  font-size: 14px;
}

@media (max-width: 1024px) {
  .header-row {
    gap: 14px;
  }

  .nav-links {
    gap: 6px;
    overflow-x: auto;
    scrollbar-width: none;
  }

  .nav-links::-webkit-scrollbar {
    display: none;
  }
}

@media (max-width: 760px) {
  .header-row {
    grid-template-columns: 1fr auto;
    grid-template-areas:
      "logo right"
      "nav nav";
    min-height: auto;
    padding: 12px 0 10px;
    gap: 10px;
  }

  .logo {
    grid-area: logo;
    font-size: 24px;
  }

  .logo-icon {
    width: 34px;
    height: 34px;
    font-size: 18px;
  }

  .right-zone {
    grid-area: right;
    justify-self: end;
  }

  .nav-links {
    grid-area: nav;
    justify-content: flex-start;
    overflow-x: auto;
    padding-bottom: 2px;
    scrollbar-width: thin;
  }

  .nav-links::-webkit-scrollbar {
    height: 5px;
  }

  .nav-links::-webkit-scrollbar-thumb {
    border-radius: 999px;
    background: rgba(255, 80, 0, 0.24);
  }

  .right-zone .ghost,
  .right-zone .btn-primary {
    min-height: 34px;
    padding: 0 13px;
  }

  .profile-chip {
    padding-right: 6px;
  }

  .avatar-text {
    width: 30px;
    height: 30px;
    font-size: 12px;
  }
}
</style>
