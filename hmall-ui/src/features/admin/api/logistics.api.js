import client from "@/core/http/client";

export const shipOrder = (orderId, data) => client.post(`/logistics/ship/${orderId}`, data);

export const queryLogistics = (orderId) => client.get(`/logistics/${orderId}`);

export const queryAllPayOrders = (current = 1, size = 10) => {
  return client.get("/pay-orders/admin/all", { params: { current, size } });
};
