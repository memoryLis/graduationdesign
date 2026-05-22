import client from "@/core/http/client";
import { formatPrice } from "@/shared/utils";

function mapProduct(x) {
  return {
    id: x.id,
    name: x.name,
    price: formatPrice(x.price),
    priceFen: x.price,
    image: x.image || "",
    spec: x.spec || "",
    description: x.category || "",
    stock: x.stock || 0,
    sold: x.sold || 0
  };
}

export async function searchProducts(keyword, pageNo = 1, pageSize = 20, category = "") {
  const params = { pageNo, pageSize };
  if (keyword) params.key = keyword;
  if (category) params.category = category;
  const result = await client.get("/search/list", { params });
  const rows = result?.list || result?.items || result?.records || result?.data?.list || [];
  const total = result?.total || result?.data?.total || 0;
  const pages = result?.pages || result?.data?.pages || Math.ceil(total / pageSize);
  return {
    list: rows.map(mapProduct),
    total,
    pages
  };
}

export async function getCategories() {
  const result = await client.get("/search/categories");
  return Array.isArray(result) ? result : (result?.data || []);
}

export async function getGuessYouLikeProducts() {
  const result = await client.get("/search/searchUserLike");
  const rows = Array.isArray(result) ? result : (result?.data || []);
  return rows.map(mapProduct);
}
