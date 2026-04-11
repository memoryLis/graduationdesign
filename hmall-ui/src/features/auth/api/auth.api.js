import client from "@/core/http/client";

export function loginByPassword(payload) {
  return client.post("/users/login", {
    username: payload.username,
    password: payload.password,
    rememberMe: false
  });
}
