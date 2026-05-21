(() => {
  const escapeHtml = (value) =>
    value
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;");

  const restore = (value, tokens) =>
    tokens.reduce((html, token, index) => html.replaceAll(`__HL_${index}__`, token), value);

  const protect = (html, tokens, pattern, className) =>
    html.replace(pattern, (match) => {
      const index = tokens.length;
      tokens.push(`<span class="${className}">${match}</span>`);
      return `__HL_${index}__`;
    });

  const highlightJava = (raw) => {
    const tokens = [];
    let html = escapeHtml(raw);
    html = protect(html, tokens, /&quot;(?:\\.|[^&])*?&quot;/g, "tok-string");
    html = protect(html, tokens, /\/\/.*$/gm, "tok-comment");
    html = protect(html, tokens, /@[A-Za-z_][\w.]*/g, "tok-annotation");
    html = html.replace(
      /\b(public|private|protected|class|static|final|return|new|if|else|for|while|try|catch|throws|throw|var|void|boolean|true|false|null|instanceof)\b/g,
      '<span class="tok-keyword">$1</span>'
    );
    html = html.replace(/\b(\d+)\b/g, '<span class="tok-number">$1</span>');
    html = html.replace(/\b([a-z][A-Za-z0-9_]*)\s*(?=\()/g, '<span class="tok-method">$1</span>');
    return restore(html, tokens);
  };

  const highlightXml = (raw) => {
    let html = escapeHtml(raw);
    html = html.replace(/(&lt;\/?)([A-Za-z_][\w:.-]*)/g, '$1<span class="tok-tag">$2</span>');
    html = html.replace(/([A-Za-z_][\w:.-]*)(=)(&quot;[^&]*?&quot;)/g, '<span class="tok-attr">$1</span>$2<span class="tok-attr-value">$3</span>');
    html = html.replace(/(&lt;!--[\s\S]*?--&gt;)/g, '<span class="tok-comment">$1</span>');
    return html;
  };

  const highlightBash = (raw) => {
    const tokens = [];
    let html = escapeHtml(raw);
    html = protect(html, tokens, /#.*$/gm, "tok-comment");
    html = html.replace(/^(\s*)(\.\/[\w./-]+|git|cd|curl|keytool)(\b)/gm, '$1<span class="tok-method">$2</span>$3');
    html = html.replace(/(&quot;[^&]*?&quot;)/g, '<span class="tok-string">$1</span>');
    return restore(html, tokens);
  };

  const highlightProperties = (raw) =>
    escapeHtml(raw)
      .split("\n")
      .map((line) =>
        line.replace(/^([^=\s]+)(=)(.*)$/g, '<span class="tok-property">$1</span>$2<span class="tok-string">$3</span>')
      )
      .join("\n");

  const highlightLog = (raw) =>
    escapeHtml(raw)
      .split("\n")
      .map((line) => {
        let html = line;
        html = html.replace(/^(\d{4}-\d{2}-\d{2}T[^\s]+)/, '<span class="tok-time">$1</span>');
        html = html.replace(/\b(INFO|TRACE|WARN|ERROR|DEBUG)\b/g, '<span class="tok-level">$1</span>');
        html = html.replace(/\b(Incoming Request|Outgoing Request|Incoming Response|Outgoing Response)\b/g, '<span class="tok-http">$1</span>');
        html = html.replace(/\b(GET|POST|PUT|DELETE|HTTP\/1\.1)\b/g, '<span class="tok-method">$1</span>');
        html = html.replace(/\b(200 OK|400 Bad Request|401 Unauthorized|403 Forbidden|500 Internal Server Error)\b/g, '<span class="tok-status">$1</span>');
        html = html.replace(/(https?:\/\/[^\s]+)/g, '<span class="tok-url">$1</span>');
        html = html.replace(/(&lt;\/?)([A-Za-z_][\w:.-]*)/g, '$1<span class="tok-tag">$2</span>');
        return html;
      })
      .join("\n");

  const highlighters = {
    "language-java": highlightJava,
    "language-xml": highlightXml,
    "language-bash": highlightBash,
    "language-properties": highlightProperties,
    "language-log": highlightLog
  };

  const highlightCodeBlock = (block) => {
    const raw = block.dataset.rawCode ?? block.textContent;
    block.dataset.rawCode = raw;
    const highlighter = [...block.classList].map((name) => highlighters[name]).find(Boolean);
    block.innerHTML = highlighter ? highlighter(raw) : escapeHtml(raw);
  };

  const loadLogs = async () => {
    const logBlocks = document.querySelectorAll("[data-log-src]");
    await Promise.all(
      Array.from(logBlocks).map(async (block) => {
        const source = block.getAttribute("data-log-src");
        try {
          const response = await fetch(source);
          if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
          }
          block.dataset.rawCode = await response.text();
        } catch (error) {
          block.dataset.rawCode = `Could not load ${source}. Run a local HTTP server or deploy this directory to Vercel.`;
        }
        highlightCodeBlock(block);
      })
    );
  };

  document.querySelectorAll("pre code:not([data-log-src])").forEach(highlightCodeBlock);
  loadLogs();

  const copyButtons = document.querySelectorAll("[data-copy-code]");
  copyButtons.forEach((button) => {
    button.addEventListener("click", async () => {
      const card = button.closest(".code-card");
      const codeBlock = card?.querySelector("pre code");
      const code = codeBlock?.dataset.rawCode ?? codeBlock?.innerText ?? "";
      if (!code) {
        return;
      }

      const originalText = button.textContent;
      try {
        await navigator.clipboard.writeText(code);
        button.textContent = "Copied";
      } catch {
        button.textContent = "Select text";
      }

      window.setTimeout(() => {
        button.textContent = originalText;
      }, 1600);
    });
  });

  const tocLinks = Array.from(document.querySelectorAll(".toc a"));
  const sections = tocLinks
    .map((link) => document.querySelector(link.getAttribute("href")))
    .filter(Boolean);

  const activeObserver = new IntersectionObserver(
    (entries) => {
      const visible = entries
        .filter((entry) => entry.isIntersecting)
        .sort((a, b) => b.intersectionRatio - a.intersectionRatio)[0];

      if (!visible) {
        return;
      }

      tocLinks.forEach((link) => {
        link.classList.toggle("active", link.getAttribute("href") === `#${visible.target.id}`);
      });
    },
    {
      rootMargin: "-20% 0px -65% 0px",
      threshold: [0.1, 0.25, 0.5]
    }
  );

  sections.forEach((section) => activeObserver.observe(section));

  const revealObserver = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add("visible");
          revealObserver.unobserve(entry.target);
        }
      });
    },
    { threshold: 0.12 }
  );

  document.querySelectorAll(".reveal").forEach((element) => {
    revealObserver.observe(element);
  });
})();
