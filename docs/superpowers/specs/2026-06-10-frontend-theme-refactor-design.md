# Frontend Theme Refactor Design

> Date: 2026-06-10 | Status: Design Approved
> Branch: `codex/frontend-theme-refactor`
>
> Refactor the AI Manager frontend styles: introduce light/dark dual-mode switching,
> reorganize CSS architecture from a single 685-line `theme.css` into a modular token system,
> and adopt a warm-gray neutral light theme alongside the existing Slate Blue dark theme.

## Goals

- Light/dark dual-mode with user-toggleable switch
- Light theme: warm-gray neutral, low-saturation, professional feel
- Dark theme: preserve existing Slate Blue (`#4a6fa5`) tone with variable cleanup
- CSS architecture: split single file into `tokens.css` / `light.css` / `dark.css` / `theme.css`
- Use Element Plus dark-mode API (`html.dark` class) as switching mechanism

## Non-Goals

- No new dependencies (Sass, Tailwind, UnoCSS)
- No backend changes
- No view business logic refactoring

## Architecture

### File Structure

```
frontend/src/assets/styles/
├── tokens.css      # Base variables (radius / shadow / font / spacing / transition)
├── light.css       # Light mode: --app-* colors + --el-* overrides
├── dark.css        # Dark mode: --app-* colors + --el-* overrides
├── theme.css       # EP component fine-tunes + utility classes (~100 lines)
└── index.css       # Entry: @import tokens → light/dark → theme
```

### Loading Chain

- `App.vue` changes from `@import './assets/styles/theme.css'` to `@import './assets/styles/index.css'`
- `main.js` may drop `import 'element-plus/dist/index.css'` since index.css covers EP variable overrides

### Three-Layer Token System

| Layer | File | Content | Theme-dependent? |
|-------|------|---------|-----------------|
| Base | tokens.css | radius, shadow, font, spacing, transition | No |
| Color | light.css / dark.css | `--app-bg-page`, `--app-text-primary`, `--el-color-*`, etc. | Yes |
| Component | theme.css | EP component tweaks, scrollbar, `.glass-card`, `.accent-text` | No (uses vars) |

### Switching Mechanism

- `document.documentElement.classList.toggle('dark')` on `<html>` element
- Element Plus auto-detects `html.dark` and applies its built-in dark variables
- `--app-*` custom variables use `:root` / `html.dark` selectors
- New composable: `src/composables/useTheme.js`
  - Initialize: read `localStorage('theme')`, fallback to `prefers-color-scheme`
  - Toggle: `classList.toggle('dark')` + persist to localStorage
  - Expose reactive `isDark` ref for components

### Theme Switch UI

- Toggle button in `MainLayout.vue` topbar (right side)
- Icon: sun/moon from Element Plus Icons
- Tooltip on hover

## Color Palette

### Light Theme (warm-gray neutral)

| Token | Value | Usage |
|-------|-------|-------|
| `--app-bg-page` | `#f5f4f1` | Page background |
| `--app-bg-card` | `#ffffff` | Card/surface background |
| `--app-bg-elevated` | `#ffffff` | Dropdowns, dialogs |
| `--app-text-primary` | `#1a1a1a` | Primary text |
| `--app-text-secondary` | `rgba(26,26,26,0.7)` | Secondary text |
| `--app-text-tertiary` | `rgba(26,26,26,0.45)` | Placeholder / muted |
| `--app-accent` | `#4a6fa5` | Primary action color |
| `--app-border` | `rgba(0,0,0,0.08)` | Borders |

### Dark Theme (Slate Blue, preserved)

Current `#0a0a0f` base with `#4a6fa5` accent, variable names unified to `--app-*` prefix.

## Migration Plan

### Step 1 — New file skeleton
- Create `tokens.css`, `light.css`, `dark.css`, `index.css`
- Create `useTheme.js`
- Switch `App.vue` import to `index.css`
- `dark.css` copies existing variable values → no visual change

### Step 2 — Rename variables
- `--bg-primary` → `--app-bg-primary`, `--text-primary` → `--app-text-primary`, etc.
- Grep 17 views + 1 component, replace all `var(--xxx)` references
- Verify with `npm run build`

### Step 3 — Light theme values
- Fill `light.css` with warm-gray palette
- Verify 5 core pages: Dashboard, ModelList, ChatView, UserManage, Login
- Tune contrast and readability

### Step 4 — Toggle switch & cleanup
- Add toggle button in `MainLayout.vue`
- Delete old `theme.css`
- Final verification: `mvn compile` + `npm run build`

## Risks & Mitigations

| Risk | Likelihood | Mitigation |
|------|-----------|------------|
| Missed `var(--`) → undefined values | Medium | `rg -- var\(--` search covers all files |
| EP light-mode component styling clashes | Medium | Per-page walkthrough in Step 3 |
| Old `--el-*` overrides conflict with EP defaults | Low | Add `html:not(.dark)` qualifier where needed |
| Login/Register pages render outside MainLayout | Low | Include theme init in those pages too |

## Rollback

All work on branch `codex/frontend-theme-refactor`. Revert by switching back to master.

## File Change Summary

| Action | File |
|--------|------|
| **Create** | `frontend/src/assets/styles/tokens.css` |
| **Create** | `frontend/src/assets/styles/light.css` |
| **Create** | `frontend/src/assets/styles/dark.css` |
| **Create** | `frontend/src/assets/styles/index.css` |
| **Create** | `frontend/src/composables/useTheme.js` |
| **Modify** | `frontend/src/App.vue` |
| **Modify** | `frontend/src/views/layout/MainLayout.vue` |
| **Modify** | `frontend/src/views/auth/Login.vue` (theme init) |
| **Modify** | `frontend/src/views/auth/Register.vue` (theme init) |
| **Modify** | `frontend/src/views/auth/AdminLogin.vue` (theme init) |
| **Modify** | 14 remaining views (variable rename only) |
| **Modify** | `frontend/src/components/Pagination.vue` (variable rename) |
| **Delete** | `frontend/src/assets/styles/theme.css` |
