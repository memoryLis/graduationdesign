<template>
  <section class="pay-success fade-up">
    <div class="card success-card">
      <div class="icon">✅</div>
      <h2>支付成功</h2>
      <p class="amount">支付金额：￥{{ displayAmount }}</p>
      <p class="order-no">订单号：{{ orderId }}</p>
      <div class="actions">
        <RouterLink class="btn-primary" to="/">继续购物</RouterLink>
        <RouterLink class="ghost-btn" to="/cart">查看购物车</RouterLink>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed } from "vue";
import { useRoute } from "vue-router";

const route = useRoute();
const orderId = computed(() => route.query.orderId || "");
const displayAmount = computed(() => {
  const amt = Number(route.query.amount) || 0;
  return (amt / 100).toFixed(2);
});
</script>

<style scoped>
.pay-success {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

.success-card {
  width: min(480px, 94vw);
  padding: 40px 28px;
  text-align: center;
}

.icon {
  font-size: 64px;
  margin-bottom: 16px;
}

h2 {
  margin: 0 0 12px;
  font: 700 30px var(--font-display);
  color: #27ae60;
}

.amount {
  font-size: 20px;
  color: var(--brand-dark);
  font-weight: 700;
  margin: 8px 0;
}

.order-no {
  color: var(--text-sub);
  margin-bottom: 24px;
}

.actions {
  display: flex;
  justify-content: center;
  gap: 16px;
}

.ghost-btn {
  padding: 10px 18px;
  border: 1px solid var(--border);
  border-radius: 999px;
  color: var(--brand-dark);
}

.ghost-btn:hover {
  background: rgba(212, 90, 31, 0.08);
}
</style>
