# Frontend Theme Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Introduce light/dark dual-mode switching and reorganize CSS from a single 685-line `theme.css` into a modular token system (tokens / light / dark / theme layers).

**Architecture:** Three-layer CSS: `tokens.css` (base variables), `light.css` + `dark.css` (color themes with `--app-*` and `--el-*` overrides), `theme.css` (component tweaks + utility classes). Switching via `html.dark` class toggled by `useTheme.js` composable.

**Tech Stack:** Vue 3, Vite, Element Plus 2.x, native CSS custom properties

---

## Variable Rename Map

| Old Name | New Name |
|----------|----------|
| `--bg-primary` | `--app-bg-primary` |
| `--bg-secondary` | `--app-bg-secondary` |
| `--bg-card` | `--app-bg-card` |
| `--bg-card-hover` | `--app-bg-card-hover` |
| `--bg-elevated` | `--app-bg-elevated` |
| `--accent` | `--app-accent` |
| `--accent-light` | `--app-accent-light` |
| `--accent-dark` | `--app-accent-dark` |
| `--text-primary` | `--app-text-primary` |
| `--text-secondary` | `--app-text-secondary` |
| `--text-tertiary` | `--app-text-tertiary` |
| `--text-muted` | `--app-text-muted` |
| `--border-color` | `--app-border-color` |
| `--border-color-light` | `--app-border-color-light` |
| `--border-color-hover` | `--app-border-color-hover` |
| `--shadow-card` | `--app-shadow-card` |
| `--shadow-elevated` | `--app-shadow-elevated` |
| `--shadow-glow` | `--app-shadow-glow` |
| `--radius-sm` | `--app-radius-sm` |
| `--radius-md` | `--app-radius-md` |
| `--radius-lg` | `--app-radius-lg` |
| `--radius-xl` | `--app-radius-xl` |
| `--gradient-accent` | `--app-gradient-accent` |
| `--gradient-card` | `--app-gradient-card` |
| `--gradient-bg` | `--app-gradient-bg` |
| `--gradient-input` | `--app-gradient-input` |
| `--transition-fast` | `--app-transition-fast` |
| `--transition-normal` | `--app-transition-normal` |
| `--transition-slow` | `--app-transition-slow` |
| `--accent-glow` | literal `rgba(74,111,165,0.12)` |
| `--accent-glow-strong` | literal `rgba(74,111,165,0.25)` |
| `--blue-accent` | removed |
| `--teal-accent` | removed |

---

## Tasks Overview

### Task 1: Git branch setup
- `git checkout -b codex/frontend-theme-refactor`

### Task 2: tokens.css — Base variables (radius, shadow, font, spacing, transition, blur)

**Create:** `frontend/src/assets/styles/tokens.css`

All `:root` level, theme-independent. Full code in plan appendix.

### Task 3: dark.css — Dark theme colors (Slate Blue)

**Create:** `frontend/src/assets/styles/dark.css`

All dark `--app-*` and `--el-*` variables scoped under `html.dark { ... }`. Preserves existing `#0a0a0f` dark scheme.

### Task 4: light.css — Light theme colors (Warm Gray)

**Create:** `frontend/src/assets/styles/light.css`

All light `--app-*` and `--el-*` variables scoped under `:root { ... }`. Warm-gray palette: `--app-bg-primary: #f5f4f1`, accent stays `#4a6fa5`.

### Task 5: theme.css — Component overrides + utility classes

**Overwrite:** `frontend/src/assets/styles/theme.css`

Replace the 685-line file with ~250 lines. All color-dependent rules now reference `--app-*` / `--el-*` variables. Keeps: scrollbar, popper/dropdown, radio-button, menu, table, input, buttons, select, dialog, tag, pagination, message, loading, card, checkbox, switch, link, cascader, date/time picker, form, empty state + `.accent-text`, `.accent-gradient-text`, `.glass-card` utility classes.

### Task 6: index.css — Import chain

**Create:** `frontend/src/assets/styles/index.css`

```css
@import "./tokens.css";
@import "./light.css";
@import "./dark.css";
@import "./theme.css";
```

### Task 7: useTheme.js — Theme switching composable

**Create:** `frontend/src/composables/useTheme.js`

Key behavior:
- On first load: reads `localStorage('theme')`, fallback `prefers-color-scheme`
- `document.documentElement.classList.toggle('dark')` for switching
- Listens to OS preference changes only when user hasn't explicitly set
- Exports `{ isDark: Ref<boolean>, toggle: () => void }`

### Task 8: Update App.vue — Switch import

**Modify:** `frontend/src/App.vue`

Change `<style>` block from `@import './assets/styles/theme.css'` to `@import './assets/styles/index.css'`.

### Task 9: Update main.js — Remove EP default CSS

**Modify:** `frontend/src/main.js`

Remove `import 'element-plus/dist/index.css'` (our index.css chain now provides `--el-*` overrides).

### Task 10: Rename CSS variables in all 18 Vue/CSS files

Run PowerShell script to search-replace all old `var(--xxx)` references to `var(--app-xxx)` across:

- `frontend/src/components/Pagination.vue`
- `frontend/src/views/layout/MainLayout.vue`
- `frontend/src/views/admin/Dashboard.vue`
- `frontend/src/views/admin/UserManage.vue`
- `frontend/src/views/admin/RoleManage.vue`
- `frontend/src/views/admin/ModelManage.vue`
- `frontend/src/views/admin/ModelUsage.vue`
- `frontend/src/views/admin/AuditLogManage.vue`
- `frontend/src/views/admin/CloudAccounts.vue`
- `frontend/src/views/admin/CloudResources.vue`
- `frontend/src/views/admin/CloudResourceTypePage.vue`
- `frontend/src/views/admin/CloudTopology.vue`
- `frontend/src/views/auth/Login.vue`
- `frontend/src/views/auth/Register.vue`
- `frontend/src/views/auth/AdminLogin.vue`
- `frontend/src/views/front/ChatView.vue`
- `frontend/src/views/front/ModelList.vue`
- `frontend/src/views/user/UserProfile.vue`

Verify with: `rg 'var\(--(bg-|accent\b|text-|border-|shadow-|radius-|gradient-|transition-)'` → expected empty.

### Task 11: Verify npm build

```bash
cd frontend && npm run build
```
Expected: zero errors.

### Task 12: Theme toggle in MainLayout

**Modify:** `frontend/src/views/layout/MainLayout.vue`

- Import `Sunny, Moon` from `@element-plus/icons-vue`
- Import `useTheme` composable
- Add `<button class="theme-toggle-btn">` with `Sunny`/`Moon` icon in topbar
- Style: 36x36px, border, hover accent color

### Task 13: Theme init for auth pages

**Modify:** `Login.vue`, `Register.vue`, `AdminLogin.vue`

Add `import { useTheme } from '@/composables/useTheme'; useTheme();` to each `<script setup>` to prevent FOUC on pages outside MainLayout.

### Task 14: Final verification

1. `npm run build` — zero errors
2. `npm run dev` — spot-check 5 core pages in both modes
3. `mvn compile` — backend sanity check

### Task 15: Merge to master

```bash
git checkout master
git merge codex/frontend-theme-refactor
git push gitee master
git push github master
```
