// SPA Navigation for Design System docs
// Supports both HTTP server and file:// protocol
(function() {
  const contentEl = document.getElementById('page-content');
  const links = document.querySelectorAll('.sidebar-link[data-page]');
  const isFileProtocol = window.location.protocol === 'file:';

  // Load a page into the content area
  async function loadPage(pagePath) {
    try {
      // For file:// protocol, use iframe technique as fallback
      if (isFileProtocol) {
        loadPageViaIframe(pagePath);
        return;
      }

      const response = await fetch(pagePath);
      if (!response.ok) throw new Error('Page not found');
      const html = await response.text();
      injectContent(html, pagePath);

    } catch (error) {
      // Fallback to iframe method
      loadPageViaIframe(pagePath);
    }
  }

  // Load page using a hidden iframe (works with file:// protocol)
  function loadPageViaIframe(pagePath) {
    const iframe = document.createElement('iframe');
    iframe.style.display = 'none';
    iframe.onload = function() {
      try {
        const doc = iframe.contentDocument || iframe.contentWindow.document;
        const main = doc.querySelector('main') || doc.querySelector('body');
        if (main) {
          injectContent(main.innerHTML, pagePath, true);
        }
      } catch (e) {
        // If even iframe fails (some browsers block file:// iframe access),
        // navigate directly
        contentEl.innerHTML = `
          <div class="content-header">
            <div>
              <h1>Direct Navigation</h1>
              <p class="page-subtitle">SPA 모드가 지원되지 않습니다. 아래 링크를 클릭하세요.</p>
            </div>
          </div>
          <p><a href="${pagePath}" style="color: var(--md-primary); font-size: 16px;">📄 ${pagePath} 직접 열기</a></p>
          <p style="margin-top: 16px; font-size: 13px; color: var(--md-on-surface-variant);">
            Tip: 로컬 서버를 사용하면 SPA 모드로 더 나은 경험을 할 수 있습니다.<br>
            <code style="background: var(--md-surface-container-high); padding: 4px 8px; border-radius: 4px;">python -m http.server 8000</code> 또는
            <code style="background: var(--md-surface-container-high); padding: 4px 8px; border-radius: 4px;">npx serve .</code>
          </p>
        `;
      }
      document.body.removeChild(iframe);
    };
    iframe.onerror = function() {
      document.body.removeChild(iframe);
    };
    iframe.src = pagePath;
    document.body.appendChild(iframe);
  }

  // Inject HTML content into the content area
  function injectContent(html, pagePath, isRawHtml) {
    if (!isRawHtml) {
      const parser = new DOMParser();
      const doc = parser.parseFromString(html, 'text/html');
      const main = doc.querySelector('main') || doc.querySelector('body');
      contentEl.innerHTML = main.innerHTML;
    } else {
      contentEl.innerHTML = html;
    }

    // Execute inline scripts in the loaded content
    const scripts = contentEl.querySelectorAll('script');
    scripts.forEach(function(oldScript) {
      const newScript = document.createElement('script');
      if (oldScript.src) {
        newScript.src = oldScript.src;
      } else {
        newScript.textContent = oldScript.textContent;
      }
      oldScript.parentNode.replaceChild(newScript, oldScript);
    });

    // Re-initialize inspector for new content
    if (window.Inspector && window.Inspector.refresh) {
      window.Inspector.refresh();
    }

    // Update active link
    links.forEach(function(link) {
      link.classList.toggle('active', link.dataset.page === pagePath);
    });

    // Scroll to top
    contentEl.scrollTo(0, 0);

    // Store in URL hash
    window.location.hash = pagePath;
  }

  // Attach click handlers
  links.forEach(function(link) {
    link.addEventListener('click', function(e) {
      e.preventDefault();
      loadPage(link.dataset.page);
    });
  });

  // Load page from hash on init
  var initialPage = window.location.hash.slice(1) || 'pages/tokens/colors.html';
  loadPage(initialPage);

  // Handle back/forward
  window.addEventListener('hashchange', function() {
    var page = window.location.hash.slice(1);
    if (page) loadPage(page);
  });

  // Expose for external use
  window.Nav = { loadPage: loadPage };
})();
