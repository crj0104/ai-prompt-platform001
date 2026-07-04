const DEFAULT_API_BASE = "http://localhost:8080/api";

const state = {
  session: JSON.parse(window.localStorage.getItem("session") || "null"),
  templates: [],
  selectedTemplate: null,
  activeView: "auth",
  marketQuery: new URLSearchParams(),
  page: { current: 1, size: 4, total: 0, pages: 0 },
};

const viewMeta = {
  auth: ["登录 / 注册", "请先登录或注册，再进入系统。"],
  market: ["模板市场", "搜索、筛选、购买并使用 AI 提示词模板。"],
  use: ["模板使用", "使用已购买或已领取的提示词模板。"],
  profile: ["个人中心", "查看订单、使用记录和余额变化。"],
  creator: ["创作者看板", "查看发布模板的运营数据。"],
  platform: ["平台总览", "查看平台统计与整体业务概况。"],
};

const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => Array.from(document.querySelectorAll(selector));

function apiBase() {
  const queryBase = new URLSearchParams(window.location.search).get("apiBase");
  const storedBase = window.localStorage.getItem("apiBase");
  return (queryBase || storedBase || DEFAULT_API_BASE).replace(/\/$/, "");
}

async function api(path, options = {}) {
  const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
  if (state.session?.userId) headers["X-User-Id"] = String(state.session.userId);
  const response = await fetch(`${apiBase()}${path}`, { ...options, headers });

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
  if (!message) return;
  message.textContent = text;
  message.hidden = false;
  clearTimeout(showMessage.timer);
  showMessage.timer = window.setTimeout(() => {
    message.hidden = true;
  }, 3200);
}

function money(value) {
  const number = Number(value || 0);
  return number === 0 ? "免费" : `￥${number.toFixed(2)}`;
}

function formatBalance(value) {
  const number = Number(value || 0);
  return `￥${number.toFixed(2)}`;
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function shortText(text, size = 90) {
  if (!text) return "暂无内容";
  return text.length > size ? `${text.slice(0, size)}...` : text;
}

function roleLabel(role) {
  return { USER: "普通用户", CREATOR: "创作者", ADMIN: "管理员" }[role] || role || "-";
}

function saveSession(session) {
  state.session = session;
  window.localStorage.setItem("session", JSON.stringify(session));
  renderShell();
}

function clearSession() {
  state.session = null;
  state.selectedTemplate = null;
  window.localStorage.removeItem("session");
  renderShell();
}

function allowedViews() {
  if (!state.session) return [];
  const role = state.session.role;
  if (role === "ADMIN") return ["platform"];
  if (role === "CREATOR") return ["market", "use", "profile", "creator"];
  return ["market", "use", "profile"];
}

function renderShell() {
  const loggedIn = Boolean(state.session);
  $("#authView").classList.toggle("active", !loggedIn);
  $("#appShell").hidden = !loggedIn;
  $("#sessionPanel").innerHTML = loggedIn
    ? `<span class="session-name">${escapeHtml(state.session.username)} · ${roleLabel(state.session.role)}</span>
       <button id="logoutBtn" class="ghost-outline" type="button">退出登录</button>`
    : `<span class="muted">未登录</span>`;

  if (!loggedIn) {
    setViewTitle("auth");
    state.activeView = "auth";
    return;
  }

  const views = allowedViews();
  $("#navBar").innerHTML = views.map((view) => (
    `<button class="nav-item" data-view="${view}" type="button">${viewMeta[view][0]}</button>`
  )).join("") + `<button id="reloadBtn" class="ghost-outline nav-refresh" type="button">刷新数据</button>`;

  if (!views.includes(state.activeView)) {
    state.activeView = views[0];
  }
  setActiveView(state.activeView, { refresh: true });
}

function setViewTitle(view) {
  $("#viewTitle").textContent = viewMeta[view][0];
  $("#viewSubtitle").textContent = viewMeta[view][1];
}

function setActiveView(view, { refresh = false } = {}) {
  state.activeView = view;
  setViewTitle(view);
  $$(".nav-item").forEach((item) => item.classList.toggle("active", item.dataset.view === view));
  $$("#appShell .view").forEach((panel) => panel.classList.toggle("active", panel.id === `${view}View`));
  if (refresh) runSafely(refreshCurrentView);
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

async function login(account, password) {
  const result = await api("/auth/login", {
    method: "POST",
    body: JSON.stringify({ account, password }),
  });
  if (!result.success) {
    showMessage(result.message || "登录失败");
    return;
  }
  saveSession({
    userId: result.userId,
    username: result.username || account,
    role: result.role || "USER",
  });
  showMessage("登录成功");
}

async function loadTags() {
  const tags = await api("/tags");
  $("#tagSelect").innerHTML = `<option value="">全部标签</option>${tags
    .map((tag) => `<option value="${escapeHtml(tag)}">${escapeHtml(tag)}</option>`)
    .join("")}`;
}

async function loadTemplates(params = state.marketQuery, page = 1) {
  state.marketQuery = new URLSearchParams(params.toString());
  state.marketQuery.set("current", page);
  state.marketQuery.set("size", state.page.size);
  const query = state.marketQuery.toString();
  const data = await api(`/templates/search?${query}`);
  state.templates = data.records || data || [];
  state.page.current = data.current || page;
  state.page.total = data.total || 0;
  state.page.pages = data.pages || 1;
  renderTemplates();
  renderPagination();
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

function renderPagination() {
  const container = $("#pagination");
  const { current, pages, total, size } = state.page;

  const maxButtons = 5;
  let pageButtons = "";
  if (pages <= 1) {
    pageButtons = `<button class="page-current" type="button">1</button>`;
  } else {
    let start = Math.max(1, current - Math.floor(maxButtons / 2));
    let end = Math.min(pages, start + maxButtons - 1);
    if (end - start < maxButtons - 1) start = Math.max(1, end - maxButtons + 1);

    const buttons = [];
    for (let i = start; i <= end; i++) {
      buttons.push(`<button class="${i === current ? 'page-current' : ''}" data-page="${i}" type="button">${i}</button>`);
    }
    pageButtons = `${start > 1 ? '<button data-page="1" type="button">1</button><span class="page-info">...</span>' : ''}${buttons.join("")}${end < pages ? '<span class="page-info">...</span><button data-page="' + pages + '" type="button">' + pages + '</button>' : ''}`;
  }

  container.innerHTML = `
    <button data-page="${current - 1}" ${current <= 1 ? 'disabled' : ''} type="button">上一页</button>
    ${pageButtons}
    <button data-page="${current + 1}" ${current >= pages || pages <= 1 ? 'disabled' : ''} type="button">下一页</button>
    <span class="page-info">共 ${total} 条</span>
    <select id="pageSizeSelect">
      <option value="4" ${size === 4 ? 'selected' : ''}>4 条/页</option>
      <option value="12" ${size === 12 ? 'selected' : ''}>12 条/页</option>
      <option value="20" ${size === 20 ? 'selected' : ''}>20 条/页</option>
    </select>
  `;
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
      <h3>使用与购买</h3>
      <div class="card-actions">
        <button class="action-btn" data-purchase="${item.id}" type="button">购买/领取</button>
        <button class="ghost-outline" data-favorite="${item.id}" type="button">收藏</button>
      </div>
      <p class="muted">使用输入摘要</p>
      <textarea id="useSummary"></textarea>
      <div class="card-actions">
        <button class="action-btn" data-use="${item.id}" type="button">使用模板</button>
      </div>
      <pre id="useResult" class="prompt-preview" hidden></pre>
    </section>

    <section class="panel">
      <h3>提交评价</h3>
      <p class="muted">评价内容</p>
      <textarea id="reviewContent"></textarea>
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
      <div class="list">${renderReviewRows(item.reviews || [])}</div>
    </section>
  `;
}

function renderReviewRows(reviews) {
  if (!reviews.length) return `<div class="list-row muted">暂无评价</div>`;
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
  const isUser = state.session?.role === "USER";
  $("#profilePanel").innerHTML = `
    <section class="panel">
      <div class="split">
        <div>
          <h2>${escapeHtml(profile.username || "-")}</h2>
          <p class="muted">${escapeHtml(profile.email || "")} ${escapeHtml(profile.phone || "")}</p>
        </div>
        <div>
          <h3>当前余额</h3>
          <p class="price">${formatBalance(profile.balance)}</p>
        </div>
        <div>
          ${isUser
            ? '<button id="upgradeBtn" class="action-btn" type="button">成为创作者</button>'
            : '<span class="muted">创作者资料请在创作者看板查看</span>'}
        </div>
      </div>
    </section>
    <div class="split">
      ${renderList("我的收藏", profile.favorites, (item) => `${item.title} · 评分 ${item.avgScore ?? 0} · ${money(item.price)}`)}
      ${renderList("我的订单", profile.orders, (item) => `${item.orderNo} · ${item.templateTitle} · ${money(item.payAmount)}`)}
      ${renderList("使用记录", profile.usageLogs, (item) => `${item.templateTitle} · ${item.inputSummary}`)}
      ${renderList("余额流水", profile.balanceLogs, (item) => `${item.changeType} · ${money(item.changeAmount)} · 余额 ${money(item.balanceAfter)}`)}
    </div>
  `;
}

function renderUseCenter(profile) {
  const usableOrders = (profile.orders || []).filter((order) => order.templateId);
  return `
    <section class="panel">
      <div class="section-head">
        <h3>我的可用模板</h3>
        <span class="muted">购买或领取后可以在这里直接使用</span>
      </div>
      <div class="template-list">
        ${usableOrders.length ? usableOrders.map((order) => `
          <article class="template-row use-row">
            <div class="row-title">
              <strong>${escapeHtml(order.templateTitle || "-")}</strong>
              <p>订单：${escapeHtml(order.orderNo || "-")} · ${escapeHtml(order.payStatus || "-")} · ${money(order.payAmount)}</p>
            </div>
            <div class="use-input">
              <textarea data-use-owned-summary="${order.templateId}" placeholder="输入本次使用需求，例如：帮我写一段商品详情页文案"></textarea>
            </div>
            <div class="row-actions">
              <button class="action-btn" data-use-owned="${order.templateId}" type="button">使用模板</button>
            </div>
            <pre id="ownedUseResult-${order.templateId}" class="prompt-preview use-result" hidden></pre>
          </article>
        `).join("") : '<div class="empty-state">还没有可用模板，请先到模板市场购买或领取。</div>'}
      </div>
    </section>
    ${renderList("最近使用记录", profile.usageLogs, (item) => `${item.templateTitle} · ${item.inputSummary}`)}
  `;
}

async function loadUseCenter() {
  const profile = await api("/profile");
  $("#usePanel").innerHTML = renderUseCenter(profile);
}

function renderPublishForm(template = null) {
  const editing = Boolean(template);
  const tags = (template?.tags || []).join(", ");
  const priceType = Number(template?.price || 0) === 0 ? "FREE" : "PAID";
  return `
    <section class="panel">
      <div class="section-head">
        <h3>${editing ? "修改模板" : "发布模板"}</h3>
        <span class="muted">${editing ? "修改会生成新的模板版本" : "发布后会出现在模板市场和创作者看板"}</span>
      </div>
      <form id="publishTemplateForm" class="creator-form" data-template-id="${editing ? template.id : ""}">
        <input name="title" placeholder="模板标题" value="${escapeHtml(template?.title || "")}">
        <input name="sceneDesc" placeholder="适用场景" value="${escapeHtml(template?.scene || "")}">
        <textarea name="promptContent" placeholder="提示词内容">${escapeHtml(template?.promptContent || "")}</textarea>
        <input name="tags" placeholder="标签，用逗号分隔，例如：写作,办公" value="${escapeHtml(tags)}">
        <select name="priceType">
          <option value="FREE" ${priceType === "FREE" ? "selected" : ""}>免费</option>
          <option value="PAID" ${priceType === "PAID" ? "selected" : ""}>付费</option>
        </select>
        <input name="price" type="number" min="0" step="0.01" placeholder="价格" value="${editing ? Number(template.price || 0) : ""}">
        <div class="card-actions">
          <button class="action-btn" type="submit">${editing ? "保存修改" : "发布模板"}</button>
          ${editing ? '<button class="ghost-outline" data-cancel-edit-template type="button">取消编辑</button>' : ""}
        </div>
      </form>
    </section>
  `;
}

function renderCreatorTemplates(templates = []) {
  return `
    <section class="panel">
      <div class="section-head">
        <h3>我的模板</h3>
        <span class="muted">可修改或删除自己发布的模板</span>
      </div>
      <div class="template-list">
        ${templates.length ? templates.map((item) => `
          <article class="template-row">
            <div class="row-title">
              <strong>${escapeHtml(item.title)}</strong>
              <p>${escapeHtml(shortText(item.scene, 70))}</p>
            </div>
            <div class="tag-list">${renderTagList(item.tags || [])}</div>
            <div class="row-stats">
              <span>状态：${escapeHtml(item.status || "-")}</span>
              <span>使用：${item.useCount ?? 0}</span>
              <span>收藏：${item.favoriteCount ?? 0}</span>
              <span class="price">${money(item.price)}</span>
            </div>
            <div class="row-actions">
              <button class="ghost-outline" data-edit-template="${item.id}" type="button">修改</button>
              <button class="ghost-outline danger-btn" data-delete-template="${item.id}" type="button">删除</button>
            </div>
          </article>
        `).join("") : '<div class="empty-state">还没有发布模板</div>'}
      </div>
    </section>
  `;
}

function renderLineChart(points = []) {
  const width = 640;
  const height = 220;
  const padding = 28;
  const values = points.map((item) => Number(item.count || 0));
  const max = Math.max(1, ...values);
  const xStep = points.length > 1 ? (width - padding * 2) / (points.length - 1) : 0;
  const coords = points.map((item, index) => {
    const x = padding + index * xStep;
    const y = height - padding - (Number(item.count || 0) / max) * (height - padding * 2);
    return { x, y, item };
  });
  const path = coords.map((point, index) => `${index === 0 ? "M" : "L"} ${point.x.toFixed(1)} ${point.y.toFixed(1)}`).join(" ");
  const lastLabel = points.length ? String(points[points.length - 1].date || "").slice(5) : "-";
  const firstLabel = points.length ? String(points[0].date || "").slice(5) : "-";

  return `
    <section class="panel chart-panel">
      <div class="section-head">
        <h3>模板使用趋势</h3>
        <span class="muted">近 30 天使用量变化</span>
      </div>
      <svg class="line-chart" viewBox="0 0 ${width} ${height}" role="img" aria-label="模板使用趋势折线图">
        <line x1="${padding}" y1="${height - padding}" x2="${width - padding}" y2="${height - padding}" class="chart-axis"></line>
        <line x1="${padding}" y1="${padding}" x2="${padding}" y2="${height - padding}" class="chart-axis"></line>
        <path d="${path}" class="chart-line"></path>
        ${coords.map((point) => `
          <circle cx="${point.x.toFixed(1)}" cy="${point.y.toFixed(1)}" r="3.5" class="chart-dot">
            <title>${escapeHtml(point.item.date)}：${point.item.count || 0} 次</title>
          </circle>
        `).join("")}
        <text x="${padding}" y="${height - 6}" class="chart-label">${escapeHtml(firstLabel)}</text>
        <text x="${width - padding}" y="${height - 6}" class="chart-label" text-anchor="end">${escapeHtml(lastLabel)}</text>
        <text x="${padding + 4}" y="${padding - 8}" class="chart-label">${max} 次</text>
      </svg>
    </section>
  `;
}

function renderCreatorAnalytics(data) {
  const monthlyIncome = (data.income || []).filter((item) => Number(item.income || 0) > 0);
  const templateIncome = data.templateIncome || [];
  return `
    ${renderLineChart(data.trend || [])}
    <div class="split">
      <section class="panel">
        <div class="section-head">
          <h3>按月收入</h3>
          <span class="muted">来自真实付费订单</span>
        </div>
        <div class="list">
          ${monthlyIncome.length ? monthlyIncome.map((item) => `
            <div class="list-row income-row">
              <strong>${escapeHtml(item.month)}</strong>
              <span class="price">${money(item.income)}</span>
            </div>
          `).join("") : '<div class="list-row muted">暂无收入数据</div>'}
        </div>
      </section>
      <section class="panel">
        <div class="section-head">
          <h3>按模板收入</h3>
          <span class="muted">统计每个模板的付费订单</span>
        </div>
        <div class="list">
          ${templateIncome.length ? templateIncome.map((item) => `
            <div class="list-row income-row">
              <div>
                <strong>${escapeHtml(item.title)}</strong>
                <p>订单：${item.orderCount || 0} 笔</p>
              </div>
              <span class="price">${money(item.income)}</span>
            </div>
          `).join("") : '<div class="list-row muted">暂无模板收入数据</div>'}
        </div>
      </section>
    </div>
  `;
}

async function loadCreatorDashboard() {
  $("#creatorPanel").innerHTML = renderPublishForm();
  await loadDashboard("/stats/creator", $("#creatorPanel"), { append: true });
}

async function loadDashboard(path, target, { append = false } = {}) {
  const data = await api(path);
  const isAdmin = state.session?.role === "ADMIN";
  let adminSection = "";
  if (isAdmin && data.templates) {
    adminSection = `
      <section class="panel">
        <div class="section-head">
          <h3>免费活动管理</h3>
          <span class="muted">将模板加入限时免费活动并补充库存</span>
        </div>
        <div class="list">
          ${data.templates.map((item) => `
            <div class="list-row">
              <strong>${escapeHtml(item.title)}</strong>
              <p>价格：${money(item.price)} · 使用：${item.useCount ?? 0}</p>
              <button class="ghost-outline" data-set-free="${item.id}" type="button">加入免费活动</button>
            </div>
          `).join("")}
        </div>
      </section>`;
  }
  const html = `
    <div class="dashboard-grid">
      ${(data.statCards || []).map((stat) => `
        <section class="stat-card">
          <span class="muted">${escapeHtml(stat.label)}</span>
          <strong>${escapeHtml(stat.value)}</strong>
          ${stat.trend ? `<span>${escapeHtml(stat.trend)}</span>` : ""}
        </section>
      `).join("")}
    </div>
    ${adminSection}
    ${path.includes("/stats/creator") ? renderCreatorAnalytics(data) : ""}
    ${path.includes("/stats/creator") ? renderCreatorTemplates(data.templates || []) : ""}
    <div class="split">
      ${path.includes("/stats/creator") ? "" : renderList("热门模板", data.templates || [], (item) => `${item.title} · ${money(item.price)} · 使用 ${item.useCount || 0}`)}
      ${renderList("活动信息", data.campaigns || [], (item) => `${item.name} · ${item.templateTitle} · 剩余 ${item.remainingQuota}`)}
    </div>
  `;
  target.innerHTML = append ? target.innerHTML + html : html;
}

async function refreshCurrentView() {
  if (!state.session) return;
  if (state.activeView === "market") {
    await loadTags();
    await loadTemplates(state.marketQuery, state.page.current);
  }
  if (state.activeView === "use") await loadUseCenter();
  if (state.activeView === "profile") await loadProfile();
  if (state.activeView === "creator") await loadCreatorDashboard();
  if (state.activeView === "platform") await loadDashboard("/stats/platform", $("#platformPanel"));
}

/* ---- Auth tab switching ---- */
document.addEventListener("click", (event) => {
  const tab = event.target.closest("[data-auth-tab]");
  if (tab) {
    $$(".auth-tab").forEach((t) => t.classList.remove("active"));
    tab.classList.add("active");
    $("#loginForm").hidden = tab.dataset.authTab !== "login";
    $("#registerForm").hidden = tab.dataset.authTab !== "register";
  }
});

/* ---- Main click handler ---- */
document.addEventListener("click", (event) => {
  runSafely(async () => {
    if (event.target.closest("#logoutBtn")) {
      clearSession();
      return;
    }

    const navButton = event.target.closest(".nav-item");
    if (navButton) {
      setActiveView(navButton.dataset.view, { refresh: true });
      return;
    }

    if (event.target.closest("#reloadBtn")) {
      await refreshCurrentView();
      return;
    }

    if (event.target.closest("#upgradeBtn")) {
      const result = await api("/users/creator-apply", { method: "POST" });
      showMessage(result.message || "操作成功");
      if (result.success && result.role) {
        state.session.role = result.role;
        window.localStorage.setItem("session", JSON.stringify(state.session));
        renderShell();
      }
      return;
    }

    if (event.target.closest("[data-cancel-edit-template]")) {
      $("#creatorPanel").innerHTML = renderPublishForm();
      await loadDashboard("/stats/creator", $("#creatorPanel"), { append: true });
      return;
    }

    const editTemplateBtn = event.target.closest("[data-edit-template]");
    if (editTemplateBtn) {
      const detail = await api(`/templates/${editTemplateBtn.dataset.editTemplate}`);
      $("#creatorPanel").innerHTML = renderPublishForm(detail);
      await loadDashboard("/stats/creator", $("#creatorPanel"), { append: true });
      return;
    }

    const deleteTemplateBtn = event.target.closest("[data-delete-template]");
    if (deleteTemplateBtn) {
      if (!window.confirm("确定删除这个模板吗？删除后不会在模板市场展示。")) return;
      const result = await api(`/templates/${deleteTemplateBtn.dataset.deleteTemplate}`, { method: "DELETE" });
      showMessage(result.message || "模板已删除");
      await loadCreatorDashboard();
      if (state.activeView === "market") await loadTemplates(state.marketQuery, state.page.current);
      return;
    }

    const setFreeBtn = event.target.closest("[data-set-free]");
    if (setFreeBtn) {
      const id = setFreeBtn.dataset.setFree;
      const result = await api(`/admin/templates/${id}/free`, { method: "POST" });
      showMessage(result.message || "操作成功");
      await refreshCurrentView();
      return;
    }

    if (event.target.closest("[data-close-modal]") || event.target.closest("#closeModalBtn")) {
      closeModal();
      return;
    }

    const pageButton = event.target.closest("[data-page]");
    if (pageButton) {
      const targetPage = Number(pageButton.dataset.page);
      if (targetPage >= 1 && targetPage <= state.page.pages) {
        await loadTemplates(state.marketQuery, targetPage);
      }
      return;
    }

    const favoriteButton = event.target.closest("[data-favorite]");
    if (favoriteButton) {
      const id = favoriteButton.dataset.favorite;
      const result = await api(`/templates/${id}/favorite`, { method: "POST" });
      showMessage(result.message || "操作成功");
      await loadTemplates(state.marketQuery, state.page.current);
      if (state.selectedTemplate?.id === Number(id)) await selectTemplate(id, { open: false });
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
      await loadTemplates(state.marketQuery, state.page.current);
      if (state.selectedTemplate?.id === Number(id)) await selectTemplate(id, { open: false });
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
      if (result.usageResult || result.promptContent) {
        $("#useResult").textContent = result.usageResult || result.promptContent;
        $("#useResult").hidden = false;
      }
      await loadTemplates(state.marketQuery, state.page.current);
      await selectTemplate(id, { open: false });
      return;
    }

    const ownedUseButton = event.target.closest("[data-use-owned]");
    if (ownedUseButton) {
      const id = ownedUseButton.dataset.useOwned;
      const summary = document.querySelector(`[data-use-owned-summary="${id}"]`)?.value || "";
      const result = await api(`/templates/${id}/use`, {
        method: "POST",
        body: JSON.stringify({ inputSummary: summary }),
      });
      showMessage(result.message || "模板使用成功");
      const resultBox = $(`#ownedUseResult-${id}`);
      if (resultBox) {
        resultBox.textContent = result.usageResult || result.promptContent || "";
        resultBox.hidden = false;
      }
      await loadTemplates(state.marketQuery, state.page.current);
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
      await loadTemplates(state.marketQuery, state.page.current);
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
  if (event.key === "Escape" && !$("#templateModal").hidden) closeModal();
});

$("#loginForm").addEventListener("submit", (event) => {
  runSafely(async () => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    await login(form.get("account"), form.get("password"));
  });
});

$("#registerForm").addEventListener("submit", (event) => {
  event.preventDefault();
  runSafely(async () => {
    const form = new FormData(event.currentTarget);
    const payload = Object.fromEntries(form.entries());
    const result = await api("/auth/register", {
      method: "POST",
      body: JSON.stringify(payload),
    });
    if (!result.success) {
      showMessage(result.message || "注册失败");
      return;
    }
    await login(payload.username, payload.password);
  });
});

document.addEventListener("submit", (event) => {
  if (event.target.id !== "publishTemplateForm") return;
  event.preventDefault();
  runSafely(async () => {
    const form = new FormData(event.target);
    const templateId = event.target.dataset.templateId;
    const priceType = form.get("priceType");
    const price = priceType === "FREE" ? 0 : Number(form.get("price") || 0);
    const payload = {
      title: form.get("title"),
      sceneDesc: form.get("sceneDesc"),
      promptContent: form.get("promptContent"),
      priceType,
      price,
      tags: String(form.get("tags") || "")
        .split(/[,，]/)
        .map((tag) => tag.trim())
        .filter(Boolean),
    };
    const result = await api(templateId ? `/templates/${templateId}` : "/templates", {
      method: templateId ? "PUT" : "POST",
      body: JSON.stringify(payload),
    });
    if (!result.success) {
      showMessage(result.message || "操作失败");
      return;
    }
    event.target.reset();
    showMessage(result.message || "操作成功");
    state.page.current = 1;
    await loadCreatorDashboard();
  });
});

$("#searchForm").addEventListener("submit", (event) => {
  runSafely(async () => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const params = new URLSearchParams();
    for (const [key, value] of form.entries()) {
      if (value) params.set(key, value);
    }
    state.page.current = 1;
    await loadTemplates(params, 1);
  });
});

$$("#pageSizeSelect").forEach((select) => {
  select.addEventListener("change", async (event) => {
    state.page.size = Number(event.target.value);
    state.page.current = 1;
    await loadTemplates(state.marketQuery, 1);
  });
});

/* Delegate pageSizeSelect change (created dynamically) */
document.addEventListener("change", (event) => {
  if (event.target.id === "pageSizeSelect") {
    runSafely(async () => {
      state.page.size = Number(event.target.value);
      state.page.current = 1;
      await loadTemplates(state.marketQuery, 1);
    });
  }
});

renderShell();
