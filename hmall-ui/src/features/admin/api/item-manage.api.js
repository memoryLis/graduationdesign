import client from "@/core/http/client";

export const getItemList = (pageNo = 1, pageSize = 5, name = "") => {
  return client.get("/items/manage/list", { params: { pageNo, pageSize, name } });
};

export const addItem = (formData) => {
  return client.post("/items/manage/add", formData, {
    headers: { "Content-Type": "multipart/form-data" }
  });
};

export const updateItem = (formData) => {
  return client.put("/items/manage/update", formData, {
    headers: { "Content-Type": "multipart/form-data" }
  });
};

export const deleteItem = (id) => {
  return client.delete(`/items/manage/delete/${id}`);
};

export const addHotItem = (itemId) => {
  return client.post("/items/manage/addHotItem", null, { params: { itemId } });
};

export const cancelHotItem = (itemId) => {
  return client.post("/items/manage/cancelHotItem", null, { params: { itemId } });
};
