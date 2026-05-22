<template>
  <section class="search-page fade-up">
    <div class="search-box card">
      <input v-model="keyword" placeholder="搜索你想要的商品" @keyup.enter="handleSearchAction(1)" />
      <button class="btn-primary" @click="handleSearchAction(1)">搜索</button>
    </div>

    <div class="category-bar" v-if="categories.length">
      <button
        :class="['cat-tag', { active: activeCategory === GUESS_CATEGORY }]"
        @click="selectGuessYouLike"
      >
        猜你喜欢
      </button>
      <button
        :class="['cat-tag', { active: activeCategory === '' }]"
        @click="selectCategory('')"
      >
        全部
      </button>
      <button
        v-for="cat in categories"
        :key="cat"
        :class="['cat-tag', { active: activeCategory === cat }]"
        @click="selectCategory(cat)"
      >
        {{ cat }}
      </button>
    </div>

    <p class="loading" v-if="loading">{{ loadingText }}</p>

    <div class="grid" v-else-if="list.length">
      <ProductCard v-for="item in list" :key="item.id" :product="item" @add-cart="addToCart" />
    </div>

    <p class="empty" v-else-if="searched">{{ emptyText }}</p>
    <p class="hint" v-else>点击“猜你喜欢”或选择分类浏览商品，也可以直接搜索。</p>

    <div class="pagination" v-if="showPagination">
      <button class="page-btn" :disabled="currentPage <= 1" @click="doSearch(currentPage - 1)">上一页</button>
      <template v-for="p in displayPages" :key="p">
        <span v-if="p === '...'" class="page-ellipsis">...</span>
        <button v-else :class="['page-btn', { active: p === currentPage }]" @click="doSearch(p)">
          {{ p }}
        </button>
      </template>
      <button class="page-btn" :disabled="currentPage >= totalPages" @click="doSearch(currentPage + 1)">下一页</button>
      <span class="page-info">共 {{ total }} 件商品</span>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import ProductCard from "@/shared/components/ProductCard.vue";
import {
  getCategories,
  getGuessYouLikeProducts,
  searchProducts
} from "@/features/search/api/search.api";
import { useCartStore } from "@/features/cart/store/cart.store";
import { useAuthStore } from "@/features/auth/store/auth.store";

const GUESS_CATEGORY = "__guess__";

const route = useRoute();
const router = useRouter();
const cartStore = useCartStore();
const authStore = useAuthStore();

const keyword = ref("");
const list = ref([]);
const loading = ref(false);
const searched = ref(false);
const categories = ref([]);
const activeCategory = ref(GUESS_CATEGORY);
const currentPage = ref(1);
const total = ref(0);
const totalPages = ref(0);
const pageSize = 20;

const showPagination = computed(() => activeCategory.value !== GUESS_CATEGORY && totalPages.value > 1);

const loadingText = computed(() => (
  activeCategory.value === GUESS_CATEGORY ? "推荐生成中..." : "搜索中..."
));

const emptyText = computed(() => (
  activeCategory.value === GUESS_CATEGORY
    ? "暂时没有可展示的推荐结果。"
    : "没有搜索到内容，试试其它关键词或分类。"
));

const displayPages = computed(() => {
  const pages = [];
  const tp = totalPages.value;
  const cp = currentPage.value;
  if (tp <= 7) {
    for (let i = 1; i <= tp; i += 1) pages.push(i);
  } else {
    pages.push(1);
    if (cp > 3) pages.push("...");
    const start = Math.max(2, cp - 1);
    const end = Math.min(tp - 1, cp + 1);
    for (let i = start; i <= end; i += 1) pages.push(i);
    if (cp < tp - 2) pages.push("...");
    pages.push(tp);
  }
  return pages;
});

const loadGuessYouLike = async () => {
  loading.value = true;
  searched.value = true;
  activeCategory.value = GUESS_CATEGORY;
  currentPage.value = 1;
  totalPages.value = 0;
  try {
    const result = await getGuessYouLikeProducts();
    list.value = result || [];
    total.value = list.value.length;
  } catch {
    list.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
};

const doSearch = async (page = 1) => {
  loading.value = true;
  searched.value = true;
  currentPage.value = page;
  try {
    const category = activeCategory.value === GUESS_CATEGORY ? "" : activeCategory.value;
    const result = await searchProducts(keyword.value, page, pageSize, category);
    list.value = result.list || [];
    total.value = result.total || 0;
    totalPages.value = result.pages || Math.ceil((result.total || 0) / pageSize);
  } catch {
    list.value = [];
    total.value = 0;
    totalPages.value = 0;
  } finally {
    loading.value = false;
  }
};

const selectGuessYouLike = async () => {
  keyword.value = "";
  await router.replace({ path: "/search", query: {} });
  await loadGuessYouLike();
};

const selectCategory = (cat) => {
  activeCategory.value = cat;
  doSearch(1);
};

const handleSearchAction = (page = 1) => {
  if (activeCategory.value === GUESS_CATEGORY) {
    activeCategory.value = "";
  }
  doSearch(page);
};

const addToCart = async (item) => {
  if (!authStore.isLoggedIn) {
    router.push("/login");
    return;
  }
  try {
    await cartStore.addCart(item.id, 1, item);
  } catch {
    // ignore
  }
};

const syncFromRoute = async () => {
  const key = route.query.key || route.query.keyword || "";
  const cat = route.query.category || "";
  keyword.value = String(key);

  if (key || cat) {
    activeCategory.value = String(cat);
    await doSearch(1);
    return;
  }

  await loadGuessYouLike();
};

onMounted(async () => {
  try {
    categories.value = await getCategories();
  } catch {
    categories.value = [];
  }
  await syncFromRoute();
});

watch(
  () => [route.query.key, route.query.keyword, route.query.category],
  async () => {
    await syncFromRoute();
  }
);
</script>

<style scoped>
.search-page {
  margin-top: 26px;
}

.search-box {
  display: flex;
  padding: 14px;
  gap: 12px;
  align-items: center;
}

input {
  flex: 1;
  height: 46px;
  padding: 0 16px;
  font-size: 14px;
}

.category-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
  padding: 0 2px;
}

.cat-tag {
  padding: 8px 16px;
  border-radius: 18px;
  border: 1px solid #ffd8c6;
  background: #fff;
  color: var(--text-sub);
  cursor: pointer;
  font: inherit;
  font-size: 14px;
  transition: all 0.2s ease;
}

.cat-tag:hover {
  border-color: var(--brand);
  color: var(--brand);
  background: #fff7f2;
}

.cat-tag.active {
  background: var(--bg-warm);
  color: #fff;
  border-color: transparent;
}

.grid {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 18px;
}

.empty,
.hint,
.loading {
  color: var(--text-sub);
  margin-top: 20px;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 28px;
  flex-wrap: wrap;
}

.page-btn {
  min-width: 38px;
  height: 38px;
  border-radius: 19px;
  border: 1px solid var(--border);
  background: #fff;
  color: var(--text-main);
  cursor: pointer;
  font: inherit;
  font-size: 14px;
  transition: all 0.2s ease;
}

.page-btn:hover:not(:disabled) {
  border-color: #ffbf9e;
  color: var(--brand);
  background: #fff7f2;
}

.page-btn.active {
  background: var(--bg-warm);
  color: #fff;
  border-color: transparent;
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-ellipsis {
  color: var(--text-sub);
  padding: 0 4px;
}

.page-info {
  color: var(--text-sub);
  font-size: 13px;
  margin-left: 8px;
}

@media (max-width: 640px) {
  .search-box {
    flex-direction: column;
    align-items: stretch;
  }

  .search-box .btn-primary {
    width: 100%;
  }
}
</style>
