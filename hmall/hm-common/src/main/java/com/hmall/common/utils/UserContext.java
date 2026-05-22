package com.hmall.common.utils;

public class UserContext {
    private static final ThreadLocal<Long> tl = new ThreadLocal<>();
    private static final ThreadLocal<String> usernameTl = new ThreadLocal<>();

    public static void setUser(Long userId) {
        tl.set(userId);
    }

    public static Long getUser() {
        return tl.get();
    }

    public static void setUsername(String username) {
        usernameTl.set(username);
    }

    public static String getUsername() {
        return usernameTl.get();
    }

    public static boolean isAdmin() {
        return "123".equals(usernameTl.get());
    }

    public static void removeUser(){
        tl.remove();
        usernameTl.remove();
    }
}
