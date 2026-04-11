<template>
  <section class="pay-page fade-up">
    <div class="card pay-card">
      <h2>订单支付</h2>

      <div class="order-info">
        <p>订单号：{{ orderId }}</p>
        <p class="amount">应付金额：<strong>￥{{ displayAmount }}</strong></p>
      </div>

      <!-- 支付方式 -->
      <div class="pay-methods">
        <h3>选择支付方式</h3>
        <div
          class="method-item"
          :class="{ active: payMethod === 'balance' }"
          @click="payMethod = 'balance'"
        >
          <span>💰 余额支付</span>
        </div>
      </div>

      <!-- 余额支付需要输入密码 -->
      <div class="pw-section" v-if="payMethod === 'balance'">
        <label>请输入支付密码：</label>
        <input v-model="payPw" type="password" placeholder="支付密码" @keyup.enter="doPay" />
      </div>

      <button class="btn-primary pay-btn" :disabled="paying || !payMethod" @click="doPay">
        {{ paying ? '支付中...' : '确认支付' }}
      </button>

      <p class="error" v-if="errorMsg">{{ errorMsg }}</p>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { applyPayOrder, payByBalance } from "@/features/order/api/pay.api";
import { useCartStore } from "@/features/cart/store/cart.store";

const route = useRoute();
const router = useRouter();
const cartStore = useCartStore();

const orderId = ref("");
const amount = ref(0);
const payMethod = ref("balance");
const payPw = ref("");
const paying = ref(false);
const errorMsg = ref("");

const displayAmount = computed(() => (amount.value / 100).toFixed(2));

const doPay = async () => {
  if (!payMethod.value) return;
  if (payMethod.value === "balance" && !payPw.value) {
    errorMsg.value = "请输入支付密码";
    return;
  }
  paying.value = true;
  errorMsg.value = "";
  try {
    // 1. 创建支付单（bizOrderNo 保持字符串，不能用 Number() 否则大数丢精度）
    const payOrderId = await applyPayOrder({
      bizOrderNo: orderId.value,
      amount: amount.value,
      payChannelCode: "balance",
      payType: 5, // BALANCE
      orderInfo: "小楼商城订单支付"
    });

    // 2. 余额支付
    await payByBalance(payOrderId, payPw.value);

    // 3. 支付成功，刷新购物车并跳转
    cartStore.loadCart();
    router.push({ path: "/pay-success", query: { orderId: orderId.value, amount: amount.value } });
  } catch (e) {
    errorMsg.value = "支付失败：" + (e?.msg || e?.message || "请检查支付密码或余额");
  } finally {
    paying.value = false;
  }
};

onMounted(() => {
  orderId.value = route.query.orderId || "";
  amount.value = Number(route.query.amount) || 0;
  if (!orderId.value) {
    router.replace("/cart");
  }
});
</script>

<style scoped>
.pay-page {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

.pay-card {
  width: min(520px, 94vw);
  padding: 28px;
}

h2 {
  margin: 0 0 20px;
  font: 700 28px var(--font-display);
}

.order-info {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px dashed var(--border);
}

.order-info p {
  margin: 4px 0;
  color: var(--text-sub);
}

.amount strong {
  color: var(--brand-dark);
  font-size: 24px;
}

.pay-methods {
  margin-bottom: 20px;
}

h3 {
  margin: 0 0 10px;
  font-size: 16px;
}

.method-item {
  padding: 14px;
  border: 2px solid transparent;
  border-radius: 12px;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.5);
  margin-bottom: 8px;
}

.method-item:hover {
  background: rgba(212, 90, 31, 0.05);
}

.method-item.active {
  border-color: var(--brand);
  background: rgba(212, 90, 31, 0.08);
}

.pw-section {
  margin-bottom: 20px;
}

.pw-section label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
}

.pw-section input {
  width: 100%;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: 10px;
  outline: none;
  background: #fff;
}

.pw-section input:focus {
  border-color: var(--brand);
}

.pay-btn {
  width: 100%;
  padding: 14px;
  font-size: 16px;
}

.pay-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.error {
  color: #c0392b;
  margin-top: 12px;
  font-size: 14px;
}
</style>
