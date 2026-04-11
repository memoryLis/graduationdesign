import { defineStore } from "pinia";
import { addToCart, fetchCartList, removeCartItem, updateCartItem } from "@/features/cart/api/cart.api";
import { formatPrice } from "@/shared/utils";

export const useCartStore = defineStore("cart", {
  state: () => ({
    items: []
  }),
  getters: {
    totalCount: (state) => state.items.reduce((sum, i) => sum + Number(i.num || 0), 0),
    totalAmount: (state) =>
      state.items.reduce((sum, i) => sum + Number(i.price || 0) * Number(i.num || 0), 0),
    // 选中的商品
    selectedItems: (state) => state.items.filter((i) => i.selected),
    selectedCount: (state) => state.items.filter((i) => i.selected).reduce((sum, i) => sum + Number(i.num || 0), 0),
    selectedAmount: (state) =>
      state.items.filter((i) => i.selected).reduce((sum, i) => sum + Number(i.price || 0) * Number(i.num || 0), 0)
  },
  actions: {
    async loadCart() {
      try {
        const result = await fetchCartList();
        // 后端返回 List<CartVO>，直接是数组
        const rows = Array.isArray(result) ? result : (result?.list || result?.data?.list || result?.data || []);
        this.items = rows.map((x) => ({
          id: x.id,
          itemId: x.itemId || x.id,
          isHot: x.isHot || 0,
          name: x.name,
          price: formatPrice(x.price),
          priceFen: x.price,
          num: x.num || x.quantity || 1,
          image: x.image || "",
          spec: x.spec || "",
          selected: true
        }));
      } catch {
        this.items = [];
      }
    },
    async addCart(itemId, num = 1, productInfo) {
      // 后端 CartFormDTO 需要 itemId, name, spec, price(分), image
      const payload = { itemId, num };
      if (productInfo) {
        payload.name = productInfo.name || "";
        payload.spec = productInfo.spec || "";
        payload.price = productInfo.priceFen || Math.round((productInfo.price || 0) * 100);
        payload.image = productInfo.image || "";
        payload.isHot = productInfo.hot ? 1 : 0;
      }
      await addToCart(payload);
      await this.loadCart();
    },
    async updateNum(cartItem, newNum) {
      try {
        await updateCartItem({ id: cartItem.id, num: newNum });
        cartItem.num = newNum;
      } catch {
        // 静默失败，保持原数量
      }
    },
    async remove(id) {
      try {
        await removeCartItem(id);
      } finally {
        this.items = this.items.filter((x) => x.id !== id);
      }
    },
    toggleSelect(id) {
      const item = this.items.find((x) => x.id === id);
      if (item) item.selected = !item.selected;
    },
    toggleSelectAll(val) {
      this.items.forEach((i) => (i.selected = val));
    }
  }
});
