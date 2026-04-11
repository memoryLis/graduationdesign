import client from "@/core/http/client";

/**
 * 创建订单
 * 后端 POST /orders，body: OrderFormDTO { addressId, paymentType, details: [{itemId, num}] }
 * 返回 Long (订单id) — 注意：JS 中 Long 会丢精度，需要当字符串处理
 */
export async function createOrder(payload) {
  // 用 transformResponse 拿到原始文本，避免 JSON.parse 把大数字截断
  const res = await client.post("/orders", payload, {
    transformResponse: [(data) => data]
  });
  // res 此时是原始字符串，可能带引号也可能不带
  return String(res).replace(/"/g, "").trim();
}

/**
 * 根据 id 查询订单
 * 后端 GET /orders/{id} 返回 OrderVO
 */
export function fetchOrderById(id) {
  return client.get(`/orders/${id}`);
}
