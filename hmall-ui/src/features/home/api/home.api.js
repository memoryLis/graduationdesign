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

export async function getRecommendProducts() {
  const result = await client.get("/hotItem/listHotItems", {
    params: { current: 1, size: 8 }
  });
  const rows = result?.list || result?.records || result?.data?.list || [];
  return rows.map(mapProduct);
}
