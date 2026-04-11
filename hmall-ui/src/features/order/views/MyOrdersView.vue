<template>
  <section class="my-order fade-up">
    <h2>我的订单</h2>

    <div class="card actions-bar">
      <div class="tip">
        共 {{ total }} 条订单，点击某一行可查看商品明细。
      </div>
      <button class="ghost" @click="loadOrders" :disabled="loading">刷新</button>
    </div>

    <p class="loading" v-if="loading">订单加载中...</p>
    <p class="error" v-else-if="error">{{ error }}</p>

    <div class="card list" v-else-if="orders.length">
      <article class="row header-row">
        <div>业务订单号</div>
        <div>支付单号</div>
        <div>金额</div>
        <div>支付状态</div>
        <div>创建时间</div>
        <div>操作</div>
      </article>

      <template v-for="order in orders" :key="order.id">
        <article class="row data-row" @click="toggleOrder(order.id)">
          <div class="mono">{{ order.bizOrderNo }}</div>
          <div class="mono">{{ order.payOrderNo || '-' }}</div>
          <div class="price">￥{{ formatMoney(order.amount) }}</div>
          <div>
            <span class="status" :class="statusClass(order)">{{ payStatusText(order) }}</span>
          </div>
          <div>{{ formatTime(order.createTime) }}</div>
          <div class="action-cell">
            <button class="btn-edit" type="button" @click.stop="toggleOrder(order.id)">
              {{ expandedOrderId === order.id ? '收起' : '查看商品' }}
            </button>
            <button
              v-if="canRepay(order)"
              class="btn-pay"
              type="button"
              @click.stop="goPay(order)"
            >
              去支付
            </button>
            <span v-else-if="isExpiredOrder(order)" class="expired-text">订单已过期</span>
          </div>
        </article>

        <div class="detail-panel" v-if="expandedOrderId === order.id">
          <p class="loading" v-if="detailState(order.id).loading">明细加载中...</p>
          <p class="error" v-else-if="detailState(order.id).error">{{ detailState(order.id).error }}</p>
          <template v-else-if="detailState(order.id).data">
            <div class="detail-meta">
              <span>订单状态：{{ orderStatusText(detailState(order.id).data.orderStatus) }}</span>
              <span>支付方式：{{ paymentTypeText(detailState(order.id).data.paymentType) }}</span>
              <span>支付时间：{{ formatTime(detailState(order.id).data.payTime) }}</span>
              <span>订单金额：￥{{ formatMoney(detailState(order.id).data.totalFee) }}</span>
            </div>

            <div v-if="detailState(order.id).data.address" class="address-card">
              <div class="address-top">
                <strong>{{ detailState(order.id).data.address.contact }}</strong>
                <span>{{ detailState(order.id).data.address.mobile }}</span>
              </div>
              <p class="address-line">
                {{ formatAddress(detailState(order.id).data.address) }}
              </p>
              <p class="address-note" v-if="detailState(order.id).data.address.notes">
                备注：{{ detailState(order.id).data.address.notes }}
              </p>
            </div>

            <div class="item-list" v-if="detailState(order.id).data.details.length">
              <article class="item-row" v-for="item in detailState(order.id).data.details" :key="`${order.id}-${item.id || item.itemId}`">
                <img :src="getImageUrl(item.image)" :alt="item.name" @error="onImgError($event)" />
                <div class="item-info">
                  <p class="item-name">{{ item.name }}</p>
                  <p class="item-spec" v-if="item.spec">规格：{{ item.spec }}</p>
                </div>
                <div class="item-num">x{{ item.num }}</div>
                <div class="item-price">￥{{ formatMoney(item.price) }}</div>
                <div class="item-total">￥{{ formatMoney(item.price * item.num) }}</div>
              </article>
            </div>
            <p class="empty-detail" v-else>该订单暂无商品明细</p>
          </template>
        </div>
      </template>
    </div>

    <p class="empty" v-else>暂无订单记录</p>

    <div class="pagination" v-if="total > 0">
      <button class="ghost" :disabled="pageNo <= 1 || loading" @click="changePage(pageNo - 1)">上一页</button>
      <span>第 {{ pageNo }} 页 / 共 {{ totalPages }} 页</span>
      <button class="ghost" :disabled="pageNo >= totalPages || loading" @click="changePage(pageNo + 1)">下一页</button>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { queryMyPayOrders, queryPayOrderDetail } from "@/features/order/api/my-order.api";

const defaultImg = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='86' height='86'%3E%3Crect fill='%23f0e6d6' width='86' height='86'/%3E%3Ctext x='50%25' y='50%25' dominant-baseline='middle' text-anchor='middle' fill='%23ad4310' font-size='24'%3E%F0%9F%93%A6%3C/text%3E%3C/svg%3E";
const router = useRouter();

const orders = ref([]);
const total = ref(0);
const totalPages = ref(0);
const pageNo = ref(1);
const pageSize = 10;

const loading = ref(false);
const error = ref("");
const expandedOrderId = ref(null);
const detailMap = ref({});

const toKey = (id) => String(id);

const formatMoney = (cents) => {
  const val = Number(cents || 0);
  return (val / 100).toFixed(2);
};

const formatTime = (time) => {
  if (!time) return "-";
  const date = new Date(time);
  if (Number.isNaN(date.getTime())) return String(time);
  return date.toLocaleString("zh-CN", { hour12: false });
};

const isExpiredOrder = (order) => {
  if (Number(order?.status) !== 1) return false;
  if (!order?.payOverTime) return false;
  const expiredAt = new Date(order.payOverTime);
  if (Number.isNaN(expiredAt.getTime())) return false;
  return expiredAt.getTime() <= Date.now();
};

const canRepay = (order) => {
  return Number(order?.status) === 1 && !isExpiredOrder(order);
};

const payStatusText = (order) => {
  if (isExpiredOrder(order)) return "订单已过期";
  const map = {
    0: "未提交",
    1: "待支付",
    2: "已关闭",
    3: "支付成功"
  };
  return map[Number(order?.status)] || "未知";
};

const statusClass = (order) => {
  if (isExpiredOrder(order)) return "status-close";
  const map = {
    1: "status-wait",
    2: "status-close",
    3: "status-success"
  };
  return map[Number(order?.status)] || "";
};

const orderStatusText = (status) => {
  const map = {
    1: "未付款",
    2: "已付款，待发货",
    3: "已发货，待确认",
    4: "交易成功",
    5: "交易取消",
    6: "交易结束"
  };
  return map[Number(status)] || "未知";
};

const paymentTypeText = (type) => {
  const map = {
    1: "支付宝",
    2: "微信",
    3: "余额支付"
  };
  return map[Number(type)] || "未知";
};

const getImageUrl = (image) => {
  if (!image) return defaultImg;
  if (image.startsWith("http")) return image;
  if (image.startsWith("/uploads/")) return image;
  const filename = image.replace(/\\/g, "/").replace(/^static\/?/, "");
  return "/uploads/" + filename;
};

const onImgError = (e) => {
  e.target.src = defaultImg;
};

const normalizeDetail = (data) => {
  const detail = data || {};
  return {
    ...detail,
    details: Array.isArray(detail.details) ? detail.details : []
  };
};

const formatAddress = (address) => {
  if (!address) return "-";
  return [address.province, address.city, address.town, address.street].filter(Boolean).join(" ");
};

const detailState = (orderId) => {
  const key = toKey(orderId);
  return detailMap.value[key] || { loading: false, error: "", data: null };
};

const loadOrderDetail = async (payOrderId) => {
  const key = toKey(payOrderId);
  detailMap.value[key] = { loading: true, error: "", data: null };
  try {
    const res = await queryPayOrderDetail(payOrderId);
    detailMap.value[key] = {
      loading: false,
      error: "",
      data: normalizeDetail(res)
    };
  } catch (e) {
    const msg = e?.message || e?.msg || "加载订单明细失败";
    detailMap.value[key] = { loading: false, error: msg, data: null };
  }
};

const toggleOrder = async (payOrderId) => {
  if (expandedOrderId.value === payOrderId) {
    expandedOrderId.value = null;
    return;
  }

  expandedOrderId.value = payOrderId;
  const state = detailState(payOrderId);
  if (!state.data && !state.loading) {
    await loadOrderDetail(payOrderId);
  }
};

const loadOrders = async () => {
  loading.value = true;
  error.value = "";
  try {
    const res = await queryMyPayOrders(pageNo.value, pageSize);
    const data = res?.data || res || {};
    orders.value = data.list || [];
    total.value = data.total || 0;
    totalPages.value = data.pages || 0;

    if (expandedOrderId.value && !orders.value.some((o) => o.id === expandedOrderId.value)) {
      expandedOrderId.value = null;
    }
  } catch (e) {
    console.error("加载订单失败", e);
    error.value = e?.message || e?.msg || "加载订单失败，请稍后重试";
  } finally {
    loading.value = false;
  }
};

const changePage = (page) => {
  if (page < 1 || page === pageNo.value) return;
  pageNo.value = page;
  loadOrders();
};

const goPay = (order) => {
  if (!canRepay(order)) return;
  router.push({
    path: "/pay",
    query: {
      orderId: order.bizOrderNo,
      amount: order.amount
    }
  });
};

onMounted(loadOrders);
</script>

<style scoped>
.my-order {
  margin-top: 24px;
}

h2 {
  font: 700 30px var(--font-display);
  margin-bottom: 16px;
}

.actions-bar {
  padding: 16px 18px;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.tip {
  color: var(--text-sub);
  font-size: 14px;
}

.list {
  padding: 8px 18px;
}

.row {
  display: grid;
  grid-template-columns: 2fr 2fr 1fr 1fr 1.7fr 1fr;
  gap: 12px;
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px dashed var(--border);
}

.header-row {
  font-weight: 700;
  color: var(--text-main);
  border-bottom: 2px solid var(--border);
}

.data-row {
  cursor: pointer;
  transition: background 0.2s ease;
}

.data-row:hover {
  background: #fff7f2;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
  font-size: 13px;
}

.price {
  font-weight: 700;
  color: var(--brand);
}

.status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 72px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  border: 1px solid var(--border);
  color: var(--text-sub);
  background: rgba(255, 255, 255, 0.72);
}

.status-wait {
  border-color: rgba(245, 158, 11, 0.35);
  color: #a65c00;
  background: rgba(254, 243, 199, 0.55);
}

.status-close {
  border-color: rgba(239, 68, 68, 0.3);
  color: #b42318;
  background: rgba(254, 226, 226, 0.58);
}

.status-success {
  border-color: rgba(16, 185, 129, 0.32);
  color: #0f766e;
  background: rgba(209, 250, 229, 0.58);
}

.btn-edit {
  padding: 6px 12px;
  border-radius: 16px;
  border: 1px solid #ffd2bf;
  background: #fff7f2;
  cursor: pointer;
  font: inherit;
  font-size: 13px;
  transition: all 0.2s ease;
  color: var(--brand);
}

.btn-edit:hover {
  background: #fff1e8;
}

.action-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.btn-pay {
  padding: 6px 12px;
  border-radius: 16px;
  border: none;
  background: var(--bg-warm);
  color: #fff;
  cursor: pointer;
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.btn-pay:hover {
  box-shadow: 0 10px 18px rgba(255, 80, 0, 0.18);
}

.expired-text {
  font-size: 13px;
  color: #b42318;
  font-weight: 600;
}

.detail-panel {
  margin: 0 0 14px;
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 12px;
  background: #fffaf7;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  font-size: 13px;
  color: var(--text-sub);
  margin-bottom: 10px;
}

.address-card {
  margin-bottom: 12px;
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.84);
}

.address-top {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}

.address-top strong {
  color: var(--text-main);
}

.address-top span {
  color: var(--text-sub);
  font-size: 13px;
}

.address-line,
.address-note {
  margin: 0;
  color: var(--text-sub);
  font-size: 13px;
  line-height: 1.7;
}

.address-note {
  margin-top: 4px;
}

.item-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.item-row {
  display: grid;
  grid-template-columns: 62px 1fr auto auto auto;
  gap: 10px;
  align-items: center;
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 8px;
  background: rgba(255, 255, 255, 0.88);
}

.item-row img {
  width: 62px;
  height: 62px;
  object-fit: cover;
  border-radius: 8px;
  background: #f0e6d6;
}

.item-name {
  margin: 0;
  font-weight: 600;
}

.item-spec {
  margin: 3px 0 0;
  font-size: 12px;
  color: var(--text-sub);
}

.item-num,
.item-price,
.item-total {
  font-size: 13px;
  color: var(--text-sub);
  white-space: nowrap;
}

.item-total {
  color: var(--brand);
  font-weight: 700;
}

.empty,
.loading,
.error,
.empty-detail {
  color: var(--text-sub);
  padding: 16px;
}

.error {
  color: #b42318;
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

.ghost {
  padding: 8px 16px;
  border-radius: 18px;
  border: 1px solid #ffd2bf;
  color: var(--brand);
  background: #fff7f2;
  cursor: pointer;
  font: inherit;
}

.ghost:hover {
  background: #fff1e8;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 900px) {
  .row {
    grid-template-columns: 1fr;
    gap: 6px;
    padding: 12px 0;
  }

  .header-row {
    display: none;
  }

  .data-row {
    border-bottom: 1px dashed var(--border);
  }

  .action-cell {
    justify-content: flex-start;
  }

  .item-row {
    grid-template-columns: 62px 1fr;
  }

  .item-num,
  .item-price,
  .item-total {
    justify-self: start;
  }
}
</style>
