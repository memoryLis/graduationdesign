<template>
  <div>
    <AppHeader />
    <main class="container">
      <RouterView />
    </main>
    <AppFooter />
  </div>
</template>

<script setup>
import AppHeader from "@/shared/components/AppHeader.vue";
import AppFooter from "@/shared/components/AppFooter.vue";
</script>
