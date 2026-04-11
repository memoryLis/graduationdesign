import client from "@/core/http/client";

export function registerByPassword(payload) {
  return client.post("/users/register", {
    username: payload.username,
    password: payload.password,
    phone: payload.phone || undefined
  });
}
