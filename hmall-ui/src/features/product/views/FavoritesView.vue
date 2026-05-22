<template>
  <section class="favorites fade-up">
    <h2>我的收藏</h2>

    <div v-if="loading" class="tip">加载中...</div>
    <div v-else-if="error" class="tip error-text">{{ error }}</div>
    <div v-else-if="items.length === 0" class="tip empty-tip">
      还没有收藏商品，
      <RouterLink to="/">去首页逛逛</RouterLink>
    </div>
    <div v-else class="product-grid">
      <ProductCard
        v-for="item in items"
        :key="item.id"
        :product="formatItem(item)"
        @add-cart="handleAddCart(item)"
      />
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { fetchMyFavorites } from "@/features/product/api/favorite.api";
import { useCartStore } from "@/features/cart/store/cart.store";
import ProductCard from "@/shared/components/ProductCard.vue";

const router = useRouter();
const cartStore = useCartStore();
const items = ref([]);
const loading = ref(false);
const error = ref("");

const formatItem = (item) => ({
  id: item.id,
  name: item.name,
  price: (item.price || 0) / 100,
  priceFen: item.price || 0,
  image: item.image,
  spec: item.spec,
  description: item.category || item.brand || "",
  stock: item.stock || 0,
  sold: item.sold || 0
});

const handleAddCart = async (item) => {
  const token = localStorage.getItem("xl_token");
  if (!token) {
    router.push("/login?redirect=" + encodeURIComponent(router.currentRoute.value.fullPath));
    return;
  }
  try {
    await cartStore.addCart(item.id, 1, {
      name: item.name,
      spec: item.spec || "",
      priceFen: item.price || 0,
      image: item.image || ""
    });
  } catch (e) {
    alert(e?.message || "加入购物车失败");
  }
};

onMounted(async () => {
  loading.value = true;
  try {
    const res = await fetchMyFavorites();
    items.value = res?.data || res || [];
  } catch (e) {
    error.value = e?.message || "加载收藏失败";
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.favorites { margin-top: 28px; }
h2 { font: 700 30px var(--font-display); margin-bottom: 20px; }
.tip { color: var(--text-sub); padding: 14px 0; }
.error-text { color: #b42318; }
.empty-tip { font-size: 16px; }
.empty-tip a { color: var(--brand); font-weight: 600; }
.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 20px;
}
</style>
