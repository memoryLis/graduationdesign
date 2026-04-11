<template>
  <section class="order-confirm fade-up">
    <h2>确认订单</h2>

    <!-- 收货地址 -->
    <div class="card section-card">
      <h3>收货地址</h3>
      <p class="loading" v-if="loadingAddr">加载地址中...</p>
      <div v-else-if="addresses.length">
        <div
          v-for="addr in addresses"
          :key="addr.id"
          class="addr-item"
          :class="{ active: selectedAddrId === addr.id }"
          @click="selectedAddrId = addr.id"
        >
          <span class="contact">{{ addr.contact }} {{ addr.mobile }}</span>
          <span class="addr-text">{{ addr.province }}{{ addr.city }}{{ addr.town }}{{ addr.street }}</span>
          <span class="default-tag" v-if="addr.isDefault">默认</span>
        </div>
      </div>
      <p v-else class="no-addr">暂无收货地址，请先在后台添加。</p>
    </div>

    <!-- 商品清单 -->
    <div class="card section-card">
      <h3>商品清单</h3>
      <div class="item-row" v-for="item in orderItems" :key="item.itemId">
        <img :src="item.image || defaultImg" :alt="item.name" @error="onImgError($event)" />
        <div class="item-info">
          <p class="item-name">{{ item.name }}</p>
          <p class="item-spec" v-if="item.spec">{{ item.spec }}</p>
        </div>
        <span class="item-num">x{{ item.num }}</span>
        <span class="item-price">￥{{ (item.price * item.num).toFixed(2) }}</span>
      </div>
    </div>

    <!-- 汇总 -->
    <div class="card section-card summary">
      <div class="summary-row">
        <span>商品总数</span>
        <span>{{ totalNum }} 件</span>
      </div>
      <div class="summary-row total">
        <span>应付总额</span>
        <strong>￥{{ totalAmount.toFixed(2) }}</strong>
      </div>
      <button class="btn-primary submit-btn" :disabled="!selectedAddrId || submitting" @click="submitOrder">
        {{ submitting ? '提交中...' : '提交订单' }}
      </button>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useCartStore } from "@/features/cart/store/cart.store";
import { fetchAddressList } from "@/features/order/api/address.api";
import { createOrder } from "@/features/order/api/order.api";

const router = useRouter();
const cartStore = useCartStore();
const defaultImg = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='60' height='60'%3E%3Crect fill='%23f0e6d6' width='60' height='60'/%3E%3Ctext x='50%25' y='50%25' dominant-baseline='middle' text-anchor='middle' fill='%23ad4310' font-size='20'%3E📦%3C/text%3E%3C/svg%3E";

const addresses = ref([]);
const loadingAddr = ref(true);
const selectedAddrId = ref(null);
const submitting = ref(false);

// 从购物车获取选中的商品
const orderItems = computed(() => {
  return cartStore.selectedItems.map((i) => ({
    itemId: i.itemId || i.id,
    name: i.name,
    num: i.num,
    price: i.price,
    image: i.image,
    spec: i.spec || ""
  }));
});

const totalNum = computed(() => orderItems.value.reduce((s, i) => s + i.num, 0));
const totalAmount = computed(() => orderItems.value.reduce((s, i) => s + i.price * i.num, 0));

const onImgError = (e) => {
  e.target.src = defaultImg;
};

const submitOrder = async () => {
  if (!selectedAddrId.value) return;
  if (!orderItems.value.length) return;
  submitting.value = true;
  try {
    // 后端 OrderFormDTO: { addressId, paymentType, details: [{itemId, num}] }
    const orderId = await createOrder({
      addressId: selectedAddrId.value,
      paymentType: 3, // 余额支付
      details: orderItems.value.map((i) => ({ itemId: i.itemId, num: i.num }))
    });
    // 跳转支付页
    router.push({ path: "/pay", query: { orderId, amount: Math.round(totalAmount.value * 100) } });
  } catch (e) {
    alert("提交订单失败：" + (e?.msg || e?.message || "未知错误"));
  } finally {
    submitting.value = false;
  }
};

onMounted(async () => {
  // 如果没有选中商品，回到购物车
  if (!cartStore.selectedItems.length) {
    router.replace("/cart");
    return;
  }
  try {
    const result = await fetchAddressList();
    addresses.value = Array.isArray(result) ? result : (result?.data || []);
    // 默认选中第一个默认地址或第一个地址
    const defaultAddr = addresses.value.find((a) => a.isDefault);
    if (defaultAddr) selectedAddrId.value = defaultAddr.id;
    else if (addresses.value.length) selectedAddrId.value = addresses.value[0].id;
  } catch {
    addresses.value = [];
  } finally {
    loadingAddr.value = false;
  }
});
</script>

<style scoped>
.order-confirm {
  margin-top: 24px;
}

h2 {
  margin: 0 0 18px;
  font: 700 30px var(--font-display);
}

.section-card {
  padding: 20px;
  margin-bottom: 16px;
}

h3 {
  margin: 0 0 12px;
  font-size: 18px;
}

.addr-item {
  padding: 14px 16px;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  cursor: pointer;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fff;
}

.addr-item:hover {
  border-color: #ffd4c1;
  background: #fff8f4;
}

.addr-item.active {
  border-color: var(--brand);
  background: #fff4ed;
}

.contact {
  font-weight: 700;
  white-space: nowrap;
}

.addr-text {
  color: var(--text-sub);
  flex: 1;
}

.default-tag {
  background: var(--bg-warm);
  color: #fff;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
}

.no-addr {
  color: var(--text-sub);
}

.item-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #f3f3f3;
}

.item-row:last-child {
  border-bottom: none;
}

.item-row img {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  object-fit: cover;
  background: #fff7f2;
  border: 1px solid #f6ebe4;
}

.item-info {
  flex: 1;
}

.item-name {
  margin: 0;
  font-weight: 600;
}

.item-spec {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--text-sub);
}

.item-num {
  color: var(--text-sub);
}

.item-price {
  font-weight: 700;
  color: var(--brand);
  min-width: 80px;
  text-align: right;
}

.summary {
  text-align: right;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
}

.summary-row.total {
  border-top: 1px solid var(--border);
  padding-top: 12px;
  margin-top: 4px;
}

.summary-row.total strong {
  color: var(--brand);
  font-size: 24px;
}

.submit-btn {
  margin-top: 16px;
  width: 100%;
  min-height: 46px;
  padding: 0 14px;
  font-size: 16px;
}

.submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.loading {
  color: var(--text-sub);
}
</style>
