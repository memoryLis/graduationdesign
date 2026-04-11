<template>
  <section class="detail fade-up">
    <p class="loading" v-if="loading">加载中...</p>
    <p class="error" v-else-if="error">{{ error }}</p>
    <template v-else-if="product">
      <div class="card detail-card">
        <img :src="product.image || defaultImg" :alt="product.name" @error="onImgError" />
        <div class="info">
          <h1>{{ product.name }}</h1>
          <p class="spec" v-if="product.spec">规格：{{ product.spec }}</p>
          <p class="meta" v-if="product.category || product.brand">
            {{ product.category }} {{ product.brand ? "· " + product.brand : "" }}
          </p>
          <p class="price">￥{{ Number(product.price || 0).toFixed(2) }}</p>
          <p class="stock">
            库存：{{ product.stock }} | 已售：{{ product.sold }} | 评论：{{ product.commentCount || 0 }}
          </p>
          <div class="actions">
            <div class="num-ctrl">
              <button @click="num > 1 && num--">-</button>
              <span>{{ num }}</span>
              <button @click="num++">+</button>
            </div>
            <button class="btn-primary" @click="addToCart">加入购物车</button>
          </div>
        </div>
      </div>

      <div class="card comment-card">
        <div class="comment-head">
          <div>
            <h2>商品评论</h2>
            <p>共 {{ commentTotal }} 条评论</p>
          </div>
        </div>

        <div class="comment-form">
          <textarea
            v-model="commentContent"
            maxlength="300"
            placeholder="说说你对这个商品的看法、使用体验或建议..."
          ></textarea>
          <div class="comment-form-bar">
            <span>{{ commentContent.trim().length }}/300</span>
            <button v-if="authStore.isLoggedIn" class="btn-primary" :disabled="commentSubmitting" @click="submitComment">
              {{ commentSubmitting ? "发布中..." : "发表评论" }}
            </button>
            <button v-else class="ghost" @click="goLogin">登录后评论</button>
          </div>
        </div>

        <p class="loading" v-if="commentsLoading">评论加载中...</p>
        <p class="error" v-else-if="commentsError">{{ commentsError }}</p>
        <p class="empty" v-else-if="comments.length === 0">还没有评论，来发表第一条看法吧。</p>
        <div v-else class="comment-list">
          <article class="comment-item" v-for="comment in comments" :key="comment.id">
            <div class="comment-meta">
              <strong>{{ comment.username || ("用户" + comment.userId) }}</strong>
              <span>{{ formatDateTime(comment.createTime) }}</span>
            </div>
            <p>{{ comment.content }}</p>
          </article>
        </div>

        <div class="pagination" v-if="commentPages > 1">
          <button class="ghost" :disabled="commentPageNo <= 1 || commentsLoading" @click="changeCommentPage(commentPageNo - 1)">
            上一页
          </button>
          <span>第 {{ commentPageNo }} 页 / 共 {{ commentPages }} 页</span>
          <button class="ghost" :disabled="commentPageNo >= commentPages || commentsLoading" @click="changeCommentPage(commentPageNo + 1)">
            下一页
          </button>
        </div>
      </div>
    </template>
  </section>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { getProductDetail } from "@/features/product/api/product.api";
import { createItemComment, fetchItemComments } from "@/features/product/api/comment.api";
import { useCartStore } from "@/features/cart/store/cart.store";
import { useAuthStore } from "@/features/auth/store/auth.store";

const route = useRoute();
const router = useRouter();
const product = ref(null);
const loading = ref(true);
const error = ref("");
const num = ref(1);
const comments = ref([]);
const commentsLoading = ref(true);
const commentsError = ref("");
const commentPageNo = ref(1);
const commentPageSize = 10;
const commentTotal = ref(0);
const commentPages = ref(0);
const commentContent = ref("");
const commentSubmitting = ref(false);
const cartStore = useCartStore();
const authStore = useAuthStore();
const defaultImg = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='400' height='400'%3E%3Crect fill='%23f0e6d6' width='400' height='400'/%3E%3Ctext x='50%25' y='50%25' dominant-baseline='middle' text-anchor='middle' fill='%23ad4310' font-size='48'%3E%F0%9F%93%A6%3C/text%3E%3C/svg%3E";

const onImgError = (e) => {
  e.target.src = defaultImg;
};

const formatDateTime = (value) => {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  }).format(date);
};

const goLogin = () => {
  router.push({ path: "/login", query: { redirect: route.fullPath } });
};

const loadProduct = async () => {
  loading.value = true;
  error.value = "";
  try {
    product.value = await getProductDetail(route.params.id);
  } catch {
    error.value = "加载商品详情失败，请检查后端服务。";
  } finally {
    loading.value = false;
  }
};

const loadComments = async (page = commentPageNo.value) => {
  commentsLoading.value = true;
  commentsError.value = "";
  try {
    const result = await fetchItemComments(route.params.id, page, commentPageSize);
    comments.value = result.list || [];
    commentTotal.value = result.total || 0;
    commentPages.value = result.pages || 0;
    commentPageNo.value = page;
  } catch {
    comments.value = [];
    commentTotal.value = 0;
    commentPages.value = 0;
    commentsError.value = "加载评论失败，请稍后再试。";
  } finally {
    commentsLoading.value = false;
  }
};

const changeCommentPage = async (page) => {
  if (page < 1 || (commentPages.value && page > commentPages.value)) return;
  await loadComments(page);
};

const addToCart = async () => {
  if (!authStore.isLoggedIn) {
    goLogin();
    return;
  }
  if (!product.value) return;
  try {
    await cartStore.addCart(product.value.id, num.value, {
      id: product.value.id,
      name: product.value.name,
      price: product.value.price,
      priceFen: product.value.priceFen,
      image: product.value.image,
      spec: product.value.spec
    });
    alert("已加入购物车");
  } catch {
    alert("加入购物车失败");
  }
};

const submitComment = async () => {
  const content = commentContent.value.trim();
  if (!content) {
    alert("请输入评论内容");
    return;
  }
  if (content.length > 300) {
    alert("评论内容不能超过 300 字");
    return;
  }
  commentSubmitting.value = true;
  try {
    await createItemComment({
      itemId: Number(route.params.id),
      content
    });
    commentContent.value = "";
    await loadComments(1);
    if (product.value) {
      product.value.commentCount = Number(product.value.commentCount || 0) + 1;
    }
  } catch (e) {
    alert(e?.message || e?.msg || "发表评论失败");
  } finally {
    commentSubmitting.value = false;
  }
};

onMounted(async () => {
  await Promise.all([loadProduct(), loadComments(1)]);
});
</script>

<style scoped>
.detail {
  margin-top: 26px;
}

.detail-card,
.comment-card {
  padding: 22px;
}

.detail-card {
  display: grid;
  grid-template-columns: minmax(0, 480px) minmax(0, 1fr);
  gap: 28px;
}

img {
  width: 100%;
  border-radius: 14px;
  object-fit: cover;
  min-height: 320px;
  max-height: 480px;
  background: #fff7f2;
  border: 1px solid #f6ebe4;
}

.info {
  display: flex;
  flex-direction: column;
}

h1 {
  margin-top: 0;
  font: 700 32px var(--font-display);
}

.spec {
  color: var(--text-sub);
  font-size: 14px;
  margin: 4px 0;
}

.meta {
  color: var(--text-sub);
  font-size: 14px;
}

.price {
  color: var(--brand);
  font-size: 34px;
  font-weight: 700;
  margin: 20px 0 8px;
}

.stock {
  color: var(--text-sub);
  font-size: 13px;
  margin-bottom: 20px;
}

.actions {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: auto;
  padding-top: 18px;
  border-top: 1px solid #f3f3f3;
}

.num-ctrl {
  display: flex;
  align-items: center;
  gap: 0;
  border: 1px solid var(--border);
  border-radius: 20px;
  overflow: hidden;
}

.num-ctrl button {
  width: 40px;
  height: 40px;
  border: none;
  background: #fff;
  cursor: pointer;
  font-size: 18px;
  color: var(--text-main);
}

.num-ctrl button:hover {
  background: #fff4ed;
}

.num-ctrl span {
  width: 48px;
  text-align: center;
  font-weight: 600;
}

.comment-card {
  margin-top: 18px;
}

.comment-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}

.comment-head h2 {
  margin: 0;
  font: 700 26px var(--font-display);
}

.comment-head p {
  margin: 8px 0 0;
  color: var(--text-sub);
  font-size: 14px;
}

.comment-form {
  padding: 16px;
  border-radius: 14px;
  background: #fff8f3;
  border: 1px solid #ffe1d1;
}

.comment-form textarea {
  width: 100%;
  min-height: 110px;
  resize: vertical;
  padding: 14px;
  font: inherit;
  background: #fff;
}

.comment-form-bar {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.comment-form-bar span {
  color: var(--text-sub);
  font-size: 13px;
}

.comment-list {
  margin-top: 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.comment-item {
  padding: 16px;
  border-radius: 14px;
  border: 1px solid #f0f0f0;
  background: #fff;
}

.comment-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.comment-meta strong {
  color: var(--text-main);
}

.comment-meta span {
  color: var(--text-sub);
  font-size: 12px;
}

.comment-item p {
  margin: 0;
  color: var(--text-main);
  line-height: 1.8;
  white-space: pre-wrap;
}

.ghost {
  border: 1px solid #ffd2bf;
  border-radius: 20px;
  background: #fff7f2;
  color: var(--brand);
  padding: 10px 16px;
  cursor: pointer;
  font: inherit;
  font-weight: 600;
}

.ghost:hover {
  background: #fff1e8;
}

.pagination {
  margin-top: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--text-sub);
  font-size: 14px;
}

.loading,
.error,
.empty {
  color: var(--text-sub);
  margin-top: 20px;
}

.error {
  color: #c0392b;
}

@media (max-width: 900px) {
  .detail-card {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .detail-card,
  .comment-card {
    padding: 16px;
  }

  .actions,
  .comment-form-bar,
  .comment-meta,
  .pagination {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
