import client from "@/core/http/client";

export const addFavorite = (itemId) => client.post(`/favorites/${itemId}`);

export const removeFavorite = (itemId) => client.delete(`/favorites/${itemId}`);

export const fetchMyFavorites = () => client.get("/favorites");

export const checkFavorited = (itemId) => client.get(`/favorites/check/${itemId}`);
