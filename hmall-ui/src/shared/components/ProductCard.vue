<template>
  <article class="product card scale-in" @click="$router.push(`/product/${product.id}`)">
    <div class="img-wrap">
      <img :src="product.image || defaultImg" :alt="product.name" @error="onImgError" />
    </div>
    <div class="content">
      <h3>{{ product.name }}</h3>
      <p class="desc">{{ product.description || product.spec || "高品质精选商品" }}</p>
      <div class="price-row">
        <strong>￥{{ Number(product.price || 0).toFixed(2) }}</strong>
        <button class="btn-primary btn-cart" @click.stop="$emit('add-cart', product)">加入购物车</button>
      </div>
    </div>
  </article>
</template>

<script setup>
defineProps({
  product: {
    type: Object,
    required: true
  }
});

defineEmits(["add-cart"]);

const defaultImg = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='300' height='200'%3E%3Crect fill='%23eef0f8' width='300' height='200'/%3E%3Ctext x='50%25' y='50%25' dominant-baseline='middle' text-anchor='middle' fill='%235b6abf' font-size='36'%3EIMG%3C/text%3E%3C/svg%3E";

const onImgError = (event) => {
  event.target.src = defaultImg;
};
</script>

<style scoped>
.product {
  overflow: hidden;
  cursor: pointer;
  border-radius: 14px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.product:hover {
  border-color: #ffb68c;
}

.img-wrap {
  overflow: hidden;
  border-radius: 14px 14px 0 0;
  background: #fff7f2;
}

img {
  width: 100%;
  height: 220px;
  background: #fff7f2;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.product:hover img {
  transform: scale(1.03);
}

.content {
  padding: 14px 16px 16px;
}

h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.5;
  display: -webkit-box;
  min-height: 48px;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.desc {
  display: -webkit-box;
  min-height: 38px;
  margin: 6px 0 12px;
  overflow: hidden;
  color: var(--text-sub);
  font-size: 13px;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.price-row {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 8px;
}

strong {
  color: var(--brand);
  font-size: 24px;
  line-height: 1;
}

.btn-cart {
  min-height: 34px;
  padding: 0 14px;
  font-size: 13px;
}
</style>
