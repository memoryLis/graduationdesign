<template>
  <section class="item-manage fade-up">
    <h2>商品管理</h2>

    <div class="card actions-bar">
      <button class="btn-primary" @click="openAddDialog">添加商品</button>
      <div class="search-box">
        <input v-model="searchName" placeholder="按商品名称搜索..." @keyup.enter="handleSearch" />
        <button class="btn-search" @click="handleSearch">搜索</button>
        <button v-if="searchName" class="ghost" @click="handleReset">重置</button>
      </div>
    </div>

    <div v-if="items.length" class="card list">
      <article class="row header-row">
        <div>商品图片</div>
        <div>商品名称</div>
        <div>价格</div>
        <div>库存</div>
        <div>分类</div>
        <div>热门状态</div>
        <div>操作</div>
      </article>

      <article v-for="item in items" :key="item.id" class="row">
        <img :src="getImageUrl(item.image)" :alt="item.name" @error="onImgError($event)" />
        <div class="name">{{ item.name }}</div>
        <div class="price">￥{{ formatPrice(item.price) }}</div>
        <div>{{ item.stock }}</div>
        <div>{{ item.category || "-" }}</div>
        <div>
          <span :class="['hot-badge', item.hot ? 'is-hot' : 'not-hot']">
            {{ item.hot ? "热门商品" : "普通商品" }}
          </span>
        </div>
        <div class="actions">
          <button class="btn-edit" @click="handleEdit(item)">编辑</button>
          <button
            :class="item.hot ? 'btn-hot-cancel' : 'btn-hot'"
            @click="handleToggleHot(item)"
          >
            {{ item.hot ? "取消热门" : "设为热门" }}
          </button>
          <button class="btn-delete" @click="handleDelete(item.id)">删除</button>
        </div>
      </article>
    </div>

    <p v-else class="empty">暂无商品数据</p>

    <div v-if="total > 0" class="pagination">
      <button class="ghost" :disabled="pageNo <= 1" @click="changePage(pageNo - 1)">上一页</button>
      <span>第 {{ pageNo }} 页 / 共 {{ totalPages }} 页（{{ total }} 条）</span>
      <button class="ghost" :disabled="pageNo >= totalPages" @click="changePage(pageNo + 1)">下一页</button>
    </div>

    <div v-if="showAddDialog || showEditDialog" class="modal" @click.self="closeDialog">
      <div class="modal-content card">
        <h3>{{ showAddDialog ? "添加商品" : "编辑商品" }}</h3>
        <form @submit.prevent="handleSubmit">
          <div class="form-group">
            <label>商品名称</label>
            <input v-model.trim="form.name" required />
          </div>

          <div class="form-group">
            <label>价格（元）</label>
            <input v-model.number="form.priceYuan" type="number" step="0.01" min="0" required />
          </div>

          <div class="form-group">
            <label>库存</label>
            <input v-model.number="form.stock" type="number" min="0" required />
          </div>

          <div class="form-group">
            <label>分类</label>
            <select v-model="form.category" required>
              <option disabled value="">请选择分类</option>
              <option v-for="cat in categories" :key="cat" :value="cat">{{ cat }}</option>
            </select>
          </div>

          <div class="form-group">
            <label>品牌</label>
            <input v-model.trim="form.brand" />
          </div>

          <div class="form-group">
            <label>规格</label>
            <select v-model="form.spec" required>
              <option disabled value="">请选择规格</option>
              <option v-for="spec in specs" :key="spec" :value="spec">{{ spec }}</option>
            </select>
          </div>

          <div class="form-group">
            <label>商品图片 {{ showEditDialog ? "（可选替换）" : "" }}</label>
            <div class="image-picker">
              <input type="file" accept="image/*" :required="showAddDialog" @change="handleFileChange" />
              <p v-if="showEditDialog" class="file-tip">不选择新图片时，会保留当前图片。</p>
              <img
                v-if="previewImageUrl"
                class="preview-image"
                :src="previewImageUrl"
                alt="商品图片预览"
                @error="onImgError($event)"
              />
            </div>
          </div>

          <div class="form-actions">
            <button type="button" class="ghost" @click="closeDialog">取消</button>
            <button type="submit" class="btn-primary" :disabled="submitting">
              {{ submitting ? "提交中..." : "确定" }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </section>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from "vue";
import {
  addHotItem,
  addItem,
  cancelHotItem,
  deleteItem,
  getItemList,
  updateItem
} from "@/features/admin/api/item-manage.api";

const items = ref([]);
const total = ref(0);
const totalPages = ref(0);
const pageNo = ref(1);
const pageSize = 5;
const searchName = ref("");

const showAddDialog = ref(false);
const showEditDialog = ref(false);
const submitting = ref(false);
const selectedFile = ref(null);
const previewImageUrl = ref("");

const defaultImg = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='86' height='86'%3E%3Crect fill='%23f0e6d6' width='86' height='86'/%3E%3Ctext x='50%25' y='50%25' dominant-baseline='middle' text-anchor='middle' fill='%23ad4310' font-size='24'%3EIMG%3C/text%3E%3C/svg%3E";

const categories = ["拉杆箱", "牛奶", "手机", "硬盘", "水果", "玩具", "美妆", "图书", "零食", "户外运动"];
const specs = ["小", "中", "大"];

const createEmptyForm = () => ({
  id: null,
  name: "",
  priceYuan: 0,
  stock: 0,
  image: "",
  category: "",
  brand: "",
  spec: "",
  status: 1
});

const form = ref(createEmptyForm());

const formatPrice = (priceFen) => (Number(priceFen || 0) / 100).toFixed(2);

const loadItems = async () => {
  try {
    const res = await getItemList(pageNo.value, pageSize, searchName.value);
    const data = res?.data || res || {};
    items.value = data.list || [];
    total.value = data.total || 0;
    totalPages.value = data.pages || 0;
  } catch (error) {
    console.error("加载商品列表失败", error);
  }
};

const handleSearch = () => {
  pageNo.value = 1;
  loadItems();
};

const handleReset = () => {
  searchName.value = "";
  pageNo.value = 1;
  loadItems();
};

const changePage = (page) => {
  if (page < 1 || page > totalPages.value) return;
  pageNo.value = page;
  loadItems();
};

const resetPreviewImage = () => {
  if (previewImageUrl.value && previewImageUrl.value.startsWith("blob:")) {
    URL.revokeObjectURL(previewImageUrl.value);
  }
  previewImageUrl.value = "";
};

const resetFileInput = () => {
  selectedFile.value = null;
};

const openAddDialog = () => {
  closeDialog();
  showAddDialog.value = true;
};

const handleFileChange = (event) => {
  const file = event.target.files?.[0] || null;
  resetPreviewImage();
  selectedFile.value = file;
  if (file) {
    previewImageUrl.value = URL.createObjectURL(file);
  }
};

const buildItemFormData = () => {
  const formData = new FormData();
  if (form.value.id) formData.append("id", form.value.id);
  formData.append("name", form.value.name);
  formData.append("price", Math.round(Number(form.value.priceYuan || 0) * 100));
  formData.append("stock", Number(form.value.stock || 0));
  formData.append("category", form.value.category);
  formData.append("brand", form.value.brand || "");
  formData.append("spec", form.value.spec || "");
  formData.append("status", form.value.status ?? 1);
  if (selectedFile.value) {
    formData.append("file", selectedFile.value);
  }
  return formData;
};

const handleSubmit = async () => {
  if (showAddDialog.value && !selectedFile.value) {
    alert("请上传商品图片");
    return;
  }

  submitting.value = true;
  try {
    const formData = buildItemFormData();
    if (showAddDialog.value) {
      await addItem(formData);
      alert("添加成功");
    } else {
      await updateItem(formData);
      alert("修改成功");
    }
    closeDialog();
    await loadItems();
  } catch (error) {
    console.error("商品保存失败", error);
    alert(error?.message || error?.msg || "操作失败，请重试");
  } finally {
    submitting.value = false;
  }
};

const handleEdit = (item) => {
  resetPreviewImage();
  form.value = {
    id: item.id,
    name: item.name,
    priceYuan: Number(item.price || 0) / 100,
    stock: item.stock,
    image: item.image || "",
    category: item.category || "",
    brand: item.brand || "",
    spec: item.spec || "",
    status: item.status ?? 1
  };
  resetFileInput();
  previewImageUrl.value = getImageUrl(item.image);
  showEditDialog.value = true;
};

const handleToggleHot = async (item) => {
  try {
    if (item.hot) {
      await cancelHotItem(item.id);
      alert("已取消热门商品");
    } else {
      await addHotItem(item.id);
      alert("已设为热门商品");
    }
    await loadItems();
  } catch (error) {
    console.error("切换热门状态失败", error);
    alert(error?.message || error?.msg || "热门状态修改失败，请重试");
  }
};

const handleDelete = async (id) => {
  if (!confirm("确定要删除这个商品吗？")) return;
  try {
    await deleteItem(id);
    alert("删除成功");
    await loadItems();
  } catch (error) {
    console.error("删除商品失败", error);
    alert(error?.message || error?.msg || "删除失败，请重试");
  }
};

const closeDialog = () => {
  showAddDialog.value = false;
  showEditDialog.value = false;
  resetPreviewImage();
  resetFileInput();
  form.value = createEmptyForm();
};

const getImageUrl = (image) => {
  if (!image) return defaultImg;
  if (image.startsWith("http")) return image;
  if (image.startsWith("/uploads/")) return image;

  const filename = image.replace(/\\/g, "/").replace(/^static\/?/, "");
  return `/uploads/${filename}`;
};

const onImgError = (event) => {
  event.target.src = defaultImg;
};

onMounted(() => {
  loadItems();
});

onBeforeUnmount(() => {
  resetPreviewImage();
});
</script>

<style scoped>
.item-manage {
  margin-top: 24px;
}

h2 {
  margin-bottom: 16px;
  font: 700 30px var(--font-display);
}

.actions-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding: 16px 18px;
}

.search-box {
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.search-box input {
  width: 220px;
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  outline: none;
  transition: border-color 0.2s;
}

.search-box input:focus {
  border-color: var(--brand);
}

.btn-search {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  background: var(--brand);
  color: #fff;
  cursor: pointer;
  font: inherit;
  font-size: 14px;
}

.btn-search:hover {
  opacity: 0.9;
}

.list {
  padding: 8px 18px;
}

.row {
  display: grid;
  grid-template-columns: 86px minmax(120px, 2fr) 1fr 0.8fr 1fr 1fr 2fr;
  gap: 12px;
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px dashed var(--border);
}

.header-row {
  border-bottom: 2px solid var(--border);
  color: var(--text-main);
  font-weight: 700;
}

.row:last-child {
  border-bottom: none;
}

img {
  width: 86px;
  height: 86px;
  border-radius: 12px;
  background: #f0e6d6;
  object-fit: cover;
}

.name {
  font-weight: 600;
}

.price {
  color: var(--brand);
  font-weight: 700;
}

.hot-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 84px;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.is-hot {
  background: #fff1e8;
  color: #b44a18;
}

.not-hot {
  background: rgba(80, 96, 140, 0.1);
  color: var(--text-sub);
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.btn-edit,
.btn-delete,
.btn-hot,
.btn-hot-cancel {
  padding: 6px 12px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
  font: inherit;
  font-size: 13px;
  transition: all 0.2s ease;
}

.btn-edit {
  border-color: var(--brand);
  color: var(--brand);
}

.btn-edit:hover {
  background: #fff1e8;
}

.btn-hot {
  border-color: #d65924;
  color: #d65924;
}

.btn-hot:hover {
  background: #fff1e8;
}

.btn-hot-cancel {
  border-color: #8b6f47;
  color: #8b6f47;
}

.btn-hot-cancel:hover {
  background: rgba(139, 111, 71, 0.08);
}

.btn-delete {
  border-color: #c0392b;
  color: #c0392b;
}

.btn-delete:hover {
  background: rgba(192, 57, 43, 0.08);
}

.empty {
  padding: 40px;
  color: var(--text-sub);
  text-align: center;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 16px;
  padding: 12px;
  color: var(--text-sub);
  font-size: 14px;
}

.modal {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.5);
}

.modal-content {
  width: min(500px, 90vw);
  max-height: 90vh;
  overflow-y: auto;
  padding: 24px;
}

.modal-content h3 {
  margin: 0 0 20px;
  font: 700 24px var(--font-display);
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  color: var(--text-main);
  font-weight: 600;
}

.form-group input,
.form-group select {
  width: 100%;
  box-sizing: border-box;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: #fff;
  color: inherit;
  font-size: inherit;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.form-group input:focus,
.form-group select:focus {
  border-color: var(--brand);
  box-shadow: 0 0 0 3px rgba(255, 80, 0, 0.12);
}

.image-picker {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.file-tip {
  margin: 0;
  color: var(--text-sub);
  font-size: 12px;
}

.preview-image {
  width: 110px;
  height: 110px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: #f0e6d6;
  object-fit: cover;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.ghost {
  padding: 8px 16px;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: #fff7f2;
  color: var(--brand);
  cursor: pointer;
  font: inherit;
}

.ghost:hover {
  background: #fff1e8;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 960px) {
  .row {
    grid-template-columns: 72px 1fr;
  }

  .header-row {
    display: none;
  }

  .row > div:not(.actions) {
    grid-column: 2;
  }

  img {
    grid-row: span 6;
    width: 72px;
    height: 72px;
  }
}
</style>
