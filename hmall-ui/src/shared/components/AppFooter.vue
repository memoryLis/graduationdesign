<template>
  <footer class="footer">
    <div class="container footer-inner">
      <p class="brand">🏪 小楼商城</p>
      <p class="copy">© {{ new Date().getFullYear() }} 小楼商城 · 让每一次下单都更有仪式感</p>
    </div>
  </footer>
</template>

<style scoped>
.footer {
  margin-top: 56px;
  padding: 34px 0;
  border-top: 1px solid var(--border);
  background: #fff;
}

.footer-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.brand {
  margin: 0;
  font: 700 18px var(--font-display);
  color: var(--brand);
}

.copy {
  margin: 0;
  color: var(--text-sub);
  font-size: 13px;
  text-align: center;
}

@media (max-width: 640px) {
  .footer {
    margin-top: 42px;
    padding: 24px 0;
  }

  .brand {
    font-size: 16px;
  }

  .copy {
    font-size: 12px;
  }
}
</style>
