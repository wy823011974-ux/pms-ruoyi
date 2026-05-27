<template>
  <router-view />
</template>

<script setup>
</script>

<style>
/* === 全局设计基础 === */

/* 系统字体栈 — Product Register 标准 */
:root {
  --font-sans: -apple-system, BlinkMacSystemFont, "Segoe UI", system-ui, sans-serif;
  --font-mono: "SF Mono", "Cascadia Code", "Consolas", monospace;

  /* 间距节奏 — 4px 基础单位 */
  --space-1: 4px;
  --space-2: 8px;
  --space-3: 16px;
  --space-4: 24px;
  --space-5: 32px;

  /* 语义色彩 Token */
  --color-bg: #f0f2f5;
  --color-surface: #ffffff;
  --color-border: #e8eaed;
  --color-text: #303133;
  --color-text-secondary: #909399;
}

* { box-sizing: border-box; }

body {
  margin: 0; padding: 0;
  font-family: var(--font-sans);
  font-size: 14px; line-height: 1.6;
  color: var(--color-text);
  background: var(--color-bg);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

/* 统一页面标题 */
.page-title {
  font-size: 20px; font-weight: 600; color: #303133;
  margin: 0 0 0; line-height: 1.4;
}

/* 统一卡片间距 */
.el-card { margin-bottom: 16px; }
.el-card__header { padding: 14px 20px; font-weight: 600; }

/* 统一表格样式 */
.el-table { font-size: 13px; }
.el-table th.el-table__cell { background: #fafafa; color: #606266; font-weight: 500; }

/* 统一按钮组间距 */
.el-button + .el-button { margin-left: 8px; }

/* 空状态 */
.el-empty__description { color: #909399; }

/* 标签页 */
.el-tabs__nav-wrap::after { height: 1px; }

/* 过渡动画 */
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* 代码/编号 */
code, .mono {
  font-family: var(--font-mono); font-size: 12px;
  background: #f0f2f5; color: #409eff;
  padding: 1px 6px; border-radius: 3px;
}

/* 状态标签色 */
.status-active { color: #409eff; }
.status-success { color: #67c23a; }
.status-archived { color: #909399; }
</style>
