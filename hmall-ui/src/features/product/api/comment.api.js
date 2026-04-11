import client from "@/core/http/client";

function normalizeComment(data) {
  return {
    id: data.id,
    userId: data.userId,
    itemId: data.itemId,
    itemName: data.itemName || "",
    username: data.username || "",
    content: data.content || "",
    createTime: data.createTime || "",
    updateTime: data.updateTime || ""
  };
}

function normalizePage(result) {
  const data = result?.data || result || {};
  const list = Array.isArray(data.list) ? data.list : [];
  return {
    total: Number(data.total || 0),
    pages: Number(data.pages || 0),
    list: list.map(normalizeComment)
  };
}

export async function fetchItemComments(itemId, pageNo = 1, pageSize = 10) {
  const result = await client.get(`/item-comments/item/${itemId}`, {
    params: { pageNo, pageSize }
  });
  return normalizePage(result);
}

export function createItemComment(data) {
  return client.post("/item-comments", data);
}

export async function fetchMyComments(pageNo = 1, pageSize = 10) {
  const result = await client.get("/item-comments/me", {
    params: { pageNo, pageSize }
  });
  return normalizePage(result);
}

export function deleteMyComment(commentId) {
  return client.delete(`/item-comments/${commentId}`);
}
