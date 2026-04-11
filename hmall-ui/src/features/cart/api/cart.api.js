import client from "@/core/http/client";

/**
 * 获取购物车列表
 * 后端 GET /carts 返回 List<CartVO>
 */
export function fetchCartList() {
  return client.get("/carts");
}

/**
 * 添加商品到购物车
 * 后端 POST /carts，body: CartFormDTO { itemId, name, spec, price, image }
 * 注意：price 单位为分
 */
export function addToCart(payload) {
  return client.post("/carts", payload);
}

/**
 * 修改购物车商品（数量等）
 * 后端 PUT /carts，body: Cart 实体
 */
export function updateCartItem(payload) {
  return client.put("/carts", payload);
}

/**
 * 删除购物车中的商品
 * 后端 DELETE /carts/{id}
 */
export function removeCartItem(id) {
  return client.delete(`/carts/${id}`);
}
