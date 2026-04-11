<template>
  <section class="user-manage fade-up">
    <h2>用户管理</h2>

    <div class="card actions-bar">
      <div class="search-box">
        <input v-model.trim="keyword" placeholder="按用户名或手机号搜索" @keyup.enter="handleSearch" />
        <button class="btn-search" @click="handleSearch">搜索</button>
        <button v-if="keyword" class="ghost" @click="handleReset">重置</button>
      </div>
    </div>

    <div v-if="users.length" class="card list">
      <article class="row header-row">
        <div>用户</div>
        <div>手机号</div>
        <div>状态</div>
        <div>积分</div>
        <div>注册时间</div>
        <div>操作</div>
      </article>

      <article v-for="user in users" :key="user.id" class="row">
        <div class="user-cell">
          <strong>{{ user.username }}</strong>
          <span>ID: {{ user.id }}</span>
        </div>
        <div>{{ user.phone || "-" }}</div>
        <div>
          <span :class="['status-badge', user.status === 1 ? 'is-normal' : 'is-frozen']">
            {{ user.statusText || (user.status === 1 ? "正常" : "冻结") }}
          </span>
        </div>
        <div>{{ user.points ?? 0 }}</div>
        <div>{{ formatDate(user.createTime) }}</div>
        <div class="actions">
          <button class="btn-points" @click="openRechargeDialog(user)">充值积分</button>
          <button :class="user.status === 1 ? 'btn-freeze' : 'btn-unfreeze'" @click="handleToggleStatus(user)">
            {{ user.status === 1 ? "冻结" : "解冻" }}
          </button>
          <button class="btn-delete" @click="handleDelete(user)">删除</button>
        </div>
      </article>
    </div>

    <p v-else class="empty">暂无用户数据</p>

    <div v-if="total > 0" class="pagination">
      <button class="ghost" :disabled="pageNo <= 1 || loading" @click="changePage(pageNo - 1)">上一页</button>
      <span>第 {{ pageNo }} 页 / 共 {{ totalPages || 1 }} 页（{{ total }} 人）</span>
      <button class="ghost" :disabled="pageNo >= totalPages || loading" @click="changePage(pageNo + 1)">下一页</button>
    </div>

    <div v-if="showRechargeDialog" class="modal" @click.self="closeRechargeDialog">
      <div class="modal-content card">
        <h3>给 {{ currentUser?.username }} 充值积分</h3>
        <div class="form-group">
          <label>当前积分</label>
          <div class="readonly">{{ currentUser?.points ?? 0 }}</div>
        </div>
        <div class="form-group">
          <label>充值积分</label>
          <input v-model.number="rechargeForm.amount" type="number" min="1" placeholder="请输入积分数量" />
        </div>
        <div class="form-group">
          <label>备注</label>
          <textarea v-model.trim="rechargeForm.remark" rows="3" maxlength="100" placeholder="可选，默认显示为管理员充值"></textarea>
        </div>
        <div class="form-actions">
          <button class="ghost" @click="closeRechargeDialog">取消</button>
          <button class="btn-primary" :disabled="submitting" @click="handleRecharge">
            {{ submitting ? "提交中..." : "确认充值" }}
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import {
  deleteUser,
  freezeUser,
  getUserList,
  rechargeUserPoints,
  unfreezeUser
} from "@/features/admin/api/user-manage.api";

const users = ref([]);
const total = ref(0);
const totalPages = ref(0);
const pageNo = ref(1);
const pageSize = 10;
const keyword = ref("");
const loading = ref(false);
const submitting = ref(false);
const showRechargeDialog = ref(false);
const currentUser = ref(null);
const rechargeForm = reactive({
  amount: 0,
  remark: ""
});

const formatDate = (value) => {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  }).format(date);
};

const loadUsers = async () => {
  loading.value = true;
  try {
    const res = await getUserList(pageNo.value, pageSize, keyword.value);
    const data = res?.data || res || {};
    users.value = data.list || [];
    total.value = data.total || 0;
    totalPages.value = data.pages || 0;
  } catch (error) {
    console.error("加载用户列表失败", error);
    users.value = [];
    total.value = 0;
    totalPages.value = 0;
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  pageNo.value = 1;
  loadUsers();
};

const handleReset = () => {
  keyword.value = "";
  pageNo.value = 1;
  loadUsers();
};

const changePage = (page) => {
  if (page < 1 || (totalPages.value && page > totalPages.value)) return;
  pageNo.value = page;
  loadUsers();
};

const handleToggleStatus = async (user) => {
  const actionText = user.status === 1 ? "冻结" : "解冻";
  if (!window.confirm(`确认要${actionText}用户“${user.username}”吗？`)) return;
  try {
    if (user.status === 1) {
      await freezeUser(user.id);
    } else {
      await unfreezeUser(user.id);
    }
    await loadUsers();
  } catch (error) {
    console.error(`${actionText}用户失败`, error);
    alert(error?.message || error?.msg || `${actionText}用户失败`);
  }
};

const handleDelete = async (user) => {
  if (!window.confirm(`确认删除用户“${user.username}”吗？删除后不可恢复。`)) return;
  try {
    await deleteUser(user.id);
    if (users.value.length === 1 && pageNo.value > 1) {
      pageNo.value -= 1;
    }
    await loadUsers();
  } catch (error) {
    console.error("删除用户失败", error);
    alert(error?.message || error?.msg || "删除用户失败");
  }
};

const openRechargeDialog = (user) => {
  currentUser.value = user;
  rechargeForm.amount = 0;
  rechargeForm.remark = "";
  showRechargeDialog.value = true;
};

const closeRechargeDialog = () => {
  showRechargeDialog.value = false;
  currentUser.value = null;
  rechargeForm.amount = 0;
  rechargeForm.remark = "";
};

const handleRecharge = async () => {
  if (!currentUser.value) return;
  if (!Number.isInteger(rechargeForm.amount) || rechargeForm.amount <= 0) {
    alert("请输入大于 0 的整数积分");
    return;
  }
  submitting.value = true;
  try {
    await rechargeUserPoints(currentUser.value.id, {
      amount: rechargeForm.amount,
      remark: rechargeForm.remark
    });
    closeRechargeDialog();
    await loadUsers();
  } catch (error) {
    console.error("充值积分失败", error);
    alert(error?.message || error?.msg || "充值积分失败");
  } finally {
    submitting.value = false;
  }
};

onMounted(() => {
  loadUsers();
});
</script>

<style scoped>
.user-manage {
  margin-top: 24px;
}

h2 {
  margin-bottom: 16px;
  font: 700 30px var(--font-display);
}

.actions-bar {
  padding: 16px 18px;
  margin-bottom: 16px;
}

.search-box {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.search-box input {
  width: min(320px, 100%);
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  outline: none;
}

.search-box input:focus,
.form-group input:focus,
.form-group textarea:focus {
  border-color: var(--brand);
  box-shadow: 0 0 0 3px rgba(255, 80, 0, 0.12);
}

.btn-search {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  background: var(--brand);
  color: #fff;
  cursor: pointer;
  font: inherit;
}

.list {
  padding: 8px 18px;
}

.row {
  display: grid;
  grid-template-columns: 1.2fr 1fr 0.8fr 0.8fr 1.2fr 1.8fr;
  gap: 12px;
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px dashed var(--border);
}

.header-row {
  border-bottom: 2px solid var(--border);
  font-weight: 700;
}

.row:last-child {
  border-bottom: none;
}

.user-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.user-cell span {
  color: var(--text-sub);
  font-size: 12px;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 64px;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.is-normal {
  background: rgba(39, 174, 96, 0.12);
  color: #1f8f63;
}

.is-frozen {
  background: #fff1e8;
  color: #b44a18;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.actions button,
.ghost {
  padding: 6px 12px;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: #fff7f2;
  cursor: pointer;
  font: inherit;
  transition: all 0.2s ease;
}

.btn-points {
  border-color: #156ca9;
  color: #156ca9;
}

.btn-freeze,
.btn-delete {
  border-color: #c0392b;
  color: #c0392b;
}

.btn-unfreeze {
  border-color: #1f8f63;
  color: #1f8f63;
}

.actions button:hover,
.ghost:hover {
  background: #fff1e8;
}

.empty {
  padding: 40px;
  color: var(--text-sub);
  text-align: center;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 16px;
  padding: 12px;
  color: var(--text-sub);
  font-size: 14px;
}

.modal {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.5);
}

.modal-content {
  width: min(460px, 92vw);
  padding: 24px;
}

.modal-content h3 {
  margin: 0 0 18px;
  font: 700 24px var(--font-display);
}

.form-group {
  display: grid;
  gap: 8px;
  margin-bottom: 16px;
}

.form-group label {
  font-size: 14px;
  font-weight: 600;
}

.form-group input,
.form-group textarea,
.readonly {
  width: 100%;
  box-sizing: border-box;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: #fff;
  outline: none;
  font: inherit;
}

.readonly {
  background: rgba(248, 250, 252, 0.9);
}

.form-group textarea {
  resize: vertical;
  min-height: 88px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 1100px) {
  .row {
    grid-template-columns: 1fr;
  }

  .header-row {
    display: none;
  }
}
</style>
