(function() {
  const STORAGE_KEY = 'episodive-ds-theme';

  function getPreferredTheme() {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) return stored;
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  }

  function applyTheme(theme) {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem(STORAGE_KEY, theme);

    // Update toggle button text
    const btn = document.querySelector('.theme-toggle-btn');
    if (btn) {
      btn.innerHTML = theme === 'dark'
        ? '☀️ Light Mode'
        : '🌙 Dark Mode';
    }
  }

  function toggleTheme() {
    const current = document.documentElement.getAttribute('data-theme') || 'light';
    applyTheme(current === 'dark' ? 'light' : 'dark');
  }

  // Init
  applyTheme(getPreferredTheme());

  // Attach to button
  document.addEventListener('click', (e) => {
    if (e.target.closest('.theme-toggle-btn')) {
      toggleTheme();
    }
  });

  window.ThemeToggle = { toggle: toggleTheme, apply: applyTheme };
})();
