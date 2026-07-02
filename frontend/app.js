const DEFAULT_API_BASE = "http://localhost:8080/api";

const state = {
  templates: [],
  selectedTemplate: null,
  activeView: "market",
  marketQuery: new URLSearchParams(),
};

const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => Array.from(document.querySelectorAll(selector));

function apiBase() {
  const queryBase = new URLSearchParams(window.location.search).get("apiBase");
  const storedBase = window.localStorage.getItem("apiBase");
  return (queryBase || storedBase || DEFAULT_API_BASE).replace(/\/$/, "");
}

async function api(path, options = {}) {
  const response = await fetch(`${apiBase()}${path}`, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });

  if (!response.ok) {
    let message = `HTTP ${response.status}`;
    try {
      const data = await response.json();
      message = data.message || message;
    } catch (error) {
      const text = await response.text();
      if (text) message = text;
    }
    throw new Error(message);
  }

  return response.json();
}

function showMessage(text) {
  const message = $("#message");
  message.textContent = text;
  message.hidden = false;
  clearTimeout(showMessage.timer);
  showMessage.timer = window.setTimeout(() => {
    message.hidden = true;
  }, 3200);
}

function money(value) {
  const number = Number(value || 0);
  return number === 0 ? "免费" : `¥${number.toFixed(2)}`;
}

function shortText(text, size = 90) {
  if (!text) return "暂无内容";
  return text.length > size ? `${text.slice(0, size)}...` : text;
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function renderTagList(tags = []) {
  if (!tags.length) return `<span class="pill">未分类</span>`;
  return tags.map((tag) => `<span class="pill">${escapeHtml(tag)}</span>`).join("");
}

function openModal() {
  $("#templateModal").hidden = false;
  document.body.style.overflow = "hidden";
}

function closeModal() {
  $("#templateModal").hidden = true;
  document.body.style.overflow = "";
}

async function runSafely(task) {
  try {
    await task();
  } catch (error) {
    if (error.message.includes("Failed to fetch")) {
      showMessage(`无法连接后端：${apiBase()}`);
      return;
    }
    showMessage(`操作失败：${error.message}`);
  }
}

async function loadTags() {
  const tags = await api("/tags");
  $("#tagSelect").innerHTML = `<option value="">全部标签</option>${tags
    .map((tag) => `<option value="${escapeHtml(tag)}">${escapeHtml(tag)}</option>`)
    .join("")}`;
}

async function loadTemplates(params = state.marketQuery) {
  state.marketQuery = new URLSearchParams(params.toString());
  const query = state.marketQuery.toString();
  const data = await api(`/templates/search${query ? `?${query}` : ""}`);
  state.templates = data.records || data || [];
  renderTemplates();
}

function renderTemplates() {
  const container = $("#templateList");
  if (!state.templates.length) {
    container.innerHTML = `<div class="empty-state">暂无符合条件的模板</div>`;
    return;
  }

  container.innerHTML = state.templates
    .map((item) => `
      <article class="template-row" data-row-detail="${item.id}">
        <div class="row-title">
          <strong>${escapeHtml(item.title)}</strong>
          <p>${escapeHtml(shortText(item.scene, 54))}</p>
        </div>
        <div class="tag-list">${renderTagList(item.tags || [])}</div>
        <div class="row-stats">
          <span>评分：${item.avgScore ?? 0}</span>
          <span>使用：${item.useCount ?? 0}</span>
          <span>收藏：${item.favoriteCount ?? 0}</span>
          <span class="price">${money(item.price)}</span>
        </div>
        <div class="row-actions">
          <button class="ghost-outline" data-detail="${item.id}" type="button">查看详情</button>
          <button class="ghost-outline" data-favorite="${item.id}" type="button">收藏</button>
          <button class="action-btn" data-purchase="${item.id}" type="button">购买/领取</button>
        </div>
      </article>
    `)
    .join("");
}

async function selectTemplate(id, { open = true } = {}) {
  const detail = await api(`/templates/${id}`);
  state.selectedTemplate = detail;
  renderDetail(detail);
  if (open) openModal();
}

function renderDetail(item) {
  $("#modalTitle").textContent = item.title || "模板详情";
  $("#modalBody").innerHTML = `
    <section class="panel">
      <div class="section-head">
        <h3>${escapeHtml(item.title)}</h3>
        <span class="price">${money(item.price)}</span>
      </div>
      <p>${escapeHtml(item.scene || "暂无场景说明")}</p>
      <div class="meta-line">
        <span>作者：${escapeHtml(item.creatorName || "-")}</span>
        <span>评分：${item.avgScore ?? 0}</span>
        <span>收藏：${item.favoriteCount ?? 0}</span>
        <span>使用：${item.useCount ?? 0}</span>
      </div>
      <div class="tag-list">${renderTagList(item.tags || [])}</div>
    </section>

    <section class="panel">
      <h3>模板内容</h3>
      <pre class="prompt-preview">${escapeHtml(item.promptContent || "")}</pre>
    </section>

    <section class="panel">
      <h3>操作</h3>
      <div class="card-actions">
        <button class="action-btn" data-purchase="${item.id}" type="button">购买/领取</button>
        <button class="ghost-outline" data-favorite="${item.id}" type="button">收藏</button>
      </div>
      <p class="muted">使用输入摘要</p>
      <textarea id="useSummary">课程设计数据库模块优化</textarea>
      <div class="card-actions">
        <button class="action-btn" data-use="${item.id}" type="button">使用模板</button>
      </div>
      <pre id="useResult" class="prompt-preview" hidden></pre>
    </section>

    <section class="panel">
      <h3>提交评价</h3>
      <p class="muted">评价内容</p>
      <textarea id="reviewContent">结构清晰，适合课程设计演示。</textarea>
      <div class="card-actions">
        <select id="reviewScore">
          <option value="5">5 分</option>
          <option value="4">4 分</option>
          <option value="3">3 分</option>
        </select>
        <button class="action-btn" data-review="${item.id}" type="button">提交评价</button>
      </div>
    </section>

    <section class="panel">
      <h3>最近评价</h3>
      <div class="list">
        ${renderReviewRows(item.reviews || [])}
      </div>
    </section>
  `;
}

function renderReviewRows(reviews) {
  if (!reviews.length) {
    return `<div class="list-row muted">暂无评价</div>`;
  }

  return reviews.slice(0, 5).map((review) => `
    <div class="list-row">
      <strong>${escapeHtml(review.username || "匿名用户")} · ${review.score ?? 0} 分</strong>
      <p>${escapeHtml(review.content || "")}</p>
    </div>
  `).join("");
}

function renderList(title, rows = [], formatter) {
  return `
    <section class="panel">
      <h3>${title}</h3>
      <div class="list">
        ${(rows || []).length
          ? rows.map((row) => `<div class="list-row">${escapeHtml(formatter(row))}</div>`).join("")
          : `<div class="list-row muted">暂无数据</div>`}
      </div>
    </section>
  `;
}

async function loadProfile() {
  const profile = await api("/profile");
  $("#profilePanel").innerHTML = `
    <section class="panel">
      <div class="split">
        <div>
          <h2>${escapeHtml(profile.username || "-")}</h2>
          <p class="muted">${escapeHtml(profile.email || "")} ${escapeHtml(profile.phone || "")}</p>
        </div>
        <div>
          <h3>当前余额</h3>
          <p class="price">${money(profile.balance)}</p>
        </div>
        <div>
          <h3>创作者等级</h3>
          <p>${escapeHtml(profile.creatorLevel || "-")}</p>
        </div>
      </div>
    </section>
    <div class="split">
      ${renderList("我的订单", profile.orders, (item) => `${item.orderNo} · ${item.templateTitle} · ${money(item.payAmount)}`)}
      ${renderList("使用记录", profile.usageLogs, (item) => `${item.templateTitle} · ${item.inputSummary}`)}
      ${renderList("余额流水", profile.balanceLogs, (item) => `${item.changeType} · ${money(item.changeAmount)} · 余额 ${money(item.balanceAfter)}`)}
    </div>
  `;
}

async function loadDashboard(path, target) {
  const data = await api(path);
  target.innerHTML = `
    <div class="dashboard-grid">
      ${(data.statCards || []).map((card) => `
        <section class="stat-card">
          <span class="muted">${escapeHtml(card.label)}</span>
          <strong>${escapeHtml(card.value)}</strong>
          <span>${escapeHtml(card.trend)}</span>
        </section>
      `).join("")}
    </div>
    <div class="split">
      ${renderList("热门模板", data.templates || [], (item) => `${item.title} · ${money(item.price)} · 使用 ${item.useCount || 0}`)}
      ${renderList("活动信息", data.campaigns || [], (item) => `${item.name} · ${item.templateTitle} · 剩余 ${item.remainingQuota}`)}
    </div>
  `;
}

function updateViewMeta(view) {
  const metas = {
    market: {
      title: "模板市场",
      subtitle: "搜索、筛选、购买并使用 AI 提示词模板。",
    },
    profile: {
      title: "个人中心",
      subtitle: "查看订单、使用记录和余额变化。",
    },
    creator: {
      title: "创作者看板",
      subtitle: "查看模板运营数据和活动效果。",
    },
    platform: {
      title: "平台总览",
      subtitle: "查看平台统计与整体业务概况。",
    },
  };

  $("#viewTitle").textContent = metas[view].title;
  $("#viewSubtitle").textContent = metas[view].subtitle;
}

function setActiveView(view) {
  state.activeView = view;
  $$(".nav-item").forEach((item) => item.classList.toggle("active", item.dataset.view === view));
  $$(".view").forEach((panel) => panel.classList.toggle("active", panel.id === `${view}View`));
  updateViewMeta(view);
}

async function refreshCurrentView() {
  if (state.activeView === "market") {
    await loadTags();
    await loadTemplates(state.marketQuery);
  }
  if (state.activeView === "profile") {
    await loadProfile();
  }
  if (state.activeView === "creator") {
    await loadDashboard("/stats/creator", $("#creatorPanel"));
  }
  if (state.activeView === "platform") {
    await loadDashboard("/stats/platform", $("#platformPanel"));
  }
}

document.addEventListener("click", (event) => {
  runSafely(async () => {
    const navButton = event.target.closest(".nav-item");
    if (navButton) {
      setActiveView(navButton.dataset.view);
      await refreshCurrentView();
      return;
    }

    if (event.target.closest("[data-close-modal]") || event.target.closest("#closeModalBtn")) {
      closeModal();
      return;
    }

    const favoriteButton = event.target.closest("[data-favorite]");
    if (favoriteButton) {
      const id = favoriteButton.dataset.favorite;
      const result = await api(`/templates/${id}/favorite`, { method: "POST" });
      showMessage(result.message || "操作成功");
      await loadTemplates(state.marketQuery);
      if (state.selectedTemplate?.id === Number(id)) {
        await selectTemplate(id, { open: false });
      }
      return;
    }

    const purchaseButton = event.target.closest("[data-purchase]");
    if (purchaseButton) {
      const id = purchaseButton.dataset.purchase;
      const result = await api("/orders", {
        method: "POST",
        body: JSON.stringify({ templateId: Number(id) }),
      });
      showMessage(result.message || "操作成功");
      await loadTemplates(state.marketQuery);
      if (state.selectedTemplate?.id === Number(id)) {
        await selectTemplate(id, { open: false });
      }
      return;
    }

    const useButton = event.target.closest("[data-use]");
    if (useButton) {
      const id = useButton.dataset.use;
      const result = await api(`/templates/${id}/use`, {
        method: "POST",
        body: JSON.stringify({ inputSummary: $("#useSummary").value }),
      });
      showMessage(result.message || (result.success ? "模板使用成功" : "操作成功"));
      if (result.promptContent) {
        const output = $("#useResult");
        output.textContent = result.promptContent;
        output.hidden = false;
      }
      await loadTemplates(state.marketQuery);
      await selectTemplate(id, { open: false });
      return;
    }

    const reviewButton = event.target.closest("[data-review]");
    if (reviewButton) {
      const id = reviewButton.dataset.review;
      const result = await api("/reviews", {
        method: "POST",
        body: JSON.stringify({
          templateId: Number(id),
          score: Number($("#reviewScore").value),
          content: $("#reviewContent").value,
        }),
      });
      showMessage(result.message || "评价已提交");
      await loadTemplates(state.marketQuery);
      await selectTemplate(id, { open: false });
      return;
    }

    const detailButton = event.target.closest("[data-detail]");
    if (detailButton) {
      await selectTemplate(detailButton.dataset.detail);
      return;
    }

    const row = event.target.closest("[data-row-detail]");
    if (row && !event.target.closest(".row-actions")) {
      await selectTemplate(row.dataset.rowDetail);
    }
  });
});

document.addEventListener("keydown", (event) => {
  if (event.key === "Escape" && !$("#templateModal").hidden) {
    closeModal();
  }
});

$("#searchForm").addEventListener("submit", (event) => {
  runSafely(async () => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const params = new URLSearchParams();
    for (const [key, value] of form.entries()) {
      if (value) params.set(key, value);
    }
    await loadTemplates(params);
  });
});

$("#loginForm").addEventListener("submit", (event) => {
  runSafely(async () => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const result = await api("/auth/login", {
      method: "POST",
      body: JSON.stringify(Object.fromEntries(form.entries())),
    });
    showMessage(result.message || "登录完成");
  });
});

$("#reloadBtn").addEventListener("click", () => {
  runSafely(refreshCurrentView);
});

runSafely(refreshCurrentView);
