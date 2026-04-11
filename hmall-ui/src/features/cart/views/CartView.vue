<template>
  <section class="cart fade-up">
    <h2>购物车</h2>

    <div class="card list" v-if="store.items.length">
      <div class="select-all-row">
        <label>
          <input type="checkbox" :checked="allSelected" @change="store.toggleSelectAll($event.target.checked)" />
          全选
        </label>
      </div>
      <article class="row" v-for="item in store.items" :key="item.id">
        <input type="checkbox" :checked="item.selected" @change="store.toggleSelect(item.id)" />
        <img :src="item.image || defaultImg" :alt="item.name" @error="onImgError($event)" />
        <div class="meta">
          <h3>{{ item.name }}</h3>
          <p class="spec" v-if="item.spec">{{ item.spec }}</p>
          <p>单价：￥{{ Number(item.price || 0).toFixed(2) }}</p>
        </div>
        <div class="num-ctrl">
          <button @click="changeNum(item, -1)">-</button>
          <span>{{ item.num }}</span>
          <button @click="changeNum(item, 1)">+</button>
        </div>
        <div class="subtotal">￥{{ (Number(item.price || 0) * Number(item.num || 0)).toFixed(2) }}</div>
        <button class="ghost" @click="store.remove(item.id)">删除</button>
      </article>
    </div>
    <p class="empty" v-else>购物车空空如也，去首页挑几件吧。</p>

    <div class="checkout-bar card" v-if="store.items.length">
      <p>已选 {{ store.selectedCount }} 件商品，合计 <strong>￥{{ store.selectedAmount.toFixed(2) }}</strong></p>
      <button class="btn-primary" :disabled="!store.selectedCount" @click="goCheckout">去结算</button>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useCartStore } from "@/features/cart/store/cart.store";

const store = useCartStore();
const router = useRouter();
const defaultImg = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='86' height='86'%3E%3Crect fill='%23f0e6d6' width='86' height='86'/%3E%3Ctext x='50%25' y='50%25' dominant-baseline='middle' text-anchor='middle' fill='%23ad4310' font-size='24'%3E📦%3C/text%3E%3C/svg%3E";

const allSelected = computed(() => store.items.length > 0 && store.items.every((i) => i.selected));

const onImgError = (e) => {
  e.target.src = defaultImg;
};

const changeNum = (item, delta) => {
  const newNum = item.num + delta;
  if (newNum < 1) return;
  store.updateNum(item, newNum);
};

const goCheckout = () => {
  if (!store.selectedCount) return;
  router.push("/order-confirm");
};

onMounted(() => {
  store.loadCart();
});
</script>

<style scoped>
.cart {
  margin-top: 24px;
}

h2 {
  margin: 0 0 18px;
  font: 700 30px var(--font-display);
}

.list {
  padding: 10px 20px;
}

.select-all-row {
  padding: 14px 0;
  border-bottom: 1px solid #f2f2f2;
}

.select-all-row label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-weight: 600;
}

.row {
  display: grid;
  grid-template-columns: 30px 86px 1fr auto auto auto;
  gap: 16px;
  align-items: center;
  padding: 18px 0;
  border-bottom: 1px solid #f5f5f5;
}

.row:last-child {
  border-bottom: none;
}

.row input[type="checkbox"] {
  width: 18px;
  height: 18px;
  cursor: pointer;
}

img {
  width: 86px;
  height: 86px;
  border-radius: 10px;
  object-fit: cover;
  background: #fff7f2;
  border: 1px solid #f6ebe4;
}

h3 {
  margin: 0;
}

.spec {
  font-size: 12px;
  color: var(--text-sub);
  margin: 2px 0;
}

.meta p {
  color: var(--text-sub);
  margin: 6px 0 0;
}

.num-ctrl {
  display: flex;
  align-items: center;
  border: 1px solid var(--border);
  border-radius: 18px;
  overflow: hidden;
}

.num-ctrl button {
  width: 34px;
  height: 34px;
  border: none;
  background: #fff;
  cursor: pointer;
  font-size: 16px;
  color: var(--text-main);
}

.num-ctrl button:hover {
  background: #fff5ef;
}

.num-ctrl span {
  width: 40px;
  text-align: center;
  font-weight: 600;
  font-size: 14px;
}

.subtotal {
  font-weight: 700;
  color: var(--brand);
  min-width: 80px;
  text-align: right;
}

.ghost {
  border: 1px solid #ffd2bf;
  border-radius: 18px;
  background: #fff7f2;
  color: var(--brand);
  padding: 8px 14px;
  cursor: pointer;
  font: inherit;
  font-weight: 600;
}

.ghost:hover {
  background: #fff1e8;
}

.checkout-bar {
  margin-top: 16px;
  padding: 18px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.checkout-bar strong {
  color: var(--brand);
  font-size: 24px;
}

.checkout-bar button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.empty {
  color: var(--text-sub);
}

@media (max-width: 720px) {
  .row {
    grid-template-columns: 30px 70px 1fr;
  }

  .num-ctrl, .subtotal, .ghost {
    justify-self: start;
  }

  .checkout-bar {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>
