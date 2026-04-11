import client from "@/core/http/client";

export const fetchMyPoints = () => client.get("/users/me/points");
