import client from "@/core/http/client";

/**
 * 查询当前用户收货地址列表
 * 后端 GET /addresses 返回 List<AddressDTO>
 */
export function fetchAddressList() {
  return client.get("/addresses");
}

/**
 * 根据 id 查询地址
 * 后端 GET /addresses/{addressId}
 */
export function fetchAddressById(addressId) {
  return client.get(`/addresses/${addressId}`);
}

/**
 * 添加收货地址
 * 后端 POST /addresses/addAddress
 */
export function addAddress(data) {
  return client.post("/addresses/addAddress", data);
}

/**
 * 修改收货地址
 * 后端 POST /addresses/updateAddress
 */
export function updateAddress(data) {
  return client.post("/addresses/updateAddress", data);
}
