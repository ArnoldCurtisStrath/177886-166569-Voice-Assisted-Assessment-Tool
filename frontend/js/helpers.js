/**
 * Tiny shared helpers used across pages.
 * Load this BEFORE api.js / auth.js / router.js in index.html.
 */

// escape a string for safe interpolation into innerHTML
function escapeHtml(str) {
  if (str === null || str === undefined) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

// turn a date string into something readable; fall back to raw input
function fmtDate(str) {
  if (!str) return '-';
  var d = new Date(str);
  if (isNaN(d.getTime())) return String(str);
  return d.toLocaleDateString('en-KE', { year: 'numeric', month: 'short', day: 'numeric' });
}

// score (out of max, default 4) -> percentage; null stays '-'
function fmtPct(score, max) {
  if (score === null || score === undefined || isNaN(score)) return '-';
  max = max || 4;
  return Math.round((score / max) * 100);
}
