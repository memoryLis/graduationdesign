import client from "@/core/http/client";

export const getUserList = (pageNo = 1, pageSize = 10, keyword = "") => {
  return client.get("/users/admin/list", {
    params: { pageNo, pageSize, keyword }
  });
};

export const freezeUser = (id) => client.put(`/users/admin/${id}/freeze`);

export const unfreezeUser = (id) => client.put(`/users/admin/${id}/unfreeze`);

export const deleteUser = (id) => client.delete(`/users/admin/${id}`);

export const rechargeUserPoints = (id, payload) => {
  return client.post(`/users/admin/${id}/points/recharge`, payload);
};
