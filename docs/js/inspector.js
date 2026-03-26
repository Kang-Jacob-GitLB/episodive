/**
 * Episodive Design System — Figma-like Inspector Engine
 *
 * Attaches to any element with a [data-inspect] attribute and provides:
 *   - Hover highlight with dimension labels
 *   - Click-to-select with property panel
 *   - Alt+hover spacing measurement between elements
 *   - Padding / margin overlay visualization
 *
 * Data attributes consumed:
 *   data-inspect   — Component name (required to be inspectable)
 *   data-w         — Explicit width override (e.g. "360dp")
 *   data-h         — Explicit height override (e.g. "48dp")
 *   data-padding   — Padding spec (e.g. "16" or "16,8,16,8")
 *   data-margin    — Margin spec (e.g. "8" or "8,16,8,16")
 *   data-corner    — Corner radius (e.g. "12dp")
 *   data-color     — Color token name (e.g. "primary")
 *   data-font      — Typography style (e.g. "title-medium")
 *   data-gap       — Gap between children (e.g. "8dp")
 *   Any other data-* attributes are listed in the panel.
 */
(function () {
  'use strict';

  /* ========================================================================
     State
     ======================================================================== */
  let inspectorActive = false;
  let selectedEl = null;      // Currently selected (pinned) element
  let hoveredEl = null;       // Currently hovered inspectable element
  let altHeld = false;        // Whether Alt key is pressed

  /* ========================================================================
     Overlay DOM — created once, reused
     ======================================================================== */
  const overlay = {
    highlight: createDiv('inspector-highlight'),
    dimension: createDiv('inspector-dimension'),
    padding: createDiv('inspector-padding'),
    margin: createDiv('inspector-margin'),
    panel: createDiv('inspector-panel'),
    spacingContainer: createDiv('inspector-spacing-container'),
  };

  // Append all overlays to body once DOM is ready
  function mountOverlays() {
    const frag = document.createDocumentFragment();
    Object.values(overlay).forEach(el => frag.appendChild(el));
    document.body.appendChild(frag);
  }

  /* ========================================================================
     Toggle Button
     ======================================================================== */
  function createToggleButton() {
    const btn = document.createElement('button');
    btn.className = 'inspector-toggle';
    btn.innerHTML = '\uD83D\uDD0D Inspector';
    btn.addEventListener('click', toggleInspector);
    document.body.appendChild(btn);
    return btn;
  }

  let toggleBtn = null;

  /* ========================================================================
     Init — called on DOMContentLoaded
     ======================================================================== */
  function init() {
    mountOverlays();
    toggleBtn = createToggleButton();
    hideAll();

    // Global listeners (always bound; gated by inspectorActive flag)
    document.addEventListener('mousemove', onMouseMove, true);
    document.addEventListener('click', onClick, true);
    document.addEventListener('keydown', onKeyDown, true);
    document.addEventListener('keyup', onKeyUp, true);
    window.addEventListener('scroll', onScroll, true);
    window.addEventListener('resize', onScroll, true);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }

  /* ========================================================================
     Toggle
     ======================================================================== */
  function toggleInspector() {
    inspectorActive = !inspectorActive;
    toggleBtn.classList.toggle('active', inspectorActive);
    toggleBtn.innerHTML = inspectorActive
      ? '\u2716 Inspector'
      : '\uD83D\uDD0D Inspector';
    document.body.classList.toggle('inspector-active', inspectorActive);

    if (!inspectorActive) {
      deselect();
      hideAll();
    }
  }

  /* ========================================================================
     Mouse Move — hover highlight
     ======================================================================== */
  function onMouseMove(e) {
    if (!inspectorActive) return;

    const target = findInspectable(e.target);

    // Ignore if hovering over the panel or toggle button
    if (isInsideOverlay(e.target)) {
      hideHoverOverlays();
      hoveredEl = null;
      return;
    }

    if (target === hoveredEl) return;
    hoveredEl = target;

    if (!hoveredEl) {
      hideHoverOverlays();
      return;
    }

    // If Alt is held and we have a selected element, show spacing rulers
    if (altHeld && selectedEl && hoveredEl !== selectedEl) {
      showSpacingBetween(selectedEl, hoveredEl);
    } else {
      clearSpacing();
    }

    showHighlight(hoveredEl);
    showDimension(hoveredEl);
    showPaddingOverlay(hoveredEl);
    showMarginOverlay(hoveredEl);
  }

  /* ========================================================================
     Click — select / deselect
     ======================================================================== */
  function onClick(e) {
    if (!inspectorActive) return;

    // Ignore clicks on the toggle button or panel
    if (isInsideOverlay(e.target)) return;

    const target = findInspectable(e.target);

    if (target) {
      e.preventDefault();
      e.stopPropagation();
      select(target);
    } else {
      deselect();
    }
  }

  /* ========================================================================
     Keyboard
     ======================================================================== */
  function onKeyDown(e) {
    if (e.key === 'Alt') {
      altHeld = true;
      e.preventDefault();
    }
    if (e.key === 'Escape' && inspectorActive) {
      deselect();
    }
  }

  function onKeyUp(e) {
    if (e.key === 'Alt') {
      altHeld = false;
      clearSpacing();
    }
  }

  /* ========================================================================
     Scroll / Resize — update positions
     ======================================================================== */
  function onScroll() {
    if (!inspectorActive) return;
    if (hoveredEl) {
      showHighlight(hoveredEl);
      showDimension(hoveredEl);
      showPaddingOverlay(hoveredEl);
      showMarginOverlay(hoveredEl);
    }
    if (selectedEl) {
      showHighlight(selectedEl);
      showDimension(selectedEl);
      updatePanelPosition();
      if (altHeld && hoveredEl && hoveredEl !== selectedEl) {
        showSpacingBetween(selectedEl, hoveredEl);
      }
    }
  }

  /* ========================================================================
     Selection
     ======================================================================== */
  function select(el) {
    selectedEl = el;
    showHighlight(el);
    showDimension(el);
    showPaddingOverlay(el);
    showMarginOverlay(el);
    showPanel(el);
    showSiblingSpacing(el);
  }

  function deselect() {
    selectedEl = null;
    hideAll();
  }

  /* ========================================================================
     Highlight Overlay
     ======================================================================== */
  function showHighlight(el) {
    const r = el.getBoundingClientRect();
    positionBox(overlay.highlight, r);
    overlay.highlight.style.display = 'block';
  }

  /* ========================================================================
     Dimension Label
     ======================================================================== */
  function showDimension(el) {
    const r = el.getBoundingClientRect();
    const w = el.getAttribute('data-w') || Math.round(r.width) + 'px';
    const h = el.getAttribute('data-h') || Math.round(r.height) + 'px';

    overlay.dimension.textContent = w + ' \u00D7 ' + h;
    overlay.dimension.style.display = 'block';

    // Position above the element, centered
    const dimRect = overlay.dimension.getBoundingClientRect();
    let left = r.left + (r.width - dimRect.width) / 2;
    let top = r.top - dimRect.height - 4;

    // Keep on-screen
    if (top < 0) top = r.bottom + 4;
    if (left < 0) left = 0;
    if (left + dimRect.width > window.innerWidth) {
      left = window.innerWidth - dimRect.width;
    }

    overlay.dimension.style.left = left + 'px';
    overlay.dimension.style.top = top + 'px';
  }

  /* ========================================================================
     Padding Overlay (green)
     ======================================================================== */
  function showPaddingOverlay(el) {
    const padAttr = el.getAttribute('data-padding');
    if (!padAttr) {
      overlay.padding.style.display = 'none';
      return;
    }

    const r = el.getBoundingClientRect();
    const pad = parseSides(padAttr);

    overlay.padding.style.display = 'block';
    overlay.padding.style.left = r.left + 'px';
    overlay.padding.style.top = r.top + 'px';
    overlay.padding.style.width = r.width + 'px';
    overlay.padding.style.height = r.height + 'px';
    overlay.padding.style.borderWidth =
      pad.top + 'px ' + pad.right + 'px ' + pad.bottom + 'px ' + pad.left + 'px';
    overlay.padding.style.borderStyle = 'dashed';
    overlay.padding.style.background = 'none';
    overlay.padding.style.boxSizing = 'border-box';

    // Fill the padding region with green using a clip-path-free approach:
    // We set border-color to the green tint
    overlay.padding.style.borderColor = 'rgba(76, 175, 80, 0.25)';
  }

  /* ========================================================================
     Margin Overlay (orange)
     ======================================================================== */
  function showMarginOverlay(el) {
    const marginAttr = el.getAttribute('data-margin');
    if (!marginAttr) {
      overlay.margin.style.display = 'none';
      return;
    }

    const r = el.getBoundingClientRect();
    const m = parseSides(marginAttr);

    positionBox(overlay.margin, {
      left: r.left - m.left,
      top: r.top - m.top,
      width: r.width + m.left + m.right,
      height: r.height + m.top + m.bottom,
    });
    overlay.margin.style.display = 'block';
  }

  /* ========================================================================
     Property Panel
     ======================================================================== */
  function showPanel(el) {
    const name = el.getAttribute('data-inspect') || 'Unknown';
    const r = el.getBoundingClientRect();

    let html = '<h3>' + escapeHtml(name) + '</h3>';

    // Layout section
    html += sectionStart('Layout');
    const w = el.getAttribute('data-w') || Math.round(r.width) + 'px';
    const h = el.getAttribute('data-h') || Math.round(r.height) + 'px';
    html += row('Width', w);
    html += row('Height', h);

    const padding = el.getAttribute('data-padding');
    if (padding) html += row('Padding', formatSides(padding));

    const margin = el.getAttribute('data-margin');
    if (margin) html += row('Margin', formatSides(margin));

    const gap = el.getAttribute('data-gap');
    if (gap) html += row('Gap', gap);

    html += sectionEnd();

    // Style section
    const corner = el.getAttribute('data-corner');
    const color = el.getAttribute('data-color');
    const font = el.getAttribute('data-font');

    if (corner || color || font) {
      html += sectionStart('Style');
      if (corner) html += row('Corner', corner);
      if (color) html += colorRow('Color', color);
      if (font) html += row('Font', font);
      html += sectionEnd();
    }

    // Additional data attributes
    const extras = collectExtraData(el);
    if (extras.length > 0) {
      html += sectionStart('Properties');
      extras.forEach(function (pair) {
        html += row(pair.key, pair.value);
      });
      html += sectionEnd();
    }

    // Position section
    html += sectionStart('Position');
    html += row('X', Math.round(r.left) + 'px');
    html += row('Y', Math.round(r.top) + 'px');
    html += sectionEnd();

    overlay.panel.innerHTML = html;
    overlay.panel.style.display = 'block';

    // Animate in
    requestAnimationFrame(function () {
      overlay.panel.classList.add('visible');
    });

    updatePanelPosition();
  }

  function updatePanelPosition() {
    // Panel stays fixed at right:16px, top:80px via CSS
    // but ensure it doesn't overlap the element if there's room on the left
    // For simplicity, keep it at the fixed CSS position.
  }

  function hidePanel() {
    overlay.panel.classList.remove('visible');
    overlay.panel.style.display = 'none';
    overlay.panel.innerHTML = '';
  }

  /* ========================================================================
     Spacing Rulers — between selected and hovered (Alt mode)
     ======================================================================== */
  function showSpacingBetween(elA, elB) {
    clearSpacing();

    const a = elA.getBoundingClientRect();
    const b = elB.getBoundingClientRect();

    // Horizontal distance
    const hDist = horizontalDistance(a, b);
    if (hDist !== null && hDist.value > 0) {
      createHorizontalRuler(hDist.x, hDist.y, hDist.length, hDist.value);
    }

    // Vertical distance
    const vDist = verticalDistance(a, b);
    if (vDist !== null && vDist.value > 0) {
      createVerticalRuler(vDist.x, vDist.y, vDist.length, vDist.value);
    }
  }

  /** Show spacing to immediate siblings when an element is selected. */
  function showSiblingSpacing(el) {
    clearSpacing();

    const r = el.getBoundingClientRect();
    const parent = el.parentElement;
    if (!parent) return;

    const siblings = Array.from(parent.children).filter(function (child) {
      return child !== el && child.getAttribute('data-inspect') !== null;
    });

    siblings.forEach(function (sib) {
      const sr = sib.getBoundingClientRect();

      // Only show spacing if they are adjacent (no overlap)
      const hDist = horizontalDistance(r, sr);
      if (hDist !== null && hDist.value > 0 && hDist.value < 200) {
        createHorizontalRuler(hDist.x, hDist.y, hDist.length, hDist.value);
      }

      const vDist = verticalDistance(r, sr);
      if (vDist !== null && vDist.value > 0 && vDist.value < 200) {
        createVerticalRuler(vDist.x, vDist.y, vDist.length, vDist.value);
      }
    });
  }

  function horizontalDistance(a, b) {
    // Check if they overlap vertically (needed for horizontal ruler to make sense)
    const overlapTop = Math.max(a.top, b.top);
    const overlapBottom = Math.min(a.bottom, b.bottom);
    if (overlapTop >= overlapBottom) return null;

    const midY = (overlapTop + overlapBottom) / 2;
    let x1, x2;

    if (a.right <= b.left) {
      // a is to the left of b
      x1 = a.right;
      x2 = b.left;
    } else if (b.right <= a.left) {
      // b is to the left of a
      x1 = b.right;
      x2 = a.left;
    } else {
      return null; // overlapping horizontally
    }

    return { x: x1, y: midY, length: x2 - x1, value: Math.round(x2 - x1) };
  }

  function verticalDistance(a, b) {
    // Check if they overlap horizontally
    const overlapLeft = Math.max(a.left, b.left);
    const overlapRight = Math.min(a.right, b.right);
    if (overlapLeft >= overlapRight) return null;

    const midX = (overlapLeft + overlapRight) / 2;
    let y1, y2;

    if (a.bottom <= b.top) {
      y1 = a.bottom;
      y2 = b.top;
    } else if (b.bottom <= a.top) {
      y1 = b.bottom;
      y2 = a.top;
    } else {
      return null; // overlapping vertically
    }

    return { x: midX, y: y1, length: y2 - y1, value: Math.round(y2 - y1) };
  }

  function createHorizontalRuler(x, y, length, value) {
    const ruler = document.createElement('div');
    ruler.className = 'inspector-spacing';
    ruler.style.left = x + 'px';
    ruler.style.top = y + 'px';
    ruler.style.width = length + 'px';
    ruler.style.height = '1px';

    // Main line
    const line = document.createElement('div');
    line.className = 'inspector-spacing-line inspector-spacing-line-h';
    line.style.position = 'absolute';
    line.style.left = '0';
    line.style.top = '0';
    line.style.width = '100%';
    line.style.height = '1px';
    ruler.appendChild(line);

    // Left cap
    const capL = document.createElement('div');
    capL.className = 'inspector-spacing-cap inspector-spacing-cap-h';
    capL.style.left = '0';
    capL.style.top = '-3px';
    ruler.appendChild(capL);

    // Right cap
    const capR = document.createElement('div');
    capR.className = 'inspector-spacing-cap inspector-spacing-cap-h';
    capR.style.right = '0';
    capR.style.top = '-3px';
    ruler.appendChild(capR);

    // Label
    const label = document.createElement('div');
    label.className = 'inspector-spacing-label';
    label.textContent = value + 'px';
    label.style.left = '50%';
    label.style.top = '-18px';
    label.style.transform = 'translateX(-50%)';
    ruler.appendChild(label);

    overlay.spacingContainer.appendChild(ruler);
  }

  function createVerticalRuler(x, y, length, value) {
    const ruler = document.createElement('div');
    ruler.className = 'inspector-spacing';
    ruler.style.left = x + 'px';
    ruler.style.top = y + 'px';
    ruler.style.width = '1px';
    ruler.style.height = length + 'px';

    // Main line
    const line = document.createElement('div');
    line.className = 'inspector-spacing-line inspector-spacing-line-v';
    line.style.position = 'absolute';
    line.style.left = '0';
    line.style.top = '0';
    line.style.width = '1px';
    line.style.height = '100%';
    ruler.appendChild(line);

    // Top cap
    const capT = document.createElement('div');
    capT.className = 'inspector-spacing-cap inspector-spacing-cap-v';
    capT.style.left = '-3px';
    capT.style.top = '0';
    ruler.appendChild(capT);

    // Bottom cap
    const capB = document.createElement('div');
    capB.className = 'inspector-spacing-cap inspector-spacing-cap-v';
    capB.style.left = '-3px';
    capB.style.bottom = '0';
    ruler.appendChild(capB);

    // Label
    const label = document.createElement('div');
    label.className = 'inspector-spacing-label';
    label.textContent = value + 'px';
    label.style.left = '8px';
    label.style.top = '50%';
    label.style.transform = 'translateY(-50%)';
    ruler.appendChild(label);

    overlay.spacingContainer.appendChild(ruler);
  }

  function clearSpacing() {
    overlay.spacingContainer.innerHTML = '';
  }

  /* ========================================================================
     Hide Helpers
     ======================================================================== */
  function hideHoverOverlays() {
    overlay.highlight.style.display = 'none';
    overlay.dimension.style.display = 'none';
    overlay.padding.style.display = 'none';
    overlay.margin.style.display = 'none';
  }

  function hideAll() {
    hideHoverOverlays();
    hidePanel();
    clearSpacing();
  }

  /* ========================================================================
     DOM Helpers
     ======================================================================== */

  /** Find the closest ancestor (or self) with [data-inspect]. Picks the most
   *  specific (deepest) inspectable element. */
  function findInspectable(el) {
    if (!el || el === document.body || el === document.documentElement) return null;
    // Walk up until we find [data-inspect], but prefer the deepest match.
    // Since the event target is already the deepest element, the first
    // match going upward is the most specific one.
    let cur = el;
    while (cur && cur !== document.body) {
      if (cur.hasAttribute && cur.hasAttribute('data-inspect')) return cur;
      cur = cur.parentElement;
    }
    return null;
  }

  /** Check if an element is part of the inspector UI itself. */
  function isInsideOverlay(el) {
    if (!el) return false;
    let cur = el;
    while (cur) {
      if (cur === overlay.panel || cur === overlay.highlight ||
          cur === overlay.dimension || cur === overlay.padding ||
          cur === overlay.margin || cur === overlay.spacingContainer ||
          cur === toggleBtn) {
        return true;
      }
      cur = cur.parentElement;
    }
    return false;
  }

  /** Create a div with a class name. */
  function createDiv(className) {
    const d = document.createElement('div');
    d.className = className;
    d.style.display = 'none';
    return d;
  }

  /** Position a fixed-position box to match a DOMRect. */
  function positionBox(el, r) {
    el.style.left = r.left + 'px';
    el.style.top = r.top + 'px';
    el.style.width = (r.width || 0) + 'px';
    el.style.height = (r.height || 0) + 'px';
  }

  /* ========================================================================
     Data Attribute Helpers
     ======================================================================== */

  /** Parse a sides value like "16" or "16,8,16,8" into {top,right,bottom,left}. */
  function parseSides(val) {
    const parts = val.split(',').map(function (s) { return parseInt(s.trim(), 10) || 0; });
    if (parts.length === 1) {
      return { top: parts[0], right: parts[0], bottom: parts[0], left: parts[0] };
    }
    if (parts.length === 2) {
      return { top: parts[0], right: parts[1], bottom: parts[0], left: parts[1] };
    }
    if (parts.length === 4) {
      return { top: parts[0], right: parts[1], bottom: parts[2], left: parts[3] };
    }
    return { top: parts[0] || 0, right: parts[1] || 0, bottom: parts[2] || 0, left: parts[3] || 0 };
  }

  /** Format sides for display in the panel. */
  function formatSides(val) {
    const s = parseSides(val);
    if (s.top === s.right && s.right === s.bottom && s.bottom === s.left) {
      return s.top + 'px';
    }
    return s.top + ' ' + s.right + ' ' + s.bottom + ' ' + s.left;
  }

  /** Collect data-* attributes that are not the standard inspector ones. */
  function collectExtraData(el) {
    const skip = new Set([
      'inspect', 'w', 'h', 'padding', 'margin', 'corner', 'color', 'font', 'gap',
    ]);
    var extras = [];
    var attrs = el.attributes;
    for (var i = 0; i < attrs.length; i++) {
      var attr = attrs[i];
      if (attr.name.indexOf('data-') === 0) {
        var key = attr.name.substring(5); // strip "data-"
        if (!skip.has(key)) {
          extras.push({ key: key, value: attr.value });
        }
      }
    }
    return extras;
  }

  /* ========================================================================
     Panel HTML Helpers
     ======================================================================== */

  function sectionStart(title) {
    return '<div class="inspector-panel-section"><div class="inspector-panel-section-title">' +
      escapeHtml(title) + '</div>';
  }

  function sectionEnd() {
    return '</div>';
  }

  function row(key, value) {
    return '<div class="inspector-panel-row">' +
      '<span class="inspector-panel-key">' + escapeHtml(key) + '</span>' +
      '<span class="inspector-panel-value">' + escapeHtml(value) + '</span>' +
      '</div>';
  }

  function colorRow(key, tokenName) {
    // Attempt to resolve the CSS variable
    var varName = '--md-' + tokenName;
    var resolved = getComputedStyle(document.documentElement).getPropertyValue(varName).trim();
    var swatchColor = resolved || tokenName;

    return '<div class="inspector-panel-row">' +
      '<span class="inspector-panel-key">' + escapeHtml(key) + '</span>' +
      '<span class="inspector-panel-value">' +
        '<span class="color-swatch" style="background:' + escapeHtml(swatchColor) + '"></span>' +
        escapeHtml(tokenName) +
      '</span>' +
      '</div>';
  }

  function escapeHtml(str) {
    if (!str) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  /* ========================================================================
     Public API
     ======================================================================== */
  window.Inspector = {
    toggle: toggleInspector,
    refresh: function () {
      // Called after SPA page loads to reset state for new content
      deselect();
      hideAll();
    },
  };

})();
