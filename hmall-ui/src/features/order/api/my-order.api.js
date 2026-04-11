import client from "@/core/http/client";

export const queryMyPayOrders = (current = 1, size = 10) => {
  return client.get("/pay-orders", {
    params: { current, size }
  });
};

export const queryPayOrderDetail = (payOrderId) => {
  return client.get("/pay-orders/getPayOrderDetail", {
    params: { id: payOrderId }
  });
};
