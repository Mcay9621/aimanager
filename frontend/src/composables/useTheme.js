import { ref } from "vue";

const THEME_KEY = "theme";
const DARK_CLASS = "dark";

const isDark = ref(false);

export function useTheme() {
  function getInitialTheme() {
    const stored = localStorage.getItem(THEME_KEY);
    if (stored === "dark" || stored === "light") {
      return stored;
    }
    return window.matchMedia("(prefers-color-scheme: dark)").matches
      ? "dark"
      : "light";
  }

  function applyTheme(theme) {
    isDark.value = theme === "dark";
    document.documentElement.classList.toggle(DARK_CLASS, isDark.value);
    localStorage.setItem(THEME_KEY, theme);
  }

  // Initialize on first module load
  const initial = getInitialTheme();
  applyTheme(initial);

  // Watch for system preference changes only if user has not explicitly set
  const mediaQuery = window.matchMedia("(prefers-color-scheme: dark)");
  mediaQuery.addEventListener("change", (e) => {
    const stored = localStorage.getItem(THEME_KEY);
    if (!stored) {
      applyTheme(e.matches ? "dark" : "light");
    }
  });

  function toggle() {
    applyTheme(isDark.value ? "light" : "dark");
  }

  return { isDark, toggle };
}
