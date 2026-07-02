import fs from "node:fs/promises";
import { Presentation, PresentationFile } from "@oai/artifact-tool";

const OUT = "F:/数据库原理课设/deliverables/AI提示词模板平台课程设计汇报.pptx";
const PREVIEW_DIR = "F:/数据库原理课设/work/ppt_builder/preview";

async function writeBlob(path, blob) {
  await fs.writeFile(path, new Uint8Array(await blob.arrayBuffer()));
}

const W = 1280;
const H = 720;
const M = 54;
const ink = "#111111";
const muted = "#555555";
const panel = "#EDEDED";
const rule = "#B8BCC4";
const highlight = "#FF6B35";

function addText(slide, text, x, y, w, h, style = {}) {
  const shape = slide.shapes.add({
    geometry: "textbox",
    position: { left: x, top: y, width: w, height: h },
    fill: "none",
    line: { style: "solid", fill: "none", width: 0 },
  });
  shape.text = text;
  shape.text.style = {
    fontSize: style.fontSize ?? 22,
    bold: style.bold ?? false,
    color: style.color ?? ink,
    alignment: style.alignment ?? "left",
  };
  return shape;
}

function addBox(slide, x, y, w, h, fill = panel, lineFill = "none") {
  return slide.shapes.add({
    geometry: "rect",
    position: { left: x, top: y, width: w, height: h },
    fill,
    line: { style: "solid", fill: lineFill, width: lineFill === "none" ? 0 : 1 },
  });
}

function addRule(slide, x, y, w) {
  addBox(slide, x, y, w, 2, rule);
}

function titleSlide(slide, title, subtitle, kicker = "数据库原理课程设计") {
  slide.background.fill = "#FFFFFF";
  addText(slide, kicker, M, 44, 360, 32, { fontSize: 18, bold: true, color: muted });
  addRule(slide, M, 92, 1170);
  addText(slide, title, M, 166, 840, 178, { fontSize: 58, bold: true });
  addText(slide, subtitle, M, 366, 760, 84, { fontSize: 25, color: muted });
  addBox(slide, 884, 160, 308, 308, panel);
  addText(slide, "AI\nPrompt\nPlatform", 914, 198, 250, 210, { fontSize: 46, bold: true, alignment: "center" });
  addText(slide, "Spring Boot / MyBatis-Plus / H2 / MySQL / Frontend", M, 618, 840, 42, { fontSize: 19, color: muted });
}

function sectionTitle(slide, title, subtitle) {
  addText(slide, title, M, 42, 900, 60, { fontSize: 39, bold: true });
  addText(slide, subtitle, M, 104, 920, 34, { fontSize: 18, color: muted });
  addRule(slide, M, 150, 1170);
}

function addBullets(slide, items, x, y, w, gap = 54, fontSize = 22) {
  items.forEach((item, i) => {
    const top = y + i * gap;
    addBox(slide, x, top + 9, 10, 10, highlight);
    addText(slide, item, x + 26, top, w - 26, gap - 4, { fontSize, color: ink });
  });
}

function addCard(slide, title, body, x, y, w, h, accent = false) {
  addBox(slide, x, y, w, h, accent ? "#FFF1EA" : panel, accent ? highlight : "none");
  addText(slide, title, x + 18, y + 18, w - 36, 36, { fontSize: 24, bold: true });
  addText(slide, body, x + 18, y + 66, w - 36, h - 82, { fontSize: 18, color: muted });
}

function addTableLike(slide, headers, rows, x, y, widths, rowH = 54) {
  const totalW = widths.reduce((a, b) => a + b, 0);
  addBox(slide, x, y, totalW, rowH, panel, rule);
  let cx = x;
  headers.forEach((h, i) => {
    addText(slide, h, cx + 10, y + 14, widths[i] - 20, 26, { fontSize: 17, bold: true });
    cx += widths[i];
  });
  rows.forEach((row, r) => {
    const top = y + rowH * (r + 1);
    addBox(slide, x, top, totalW, rowH, "#FFFFFF", rule);
    cx = x;
    row.forEach((cell, i) => {
      addText(slide, cell, cx + 10, top + 12, widths[i] - 20, rowH - 18, { fontSize: 15.5, color: i === 0 ? ink : muted });
      cx += widths[i];
    });
  });
}

function createDeck() {
  const deck = Presentation.create({ slideSize: { width: W, height: H } });

  let slide = deck.slides.add();
  titleSlide(slide, "AI提示词模板平台", "围绕模板版本、交易使用、评价统计构建的数据库应用系统");

  slide = deck.slides.add();
  sectionTitle(slide, "课题要解决的是提示词模板的完整业务闭环", "系统不是单纯模板列表，而是覆盖检索、交易、使用、评价和统计。");
  addCard(slide, "设计目标", "实现一个可运行、可演示、结构清晰的前后端分离数据库应用系统。", M, 188, 350, 152, true);
  addCard(slide, "设计要求", "完成需求分析、E-R模型、逻辑和物理设计、功能实现、并发与查询优化实验。", 456, 188, 350, 152);
  addCard(slide, "小组分工", "成员A负责数据库与后端；成员B负责前端、测试、文档和演示材料。", 858, 188, 350, 152);
  addBullets(slide, ["数据库表结构覆盖核心业务数据", "后端统一编排服务组织领域流程", "前端独立调用 REST API 完成演示"], M, 420, 980, 52, 23);

  slide = deck.slides.add();
  sectionTitle(slide, "三类用户共同驱动系统功能划分", "普通用户、创作者、管理员分别对应消费、生产和平台治理视角。");
  addTableLike(slide,
    ["角色", "核心用例", "系统响应"],
    [
      ["普通用户", "搜索、收藏、购买、使用、评价", "生成订单、校验权限、记录使用日志"],
      ["创作者", "查看模板、版本、趋势、收益", "聚合模板使用量和收入数据"],
      ["管理员", "查看平台总览、排行、活动", "统计用户、模板、交易和活动状态"],
    ],
    M, 190, [180, 480, 520], 78);
  addText(slide, "用例关系：用户围绕模板产生收藏、订单、使用日志和评价；创作者围绕模板产生版本和统计；管理员围绕平台数据进行监控。", M, 520, 1040, 72, { fontSize: 24, bold: true });

  slide = deck.slides.add();
  sectionTitle(slide, "后端保留统一入口，领域服务负责具体业务", "Controller 只依赖 PortalService，应用服务再编排模板、交易、用户和统计服务。");
  const layers = [
    ["前端层", "frontend/index.html / app.js"],
    ["接口层", "ApiController"],
    ["编排层", "PortalApplicationService"],
    ["领域层", "Template / Trade / User / Dashboard / Analytics Service"],
    ["持久层", "Mapper + XML SQL + MyBatis-Plus"],
    ["数据库层", "H2 演示库 / MySQL 8.0"],
  ];
  layers.forEach((layer, i) => {
    const top = 180 + i * 70;
    addBox(slide, 122, top, 1036, 48, i === 2 ? "#FFF1EA" : panel, i === 2 ? highlight : "none");
    addText(slide, layer[0], 148, top + 10, 160, 28, { fontSize: 20, bold: true });
    addText(slide, layer[1], 336, top + 10, 780, 28, { fontSize: 20, color: muted });
  });

  slide = deck.slides.add();
  sectionTitle(slide, "数据库概念模型围绕模板实体展开", "模板是中心实体，版本、标签、订单、使用和评价围绕它形成业务闭环。");
  addBox(slide, 486, 300, 300, 84, "#FFF1EA", highlight);
  addText(slide, "PromptTemplate\n模板", 512, 312, 248, 58, { fontSize: 27, bold: true, alignment: "center" });
  const entities = [
    ["SysUser\n用户", 88, 192],
    ["Version\n模板版本", 486, 174],
    ["Tag\n标签", 890, 192],
    ["Order\n订单", 92, 456],
    ["UseLog\n使用日志", 486, 500],
    ["Review\n评价", 890, 456],
  ];
  entities.forEach(([label, x, y]) => {
    addBox(slide, x, y, 250, 72, panel, rule);
    addText(slide, label, x + 18, y + 15, 214, 42, { fontSize: 22, bold: true, alignment: "center" });
  });
  addText(slide, "创作 / 购买", 256, 278, 180, 30, { fontSize: 18, color: muted, alignment: "center" });
  addText(slide, "1:N", 572, 252, 90, 28, { fontSize: 20, bold: true, color: highlight, alignment: "center" });
  addText(slide, "N:M", 812, 278, 90, 28, { fontSize: 20, bold: true, color: highlight, alignment: "center" });
  addText(slide, "订单 -> 使用 -> 评价", 382, 424, 520, 36, { fontSize: 24, bold: true, alignment: "center" });

  slide = deck.slides.add();
  sectionTitle(slide, "逻辑与物理设计同时服务业务完整性和查询效率", "表结构覆盖业务关系，索引面向搜索、排行、趋势和用户中心查询。");
  addTableLike(slide,
    ["数据域", "核心表", "设计重点"],
    [
      ["用户域", "sys_user, user_balance_log", "账号、余额、流水审计"],
      ["模板域", "prompt_template, prompt_template_version", "当前版本指针与历史版本"],
      ["分类域", "tag, template_tag_rel", "多级标签和多对多关系"],
      ["交易域", "template_order, template_use_log, template_review", "购买、使用、反馈闭环"],
      ["活动域", "free_campaign, campaign_template, campaign_claim", "限免库存和防重复领取"],
    ],
    M, 178, [150, 430, 600], 62);
  addText(slide, "关键索引：idx_template_filter、idx_template_hot_sort、idx_use_query、idx_order_query。", M, 586, 1050, 34, { fontSize: 24, bold: true });

  slide = deck.slides.add();
  sectionTitle(slide, "购买和使用流程体现事务边界与权限校验", "优化后的流程避免重复购买和未购买直接使用付费模板。");
  const steps = [
    ["1", "校验模板状态"],
    ["2", "判断已有订单"],
    ["3", "余额校验与扣减"],
    ["4", "生成订单和流水"],
    ["5", "使用前校验权限"],
    ["6", "记录版本与日志"],
  ];
  steps.forEach((step, i) => {
    const x = M + i * 196;
    addBox(slide, x, 238, 150, 150, i === 1 || i === 4 ? "#FFF1EA" : panel, i === 1 || i === 4 ? highlight : "none");
    addText(slide, step[0], x + 48, 252, 54, 50, { fontSize: 43, bold: true, alignment: "center", color: highlight });
    addText(slide, step[1], x + 16, 318, 118, 52, { fontSize: 20, bold: true, alignment: "center" });
    if (i < steps.length - 1) addText(slide, "→", x + 154, 292, 42, 42, { fontSize: 35, bold: true, color: muted, alignment: "center" });
  });
  addBullets(slide, ["重复购买直接返回已有成功订单", "付费模板必须先购买才能使用", "使用日志记录当前版本与订单编号"], M, 480, 1020, 46, 22);

  slide = deck.slides.add();
  sectionTitle(slide, "前端页面围绕演示路径设计", "一个静态前端完成模板市场、详情、个人中心和看板演示。");
  addCard(slide, "模板市场", "搜索、标签筛选、排序、评分过滤；卡片展示价格、评分、收藏数和使用量。", M, 190, 350, 166, true);
  addCard(slide, "详情与交易", "详情面板展示当前版本提示词，并提供购买、收藏、使用和评价操作。", 456, 190, 350, 166);
  addCard(slide, "统计看板", "个人中心、创作者看板和平台总览均通过后端 API 获取数据。", 858, 190, 350, 166);
  addText(slide, "前端默认请求 http://localhost:8080/api，也可以在页面左侧手动修改后端地址。", M, 470, 1020, 56, { fontSize: 27, bold: true });

  slide = deck.slides.add();
  sectionTitle(slide, "并发控制和查询优化是本系统的数据库实验重点", "实验关注数据一致性和高频查询性能，而不是只完成页面展示。");
  addTableLike(slide,
    ["实验", "优化处理", "结论"],
    [
      ["重复购买", "查询已有 SUCCESS 订单", "接口具备幂等性"],
      ["计数更新", "SQL 原子递增/递减", "降低丢失更新风险"],
      ["模板搜索", "SQL 条件过滤 + 分页", "避免内存过滤和分页失真"],
      ["热门排行", "SQL 聚合近期使用量", "减少 Java 侧全量计算"],
    ],
    M, 184, [210, 470, 500], 74);
  addText(slide, "后端 MockMvc 测试覆盖 7 个核心用例，验证查询、登录、购买幂等、权限校验和取消收藏。", M, 570, 1060, 44, { fontSize: 24, bold: true });

  slide = deck.slides.add();
  sectionTitle(slide, "系统已完成核心闭环，后续可增强真实生产能力", "课程设计版本强调数据库设计完整性、业务流程可演示和结构可扩展。");
  addCard(slide, "已完成", "前后端分离、核心表结构、统一服务编排、模板交易使用闭环、统计分析和自动化测试。", M, 190, 350, 190, true);
  addCard(slide, "可改进", "接入真实登录态、完善活动领取、增强余额并发扣减、增加模板编辑审核和语义推荐。", 456, 190, 350, 190);
  addCard(slide, "提交材料", "课程设计报告、汇报PPT和源代码压缩包；文件名按“小组人员姓名.zip”提交。", 858, 190, 350, 190);
  addText(slide, "答辩演示建议路径：启动后端 → 打开前端 → 搜索模板 → 查看详情 → 购买/使用 → 查看个人中心和平台看板。", M, 500, 1090, 72, { fontSize: 27, bold: true });

  return deck;
}

async function main() {
  await fs.mkdir(PREVIEW_DIR, { recursive: true });
  const deck = createDeck();
  for (const [index, slide] of deck.slides.items.entries()) {
    const stem = `slide-${String(index + 1).padStart(2, "0")}`;
    await writeBlob(`${PREVIEW_DIR}/${stem}.png`, await deck.export({ slide, format: "png", scale: 1 }));
    const layout = await slide.export({ format: "layout" });
    await fs.writeFile(`${PREVIEW_DIR}/${stem}.layout.json`, await layout.text());
  }
  await writeBlob(`${PREVIEW_DIR}/montage.webp`, await deck.export({ format: "webp", montage: true, scale: 1 }));
  const pptx = await PresentationFile.exportPptx(deck);
  await pptx.save(OUT);
  console.log(OUT);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
