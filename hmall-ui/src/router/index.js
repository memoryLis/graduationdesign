import { createRouter, createWebHistory } from "vue-router";
import MainLayout from "@/layouts/MainLayout.vue";
import { getStoredUserInfo, isAdminUser } from "@/shared/utils";

const authRequired = ["cart", "order-confirm", "pay", "pay-success", "profile", "my-orders", "admin-items", "admin-users"];
const adminRequired = ["admin-items", "admin-users"];

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior() {
    return { top: 0 };
  },
  routes: [
    {
      path: "/",
      component: MainLayout,
      children: [
        {
          path: "",
          name: "home",
          component: () => import("@/features/home/views/HomeView.vue")
        },
        {
          path: "search",
          name: "search",
          component: () => import("@/features/search/views/SearchView.vue")
        },
        {
          path: "product/:id",
          name: "product-detail",
          component: () => import("@/features/product/views/ProductDetailView.vue")
        },
        {
          path: "cart",
          name: "cart",
          component: () => import("@/features/cart/views/CartView.vue")
        },
        {
          path: "order-confirm",
          name: "order-confirm",
          component: () => import("@/features/order/views/OrderConfirmView.vue")
        },
        {
          path: "pay",
          name: "pay",
          component: () => import("@/features/order/views/PayView.vue")
        },
        {
          path: "pay-success",
          name: "pay-success",
          component: () => import("@/features/order/views/PaySuccessView.vue")
        },
        {
          path: "profile",
          name: "profile",
          component: () => import("@/features/user/views/ProfileView.vue")
        },
        {
          path: "my-orders",
          name: "my-orders",
          component: () => import("@/features/order/views/MyOrdersView.vue")
        },
        {
          path: "admin/items",
          name: "admin-items",
          component: () => import("@/features/admin/views/ItemManageView.vue")
        },
        {
          path: "admin/users",
          name: "admin-users",
          component: () => import("@/features/admin/views/UserManageView.vue")
        }
      ]
    },
    {
      path: "/login",
      name: "login",
      component: () => import("@/features/auth/views/LoginView.vue")
    },
    {
      path: "/register",
      name: "register",
      component: () => import("@/features/auth/views/RegisterView.vue")
    }
  ]
});

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem("xl_token");
  const user = getStoredUserInfo();
  if (authRequired.includes(to.name) && !token) {
    next({ path: "/login", query: { redirect: to.fullPath } });
    return;
  }
  if (adminRequired.includes(to.name) && !isAdminUser(user)) {
    next("/");
    return;
  }
  next();
});

export default router;
