import client from "@/core/http/client";

export async function fetchMyBrowseHistory(pageNo = 1, pageSize = 10) {
  const result = await client.get("/browse-history/my", {
    params: { pageNo, pageSize }
  });
  const data = result?.data || result || {};
  return {
    total: Number(data.total || 0),
    pages: Number(data.pages || 0),
    list: Array.isArray(data.list) ? data.list : []
  };
}

export async function recordBrowse(itemId) {
  return client.post(`/browse-history/record/${itemId}`);
}
