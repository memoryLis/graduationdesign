<template>
  <div>
    <HeroBanner />

    <section class="section fade-up">
      <div class="title-row">
        <h2>人气推荐</h2>
        <RouterLink class="view-all" to="/search">查看全部</RouterLink>
      </div>

      <p v-if="loading" class="loading">加载中...</p>
      <p v-else-if="error" class="error">{{ error }}</p>
      <div v-else-if="products.length" class="grid">
        <ProductCard v-for="item in products" :key="item.id" :product="item" @add-cart="addToCart" />
      </div>
      <p v-else class="empty">暂无热门商品</p>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "@/features/auth/store/auth.store";
import { useCartStore } from "@/features/cart/store/cart.store";
import HeroBanner from "@/features/home/components/HeroBanner.vue";
import { getRecommendProducts } from "@/features/home/api/home.api";
import ProductCard from "@/shared/components/ProductCard.vue";

const products = ref([]);
const loading = ref(true);
const error = ref("");

const cartStore = useCartStore();
const authStore = useAuthStore();
const router = useRouter();

const addToCart = async (item) => {
  if (!authStore.isLoggedIn) {
    router.push("/login");
    return;
  }
  try {
    await cartStore.addCart(item.id, 1, item);
  } catch (err) {
    console.error("加入购物车失败", err);
  }
};

const loadRecommendProducts = async () => {
  loading.value = true;
  error.value = "";
  try {
    products.value = await getRecommendProducts();
  } catch (err) {
    console.error("加载热门商品失败", err);
    error.value = err?.message || err?.msg || "加载热门商品失败，请检查后端服务是否正常。";
    products.value = [];
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadRecommendProducts();
});
</script>

<style scoped>
.section {
  margin-top: 28px;
}

.title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

h2 {
  margin: 0;
  font: 700 28px var(--font-display);
}

.view-all {
  color: var(--brand);
  font-size: 14px;
  font-weight: 700;
  transition: color 0.2s ease;
}

.view-all:hover {
  color: var(--brand-dark);
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 18px;
}

.loading,
.error,
.empty {
  margin-top: 20px;
  color: var(--text-sub);
}

.error {
  color: #c0392b;
}
</style>
