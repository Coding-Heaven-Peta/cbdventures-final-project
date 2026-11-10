/* Apply the saved appearance before the app paints. Storage may be unavailable
   in private browsing, in which case the switch still works for this visit. */
(() => {
  const key = 'cbdventures-theme';
  function apply(theme) {
    const selected = theme === 'light' ? 'light' : 'dark';
    document.documentElement.dataset.theme = selected;
    document.documentElement.style.colorScheme = selected;
    const themeColor = document.querySelector('meta[name="theme-color"]');
    if (themeColor) themeColor.content = selected === 'light' ? '#f9f7fc' : '#0b0b0d';
  }
  let saved;
  try { saved = localStorage.getItem(key); } catch { /* use the default */ }
  apply(saved);
  window.cbdSetTheme = (theme) => {
    apply(theme);
    try { localStorage.setItem(key, document.documentElement.dataset.theme); } catch { /* session only */ }
  };
})();
