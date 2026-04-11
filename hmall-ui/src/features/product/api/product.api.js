import client from "@/core/http/client";
import { formatPrice } from "@/shared/utils";

/**
 * 获取商品详情
 * 后端 GET /items/{id} 返回 ItemDTO
 */
export async function getProductDetail(id) {
  const result = await client.get(`/items/${id}`);
  const data = result?.data || result;
  return {
    id: data.id,
    name: data.name,
    price: formatPrice(data.price),
    priceFen: data.price,
    image: data.image || "",
    spec: data.spec || "",
    category: data.category || "",
    brand: data.brand || "",
    stock: data.stock || 0,
    sold: data.sold || 0,
    commentCount: data.commentCount || 0
  };
}
