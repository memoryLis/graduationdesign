import client from "@/core/http/client";

export const fetchMyPoints = () => client.get("/users/me/points");

export const changePassword = (data) => client.put("/users/password", data);

export const dailySign = () => client.post("/users/sign");
