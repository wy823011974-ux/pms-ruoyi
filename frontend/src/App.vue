<template>
  <router-view />
</template>

<script setup>
</script>

<style>
/* 代码/编号标签 */
code, .mono {
  font-family: var(--font-mono); font-size: 12px;
  background: var(--color-primary-light); color: var(--color-primary);
  padding: 2px 8px; border-radius: 4px; font-weight: 500;
}

/* 过渡动画 */
.fade-enter-active, .fade-leave-active { transition: opacity 200ms ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
