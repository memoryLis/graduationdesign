<template>
  <section class="admin-orders fade-up">
    <h2>订单管理</h2>

    <div class="card actions-bar">
      <span class="tip">共 {{ total }} 条订单</span>
      <button class="ghost" @click="loadOrders" :disabled="loading">刷新</button>
    </div>

    <p class="loading" v-if="loading">加载中...</p>
    <p class="error" v-else-if="error">{{ error }}</p>

    <div class="card list" v-else-if="orders.length">
      <article class="row header-row">
        <div>订单号</div>
        <div>用户ID</div>
        <div>金额</div>
        <div>订单状态</div>
        <div>支付时间</div>
        <div>操作</div>
      </article>

      <template v-for="order in orders" :key="order.id">
        <article class="row data-row" @click="toggleOrder(order)">
          <div class="mono">{{ order.bizOrderNo }}</div>
          <div>{{ order.bizUserId }}</div>
          <div class="price">￥{{ formatMoney(order.amount) }}</div>
          <div>
            <span class="status" :class="statusClass(order)">{{ orderStatusText(detailState(order.id).data?.orderStatus) }}</span>
          </div>
          <div>{{ formatTime(detailState(order.id).data?.payTime || order.createTime) }}</div>
          <div class="action-cell">
            <button class="btn-edit" type="button" @click.stop="toggleOrder(order)">
              {{ expandedOrderId === order.id ? '收起' : '查看' }}
            </button>
            <button
              v-if="canShip(order)"
              class="btn-ship"
              type="button"
              @click.stop="openShipModal(order)"
            >
              发货
            </button>
          </div>
        </article>

        <div class="detail-panel" v-if="expandedOrderId === order.id">
          <p class="loading" v-if="detailState(order.id).loading">加载中...</p>
          <template v-else-if="detailState(order.id).data">
            <div class="detail-meta">
              <span>订单状态：{{ orderStatusText(detailState(order.id).data.orderStatus) }}</span>
              <span>支付方式：{{ paymentTypeText(detailState(order.id).data.paymentType) }}</span>
              <span>支付时间：{{ formatTime(detailState(order.id).data.payTime) }}</span>
              <span>订单金额：￥{{ formatMoney(detailState(order.id).data.totalFee) }}</span>
            </div>
            <div v-if="detailState(order.id).data.address" class="address-card">
              <strong>{{ detailState(order.id).data.address.contact }}</strong>
              <span>{{ detailState(order.id).data.address.mobile }}</span>
              <p>{{ formatAddress(detailState(order.id).data.address) }}</p>
            </div>
            <div class="item-list" v-if="detailState(order.id).data.details.length">
              <article class="item-row" v-for="item in detailState(order.id).data.details" :key="item.id || item.itemId">
                <img :src="getImageUrl(item.image)" :alt="item.name" @error="onImgError" />
                <div class="item-info">
                  <p class="item-name">{{ item.name }}</p>
                  <p class="item-spec" v-if="item.spec">规格：{{ item.spec }}</p>
                </div>
                <div class="item-num">x{{ item.num }}</div>
                <div class="item-price">￥{{ formatMoney(item.price) }}</div>
              </article>
            </div>
          </template>
        </div>
      </template>
    </div>

    <p class="empty" v-else>暂无订单</p>

    <div class="pagination" v-if="total > 0">
      <button class="ghost" :disabled="pageNo <= 1 || loading" @click="changePage(pageNo - 1)">上一页</button>
      <span>第 {{ pageNo }} 页 / 共 {{ totalPages }} 页</span>
      <button class="ghost" :disabled="pageNo >= totalPages || loading" @click="changePage(pageNo + 1)">下一页</button>
    </div>

    <div class="modal-mask" v-if="showShipModal" @click="closeShipModal">
      <div class="modal-box card scale-in" @click.stop>
        <div class="modal-head">
          <h4>订单发货</h4>
          <button class="icon-close" @click="closeShipModal">×</button>
        </div>
        <p class="form-error" v-if="shipError">{{ shipError }}</p>
        <div class="modal-body">
          <div class="form-grid">
            <div class="form-row form-row--span2">
              <label>订单号</label>
              <input :value="shipTarget?.bizOrderNo" disabled />
            </div>
            <div class="form-row form-row--span2">
              <label>物流公司 <span class="req">*</span></label>
              <input v-model="shipForm.logisticsCompany" placeholder="如：顺丰速运" />
            </div>
            <div class="form-row form-row--span2">
              <label>快递单号 <span class="req">*</span></label>
              <input v-model="shipForm.logisticsNumber" placeholder="请输入快递单号" />
            </div>
          </div>
        </div>
        <div class="modal-actions">
          <button class="ghost" @click="closeShipModal">取消</button>
          <button class="btn-primary" :disabled="shipSubmitting" @click="handleShip">
            {{ shipSubmitting ? '提交中...' : '确认发货' }}
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { queryPayOrderDetail } from "@/features/order/api/my-order.api";
import { queryAllPayOrders, shipOrder } from "@/features/admin/api/logistics.api";

const defaultImg = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='86' height='86'%3E%3Crect fill='%23f0e6d6' width='86' height='86'/%3E%3Ctext x='50%25' y='50%25' dominant-baseline='middle' text-anchor='middle' fill='%23ad4310' font-size='24'%3E%F0%9F%93%A6%3C/text%3E%3C/svg%3E";

const orders = ref([]);
const total = ref(0);
const totalPages = ref(0);
const pageNo = ref(1);
const pageSize = 10;
const loading = ref(false);
const error = ref("");
const expandedOrderId = ref(null);
const detailMap = ref({});

const showShipModal = ref(false);
const shipTarget = ref(null);
const shipForm = ref({ logisticsCompany: "", logisticsNumber: "" });
const shipError = ref("");
const shipSubmitting = ref(false);

const toKey = (id) => String(id);

const formatMoney = (cents) => (Number(cents || 0) / 100).toFixed(2);

const formatTime = (time) => {
  if (!time) return "-";
  const date = new Date(time);
  if (Number.isNaN(date.getTime())) return String(time);
  return date.toLocaleString("zh-CN", { hour12: false });
};

const orderStatusText = (status) => {
  const map = { 1: "未付款", 2: "已付款待发货", 3: "已发货", 4: "交易成功", 5: "已取消", 6: "已评价" };
  return map[Number(status)] || "未知";
};

const paymentTypeText = (type) => {
  const map = { 1: "支付宝", 2: "微信", 3: "余额支付" };
  return map[Number(type)] || "未知";
};

const statusClass = (order) => "";

const canShip = (order) => {
  const state = detailState(order.id);
  return state.data && state.data.orderStatus === 2;
};

const getImageUrl = (image) => {
  if (!image) return defaultImg;
  if (image.startsWith("http")) return image;
  if (image.startsWith("/uploads/")) return image;
  const filename = image.replace(/\\/g, "/").replace(/^static\/?/, "");
  return "/uploads/" + filename;
};

const onImgError = (e) => { e.target.src = defaultImg; };

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
    detailMap.value[key] = { loading: false, error: "", data: { ...(res?.data || res) } };
  } catch (e) {
    detailMap.value[key] = { loading: false, error: e?.message || "加载失败", data: null };
  }
};

const toggleOrder = async (order) => {
  if (expandedOrderId.value === order.id) {
    expandedOrderId.value = null;
    return;
  }
  expandedOrderId.value = order.id;
  const state = detailState(order.id);
  if (!state.data && !state.loading) {
    await loadOrderDetail(order.id);
  }
};

const loadOrders = async () => {
  loading.value = true;
  error.value = "";
  try {
    const res = await queryAllPayOrders(pageNo.value, pageSize);
    const data = res?.data || res || {};
    orders.value = data.list || [];
    total.value = data.total || 0;
    totalPages.value = data.pages || 0;
  } catch (e) {
    error.value = e?.message || "加载失败";
  } finally {
    loading.value = false;
  }
};

const changePage = (page) => {
  if (page < 1 || page === pageNo.value) return;
  pageNo.value = page;
  loadOrders();
};

const openShipModal = (order) => {
  shipTarget.value = order;
  shipForm.value = { logisticsCompany: "", logisticsNumber: "" };
  shipError.value = "";
  showShipModal.value = true;
};

const closeShipModal = () => {
  showShipModal.value = false;
  shipError.value = "";
};

const handleShip = async () => {
  shipError.value = "";
  if (!shipForm.value.logisticsCompany?.trim()) { shipError.value = "请输入物流公司"; return; }
  if (!shipForm.value.logisticsNumber?.trim()) { shipError.value = "请输入快递单号"; return; }
  shipSubmitting.value = true;
  try {
    await shipOrder(shipTarget.value.bizOrderNo, {
      logisticsCompany: shipForm.value.logisticsCompany.trim(),
      logisticsNumber: shipForm.value.logisticsNumber.trim()
    });
    alert("发货成功！");
    closeShipModal();
    detailMap.value = {};
    expandedOrderId.value = null;
    await loadOrders();
  } catch (e) {
    shipError.value = e?.message || e?.msg || "发货失败";
  } finally {
    shipSubmitting.value = false;
  }
};

onMounted(loadOrders);
</script>

<style scoped>
.admin-orders { margin-top: 24px; }
h2 { font: 700 30px var(--font-display); margin-bottom: 16px; }
.actions-bar { padding: 16px 18px; margin-bottom: 16px; display: flex; align-items: center; justify-content: space-between; }
.tip { color: var(--text-sub); font-size: 14px; }
.list { padding: 8px 18px; }
.row { display: grid; grid-template-columns: 2fr 1fr 1fr 1.2fr 1.5fr 1fr; gap: 12px; align-items: center; padding: 14px 0; border-bottom: 1px dashed var(--border); }
.header-row { font-weight: 700; border-bottom: 2px solid var(--border); }
.data-row { cursor: pointer; transition: background 0.2s ease; }
.data-row:hover { background: #fff7f2; }
.mono { font-family: ui-monospace, monospace; font-size: 13px; }
.price { font-weight: 700; color: var(--brand); }
.status { padding: 4px 10px; border-radius: 999px; font-size: 12px; border: 1px solid var(--border); color: var(--text-sub); background: #fff; }
.btn-edit { padding: 6px 12px; border-radius: 16px; border: 1px solid #ffd2bf; background: #fff7f2; cursor: pointer; color: var(--brand); font-size: 13px; }
.btn-edit:hover { background: #fff1e8; }
.btn-ship { padding: 6px 12px; border-radius: 16px; border: none; background: var(--bg-warm); color: #fff; cursor: pointer; font-weight: 600; font-size: 13px; }
.btn-ship:hover { box-shadow: 0 8px 16px rgba(255, 80, 0, 0.18); }
.action-cell { display: flex; gap: 8px; flex-wrap: wrap; }
.detail-panel { margin: 0 0 14px; border: 1px solid var(--border); border-radius: 12px; padding: 12px; background: #fffaf7; }
.detail-meta { display: flex; flex-wrap: wrap; gap: 14px; font-size: 13px; color: var(--text-sub); margin-bottom: 10px; }
.address-card { margin-bottom: 12px; padding: 12px; border: 1px solid var(--border); border-radius: 12px; background: #fff; }
.address-card p { margin: 6px 0 0; color: var(--text-sub); font-size: 13px; }
.item-list { display: flex; flex-direction: column; gap: 8px; }
.item-row { display: grid; grid-template-columns: 62px 1fr auto auto; gap: 10px; align-items: center; border: 1px solid var(--border); border-radius: 10px; padding: 8px; background: #fff; }
.item-row img { width: 62px; height: 62px; object-fit: cover; border-radius: 8px; background: #f0e6d6; }
.item-name { margin: 0; font-weight: 600; }
.item-spec { margin: 3px 0 0; font-size: 12px; color: var(--text-sub); }
.item-num, .item-price { font-size: 13px; color: var(--text-sub); white-space: nowrap; }
.empty, .loading, .error { color: var(--text-sub); padding: 16px; }
.error { color: #b42318; }
.pagination { display: flex; align-items: center; justify-content: center; gap: 16px; margin-top: 16px; padding: 12px; color: var(--text-sub); font-size: 14px; }
.ghost { padding: 8px 16px; border-radius: 18px; border: 1px solid #ffd2bf; color: var(--brand); background: #fff7f2; cursor: pointer; font: inherit; }
.ghost:hover { background: #fff1e8; }
button:disabled { opacity: 0.6; cursor: not-allowed; }

.modal-mask { position: fixed; inset: 0; display: flex; align-items: center; justify-content: center; z-index: 120; padding: 22px; background: rgba(60, 60, 60, 0.38); }
.modal-box { width: min(560px, 96vw); padding: 0; overflow: hidden; border-radius: 18px; }
.modal-head { display: flex; align-items: flex-start; justify-content: space-between; padding: 24px 28px 16px; border-bottom: 1px solid var(--border); }
.modal-head h4 { margin: 0; font-size: 20px; }
.icon-close { width: 36px; height: 36px; border: 1px solid var(--border); border-radius: 10px; background: #fff; color: var(--text-sub); font-size: 24px; cursor: pointer; }
.icon-close:hover { color: var(--brand); border-color: #ffcfb8; background: #fff7f2; }
.form-error { margin: 14px 28px 0; border: 1px solid rgba(239, 68, 68, 0.24); border-radius: 12px; padding: 11px 14px; color: #b42318; background: rgba(254, 242, 242, 0.88); font-size: 13px; }
.modal-body { padding: 18px 28px 10px; overflow-y: auto; }
.form-grid { display: grid; gap: 14px; }
.form-row { display: flex; flex-direction: column; gap: 7px; }
.form-row label { font-size: 14px; font-weight: 600; color: var(--text-sub); }
.form-row input { padding: 11px 12px; border: 1px solid var(--border); border-radius: 10px; font-size: 14px; outline: none; }
.form-row input:focus { border-color: var(--brand); box-shadow: 0 0 0 4px rgba(255, 80, 0, 0.12); }
.req { color: #e5484d; }
.modal-actions { display: flex; justify-content: flex-end; gap: 10px; padding: 16px 28px 22px; border-top: 1px solid var(--border); background: #fffaf7; }

@media (max-width: 900px) {
  .row { grid-template-columns: 1fr; gap: 6px; }
  .header-row { display: none; }
}
</style>
