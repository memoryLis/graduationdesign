<template>
  <section class="profile fade-up">
    <h2>个人信息</h2>

    <div class="card section point-section">
      <div class="section-header">
        <h3>我的积分</h3>
        <span class="point-total">{{ pointSummary.points }} 分</span>
      </div>

      <div v-if="pointLoading" class="tip">积分加载中...</div>
      <div v-else class="point-layout">
        <div class="point-panel">
          <p class="point-label">当前剩余积分</p>
          <strong>{{ pointSummary.points }}</strong>
        </div>

        <div class="point-records">
          <div class="point-record-header">
            <h4>积分使用情况</h4>
            <span class="tip-inline">共 {{ pointTotal }} 条</span>
          </div>

          <div v-if="pointRecords.length === 0" class="tip">暂无积分使用记录</div>
          <ul v-else class="point-record-list">
            <li v-for="record in pointRecords" :key="record.id" class="point-record-item">
              <div>
                <strong>订单 {{ record.bizOrderNo }}</strong>
                <p>{{ record.statusText }}</p>
              </div>
              <div class="point-record-side">
                <span class="point-change point-minus">-{{ record.amount }}</span>
                <small>{{ formatDateTime(record.paySuccessTime || record.createTime) }}</small>
              </div>
            </li>
          </ul>

          <div class="pagination point-pagination" v-if="pointPages > 1">
            <button class="ghost" :disabled="pointPageNo <= 1 || pointLoading" @click="changePointPage(pointPageNo - 1)">
              上一页
            </button>
            <span>第 {{ pointPageNo }} 页 / 共 {{ pointPages }} 页</span>
            <button class="ghost" :disabled="pointPageNo >= pointPages || pointLoading" @click="changePointPage(pointPageNo + 1)">
              下一页
            </button>
          </div>
        </div>
      </div>
    </div>

    <div class="card section">
      <div class="section-header">
        <h3>收货地址</h3>
        <button class="btn-primary btn-sm" @click="openAdd">+ 添加地址</button>
      </div>

      <div v-if="loading" class="tip">加载中...</div>
      <div v-else-if="addresses.length === 0" class="tip">暂无收货地址，去添加一个吧。</div>
      <ul v-else class="addr-list">
        <li class="addr-item" v-for="addr in addresses" :key="addr.id">
          <div class="addr-info">
            <span class="addr-tag" v-if="addr.isDefault === 1">默认</span>
            <strong>{{ addr.contact }}</strong>
            <span class="mobile">{{ addr.mobile }}</span>
            <p class="addr-text">{{ addr.province }} {{ addr.city }} {{ addr.town }} {{ addr.street }}</p>
            <p class="notes" v-if="addr.notes">备注：{{ addr.notes }}</p>
          </div>
          <button class="ghost" @click="openEdit(addr)">编辑</button>
        </li>
      </ul>
    </div>

    <div class="card section comment-section">
      <div class="section-header">
        <h3>我的评论</h3>
        <span class="tip-inline">共 {{ commentTotal }} 条</span>
      </div>

      <div v-if="commentLoading" class="tip">加载中...</div>
      <div v-else-if="commentError" class="tip error-text">{{ commentError }}</div>
      <div v-else-if="myComments.length === 0" class="tip">你还没有发表过评论，去商品详情页写一点吧。</div>
      <div v-else class="comment-list">
        <article class="comment-item" v-for="comment in myComments" :key="comment.id">
          <div class="comment-top">
            <div>
              <h4>{{ comment.itemName || ("商品" + comment.itemId) }}</h4>
              <p>{{ formatDateTime(comment.createTime) }}</p>
            </div>
            <div class="comment-actions">
              <RouterLink class="ghost link-btn" :to="`/product/${comment.itemId}`">查看商品</RouterLink>
              <button class="ghost danger-btn" @click="handleDeleteComment(comment)">删除评论</button>
            </div>
          </div>
          <p class="comment-content">{{ comment.content }}</p>
        </article>
      </div>

      <div class="pagination" v-if="commentPages > 1">
        <button class="ghost" :disabled="commentPageNo <= 1 || commentLoading" @click="changeCommentPage(commentPageNo - 1)">
          上一页
        </button>
        <span>第 {{ commentPageNo }} 页 / 共 {{ commentPages }} 页</span>
        <button class="ghost" :disabled="commentPageNo >= commentPages || commentLoading" @click="changeCommentPage(commentPageNo + 1)">
          下一页
        </button>
      </div>
    </div>

    <div class="modal-mask" v-if="showModal" @click="closeModal">
      <div class="modal-box card scale-in" @click.stop>
        <div class="modal-head">
          <div>
            <h4>{{ isEdit ? "编辑地址" : "新增收货地址" }}</h4>
            <p class="modal-sub">完善信息后，下单时可以直接选择。</p>
          </div>
          <button class="icon-close" type="button" aria-label="关闭" @click="closeModal">×</button>
        </div>

        <p class="form-error" v-if="formError">{{ formError }}</p>

        <div class="modal-body">
          <div class="form-grid">
            <div class="form-row">
              <label>联系人 <span class="req">*</span></label>
              <input ref="contactInputRef" v-model="form.contact" autocomplete="name" maxlength="20" placeholder="请输入联系人姓名" />
            </div>

            <div class="form-row">
              <label>手机号 <span class="req">*</span></label>
              <input
                v-model="form.mobile"
                type="tel"
                inputmode="numeric"
                autocomplete="tel"
                maxlength="11"
                placeholder="请输入 11 位手机号"
                @input="sanitizeMobile"
              />
            </div>

            <div class="form-row">
              <label>省份 <span class="req">*</span></label>
              <input v-model="form.province" autocomplete="address-level1" placeholder="如：广东省" />
            </div>

            <div class="form-row">
              <label>城市 <span class="req">*</span></label>
              <input v-model="form.city" autocomplete="address-level2" placeholder="如：深圳市" />
            </div>

            <div class="form-row">
              <label>区 / 县</label>
              <input v-model="form.town" autocomplete="address-level3" placeholder="如：南山区" />
            </div>

            <div class="form-row form-row--span2">
              <label>详细地址 <span class="req">*</span></label>
              <textarea
                v-model="form.street"
                autocomplete="street-address"
                rows="3"
                maxlength="120"
                placeholder="街道、门牌号、小区楼栋单元等"
              ></textarea>
            </div>

            <div class="form-row form-row--span2">
              <label>备注</label>
              <input v-model="form.notes" maxlength="60" placeholder="选填，例如：工作日 9:00-18:00 可收货" />
            </div>

            <div class="form-row form-row--span2 form-row--check">
              <label>
                <input type="checkbox" :checked="form.isDefault === 1" @change="form.isDefault = $event.target.checked ? 1 : 0" />
                设为默认地址
              </label>
            </div>
          </div>
        </div>

        <div class="modal-actions">
          <button type="button" class="ghost" @click="closeModal">取消</button>
          <button type="button" class="btn-primary" :disabled="submitting" @click="submitForm">
            {{ submitting ? "保存中..." : "保存地址" }}
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { addAddress, fetchAddressList, updateAddress } from "@/features/order/api/address.api";
import { queryMyPayOrders } from "@/features/order/api/my-order.api";
import { deleteMyComment, fetchMyComments } from "@/features/product/api/comment.api";
import { fetchMyPoints } from "@/features/user/api/user.api";

const addresses = ref([]);
const myComments = ref([]);
const pointSummary = ref({ points: 0 });
const pointRecords = ref([]);
const loading = ref(false);
const commentLoading = ref(false);
const pointLoading = ref(false);
const commentError = ref("");
const commentPageNo = ref(1);
const commentPageSize = 10;
const commentTotal = ref(0);
const commentPages = ref(0);
const pointPageNo = ref(1);
const pointPageSize = 6;
const pointTotal = ref(0);
const pointPages = ref(0);
const showModal = ref(false);
const isEdit = ref(false);
const submitting = ref(false);
const formError = ref("");
const contactInputRef = ref(null);

const emptyForm = () => ({
  id: null,
  contact: "",
  mobile: "",
  province: "",
  city: "",
  town: "",
  street: "",
  notes: "",
  isDefault: 0
});

const form = ref(emptyForm());

const statusTextMap = {
  0: "待提交",
  1: "待支付",
  2: "已取消",
  3: "支付成功"
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

const handleEsc = (event) => {
  if (event.key === "Escape") closeModal();
};

watch(showModal, async (visible) => {
  document.body.classList.toggle("modal-open", visible);
  if (visible) {
    window.addEventListener("keydown", handleEsc);
    await nextTick();
    contactInputRef.value?.focus?.();
  } else {
    window.removeEventListener("keydown", handleEsc);
  }
});

onBeforeUnmount(() => {
  document.body.classList.remove("modal-open");
  window.removeEventListener("keydown", handleEsc);
});

function sanitizeMobile() {
  form.value.mobile = (form.value.mobile || "").replace(/\D/g, "").slice(0, 11);
}

async function loadMyPoints() {
  pointLoading.value = true;
  try {
    const result = await fetchMyPoints();
    const data = result?.data || result || {};
    pointSummary.value = {
      userId: data.userId,
      username: data.username,
      points: data.points ?? 0
    };
  } catch (error) {
    console.error("加载积分失败", error);
    pointSummary.value = { points: 0 };
  } finally {
    pointLoading.value = false;
  }
}

async function loadPointUsage(page = pointPageNo.value) {
  pointLoading.value = true;
  try {
    const result = await queryMyPayOrders(page, pointPageSize);
    const data = result?.data || result || {};
    const list = Array.isArray(data.list) ? data.list : [];
    pointRecords.value = list
      .filter((item) => item.payType === 5 || item.payChannelCode === "balance")
      .map((item) => ({
        ...item,
        statusText: statusTextMap[item.status] || "未知状态"
      }));
    pointTotal.value = data.total || 0;
    pointPages.value = data.pages || 0;
    pointPageNo.value = page;
  } catch (error) {
    console.error("加载积分使用情况失败", error);
    pointRecords.value = [];
    pointTotal.value = 0;
    pointPages.value = 0;
  } finally {
    pointLoading.value = false;
  }
}

async function loadAddresses() {
  loading.value = true;
  try {
    const res = await fetchAddressList();
    addresses.value = Array.isArray(res) ? res : res?.data ?? [];
  } catch (error) {
    console.error("加载地址失败", error);
  } finally {
    loading.value = false;
  }
}

async function loadMyComments(page = commentPageNo.value) {
  commentLoading.value = true;
  commentError.value = "";
  try {
    const result = await fetchMyComments(page, commentPageSize);
    myComments.value = result.list || [];
    commentTotal.value = result.total || 0;
    commentPages.value = result.pages || 0;
    commentPageNo.value = page;
  } catch (error) {
    console.error("加载评论失败", error);
    myComments.value = [];
    commentTotal.value = 0;
    commentPages.value = 0;
    commentError.value = error?.message || error?.msg || "加载我的评论失败";
  } finally {
    commentLoading.value = false;
  }
}

async function changeCommentPage(page) {
  if (page < 1 || (commentPages.value && page > commentPages.value)) return;
  await loadMyComments(page);
}

async function changePointPage(page) {
  if (page < 1 || (pointPages.value && page > pointPages.value)) return;
  await loadPointUsage(page);
}

async function handleDeleteComment(comment) {
  const itemName = comment.itemName || `商品${comment.itemId}`;
  if (!window.confirm(`确认删除你在《${itemName}》下的这条评论吗？`)) {
    return;
  }
  try {
    await deleteMyComment(comment.id);
    const nextPage = myComments.value.length === 1 && commentPageNo.value > 1 ? commentPageNo.value - 1 : commentPageNo.value;
    await loadMyComments(nextPage);
  } catch (error) {
    console.error("删除评论失败", error);
    alert(error?.message || error?.msg || "删除评论失败");
  }
}

function openAdd() {
  form.value = emptyForm();
  formError.value = "";
  isEdit.value = false;
  showModal.value = true;
}

function openEdit(addr) {
  form.value = { ...addr };
  formError.value = "";
  isEdit.value = true;
  showModal.value = true;
}

function closeModal() {
  showModal.value = false;
  formError.value = "";
}

async function submitForm() {
  formError.value = "";
  const { contact, mobile, province, city, street } = form.value;
  if (!contact?.trim()) {
    formError.value = "请填写联系人姓名";
    return;
  }
  if (!mobile?.trim()) {
    formError.value = "请填写手机号";
    return;
  }
  if (!/^1\d{10}$/.test(mobile.trim())) {
    formError.value = "请输入正确的 11 位手机号";
    return;
  }
  if (!province?.trim()) {
    formError.value = "请填写省份";
    return;
  }
  if (!city?.trim()) {
    formError.value = "请填写城市";
    return;
  }
  if (!street?.trim()) {
    formError.value = "请填写详细地址";
    return;
  }

  submitting.value = true;
  try {
    const payload = {
      ...form.value,
      contact: contact.trim(),
      mobile: mobile.trim(),
      province: province.trim(),
      city: city.trim(),
      town: form.value.town?.trim?.() || "",
      street: street.trim(),
      notes: form.value.notes?.trim?.() || ""
    };
    if (isEdit.value) {
      await updateAddress(payload);
    } else {
      await addAddress(payload);
    }
    closeModal();
    await loadAddresses();
  } catch (error) {
    console.error("保存地址失败", error);
    const msg = error?.message || error?.msg || "请检查网络或联系管理员";
    formError.value = `保存失败：${msg}`;
  } finally {
    submitting.value = false;
  }
}

onMounted(async () => {
  await Promise.all([loadMyPoints(), loadPointUsage(1), loadAddresses(), loadMyComments(1)]);
});
</script>

<style scoped>
:global(body.modal-open) {
  overflow: hidden;
}

.profile {
  margin-top: 28px;
}

h2 {
  margin: 0 0 20px;
  font: 700 clamp(28px, 3vw, 34px) var(--font-display);
}

h3 {
  margin: 0;
  font-size: 20px;
}

h4 {
  margin: 0;
  font-size: 20px;
  line-height: 1.2;
}

.section {
  padding: 22px 24px;
}

.point-section {
  margin-bottom: 18px;
}

.comment-section {
  margin-top: 18px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.tip,
.tip-inline {
  color: var(--text-sub);
}

.tip {
  padding: 14px 0;
}

.tip-inline {
  font-size: 14px;
}

.point-total {
  font-size: 26px;
  font-weight: 800;
  color: var(--brand);
}

.point-layout {
  display: grid;
  grid-template-columns: minmax(220px, 280px) minmax(0, 1fr);
  gap: 16px;
}

.point-panel {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 18px;
  border-radius: 18px;
  background: linear-gradient(135deg, #fff4e8 0%, #fffaf5 100%);
  border: 1px solid #ffd8c6;
}

.point-panel strong {
  font-size: clamp(34px, 4vw, 42px);
  line-height: 1;
  color: var(--brand);
}

.point-panel span,
.point-label {
  color: var(--text-sub);
}

.point-label {
  margin: 0;
  font-size: 13px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.point-records {
  padding: 2px 0;
}

.point-record-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.point-record-list,
.addr-list,
.comment-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.point-record-item,
.addr-item,
.comment-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 16px;
  border-radius: 14px;
  border: 1px solid #f0f0f0;
  background: #fff;
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
}

.point-record-item p {
  margin: 6px 0 0;
  color: var(--text-sub);
  font-size: 13px;
}

.point-record-item:hover,
.addr-item:hover,
.comment-item:hover {
  transform: translateY(-1px);
  border-color: #ffcfb8;
  box-shadow: 0 10px 22px rgba(255, 80, 0, 0.08);
}

.point-record-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  white-space: nowrap;
}

.point-record-side small {
  color: var(--text-sub);
}

.point-change {
  font-size: 20px;
  font-weight: 800;
}

.point-minus {
  color: #c0392b;
}

.point-pagination {
  margin-top: 14px;
}

.error-text {
  color: #c0392b;
}

.addr-info {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  flex: 1;
}

.addr-tag {
  padding: 3px 10px;
  border-radius: 999px;
  background: var(--bg-warm);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
}

.mobile {
  color: var(--text-sub);
  font-size: 14px;
}

.addr-text {
  width: 100%;
  margin: 2px 0 0;
  font-size: 14px;
  color: var(--text-sub);
}

.notes {
  width: 100%;
  margin: 0;
  font-size: 12px;
  color: color-mix(in srgb, var(--text-sub) 78%, #ffffff 22%);
}

.comment-item {
  display: block;
}

.comment-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.comment-top p {
  margin: 8px 0 0;
  color: var(--text-sub);
  font-size: 13px;
}

.comment-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.comment-content {
  margin: 14px 0 0;
  color: var(--text-main);
  line-height: 1.8;
  white-space: pre-wrap;
}

.modal-mask {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 120;
  padding: 22px;
  background: rgba(60, 60, 60, 0.38);
}

.modal-box {
  width: min(760px, 96vw);
  max-height: min(90vh, 760px);
  display: flex;
  flex-direction: column;
  padding: 0;
  overflow: hidden;
  border-radius: 18px;
}

.modal-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 24px 28px 16px;
  border-bottom: 1px solid var(--border);
}

.modal-sub {
  margin: 8px 0 0;
  color: var(--text-sub);
  font-size: 13px;
}

.icon-close {
  width: 36px;
  height: 36px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: #fff;
  color: var(--text-sub);
  font-size: 24px;
  line-height: 1;
  cursor: pointer;
  transition: all 0.2s ease;
}

.icon-close:hover {
  color: var(--brand);
  border-color: #ffcfb8;
  background: #fff7f2;
}

.form-error {
  margin: 14px 28px 0;
  border: 1px solid rgba(239, 68, 68, 0.24);
  border-radius: 12px;
  padding: 11px 14px;
  color: #b42318;
  background: rgba(254, 242, 242, 0.88);
  font-size: 13px;
}

.modal-body {
  padding: 18px 28px 10px;
  overflow-y: auto;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px 16px;
}

.form-row {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.form-row--span2 {
  grid-column: span 2;
}

.form-row label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-sub);
}

.form-row input:not([type="checkbox"]),
.form-row textarea {
  width: 100%;
  padding: 11px 12px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: #fff;
  color: var(--text-main);
  font: inherit;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
}

.form-row textarea {
  resize: vertical;
  min-height: 88px;
}

.form-row input:not([type="checkbox"]):focus,
.form-row textarea:focus {
  border-color: var(--brand);
  box-shadow: 0 0 0 4px rgba(255, 80, 0, 0.12);
  background: #fff;
}

.form-row--check {
  padding-top: 2px;
}

.form-row--check label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-weight: 500;
  color: var(--text-main);
}

.form-row--check input[type="checkbox"] {
  width: 16px;
  height: 16px;
  accent-color: var(--brand);
}

.req {
  color: #e5484d;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 28px 22px;
  border-top: 1px solid var(--border);
  background: #fffaf7;
}

.ghost {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 16px;
  border-radius: 18px;
  border: 1px solid #ffd2bf;
  color: var(--brand);
  background: #fff7f2;
  cursor: pointer;
  font: inherit;
  font-size: 14px;
  transition: all 0.2s ease;
}

.ghost:hover {
  background: #fff1e8;
  border-color: #ffbf9e;
}

.danger-btn:hover {
  background: rgba(239, 68, 68, 0.08);
  border-color: rgba(239, 68, 68, 0.35);
  color: #b42318;
}

.link-btn {
  text-decoration: none;
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

.btn-sm {
  padding: 8px 16px;
  font-size: 14px;
}

@media (max-width: 860px) {
  .modal-box {
    width: min(680px, 96vw);
  }

  .point-layout {
    grid-template-columns: 1fr;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .form-row--span2 {
    grid-column: span 1;
  }
}

@media (max-width: 640px) {
  .profile {
    margin-top: 18px;
  }

  .section {
    padding: 16px;
  }

  h2 {
    margin-bottom: 14px;
    font-size: 26px;
  }

  h3 {
    font-size: 18px;
  }

  h4 {
    font-size: 18px;
  }

  .section-header,
  .comment-top,
  .addr-item,
  .point-record-item,
  .comment-actions,
  .pagination {
    align-items: flex-start;
    flex-direction: column;
  }

  .point-record-side {
    align-items: flex-start;
  }

  .modal-mask {
    padding: 10px;
    align-items: flex-end;
  }

  .modal-box {
    width: 100%;
    max-height: 94vh;
    border-radius: 18px 18px 0 0;
  }

  .modal-head,
  .modal-body,
  .modal-actions {
    padding-left: 16px;
    padding-right: 16px;
  }

  .modal-head {
    padding-top: 16px;
  }

  .form-error {
    margin-left: 16px;
    margin-right: 16px;
  }

  .modal-actions {
    padding-bottom: 14px;
  }

  .modal-actions .ghost,
  .modal-actions .btn-primary {
    flex: 1;
    justify-content: center;
  }
}
</style>
