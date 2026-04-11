import client from "@/core/http/client";

/**
 * 生成支付单
 * 后端 POST /pay-orders，返回 String (支付单id)
 * 注意：支付单 ID 是 Long 雪花ID，JS Number 会丢精度，必须当字符串处理
 */
export async function applyPayOrder(payload) {
  // 用 transformResponse 拿到原始文本，避免 JSON.parse 把大数字截断
  const res = await client.post("/pay-orders", payload, {
    transformResponse: [(data) => data]
  });
  // res 此时是原始字符串，去掉可能的引号
  return String(res).replace(/"/g, "").trim();
}

/**
 * 余额支付
 * 后端 POST /pay-orders/{id}，body: PayOrderFormDTO { pw }
 * payOrderId 必须是字符串，避免精度丢失
 */
export function payByBalance(payOrderId, pw) {
  return client.post(`/pay-orders/${payOrderId}`, { pw });
}
