INSERT INTO sys_user (user_id, username, password_hash, phone, email, creator_score, creator_level, balance, user_status)
VALUES
    (1, 'demo_user', 'demo_hash', '13800138000', 'demo@example.com', 860, 'A级创作者', 299.00, 'ACTIVE'),
    (2, 'creator_one', 'creator_hash', '13900139000', 'creator@example.com', 1280, 'S级创作者', 980.00, 'ACTIVE');

INSERT INTO prompt_template (template_id, creator_user_id, title, scene_desc, price_type, price, shelf_status, use_count, favorite_count, avg_score, review_count)
VALUES
    (101, 2, '学术论文润色助手', '适用于毕业论文、课程论文和研究报告润色', 'FREE', 0.00, 'ON_SHELF', 1280, 365, 4.8, 2),
    (102, 2, 'Java 代码评审 Prompt', '适用于 Java 后端代码评审、重构建议和风险识别', 'PAID', 29.90, 'ON_SHELF', 2430, 812, 4.9, 2),
    (103, 2, '电商文案转化模板', '适用于商品详情页、直播口播和营销活动文案', 'PAID', 19.90, 'ON_SHELF', 1670, 539, 4.7, 2);

INSERT INTO prompt_template_version (version_id, template_id, version_no, prompt_content, change_note, editor_user_id, created_at)
VALUES
    (1001, 101, 'v1.0.0', '请将以下学术内容润色为规范论文语言，保留观点并提升逻辑性。', '初始化模板', 2, NOW() - INTERVAL 30 DAY),
    (1002, 102, 'v1.0.0', '请从正确性、性能、可维护性和安全性四个维度评审以下 Java 代码。', '初始化模板', 2, NOW() - INTERVAL 25 DAY),
    (1003, 102, 'v1.1.0', '请从正确性、性能、可维护性和安全性四个维度评审以下 Java 代码，并以问题列表输出。', '增加结构化输出', 2, NOW() - INTERVAL 12 DAY),
    (1004, 103, 'v1.0.0', '请根据产品卖点、用户痛点和促销信息生成高转化营销文案。', '初始化模板', 2, NOW() - INTERVAL 18 DAY);

UPDATE prompt_template SET current_version_id = 1001 WHERE template_id = 101;
UPDATE prompt_template SET current_version_id = 1003 WHERE template_id = 102;
UPDATE prompt_template SET current_version_id = 1004 WHERE template_id = 103;

INSERT INTO tag (tag_id, tag_name, parent_tag_id, tag_level, tag_path, sort_no)
VALUES
    (1, '写作', NULL, 1, '/写作', 1),
    (2, '编程', NULL, 1, '/编程', 2),
    (3, '营销', NULL, 1, '/营销', 3),
    (4, 'Java', 2, 2, '/编程/Java', 1),
    (5, '论文', 1, 2, '/写作/论文', 1);

INSERT INTO template_tag_rel (template_id, tag_id)
VALUES
    (101, 1), (101, 5),
    (102, 2), (102, 4),
    (103, 3);

INSERT INTO template_favorite (user_id, template_id)
VALUES
    (1, 102), (1, 103);

INSERT INTO template_order (order_id, order_no, user_id, template_id, origin_amount, pay_amount, pay_status, order_status, pay_time, created_at)
VALUES
    (5001, 'ORD202606280001', 1, 102, 29.90, 29.90, 'PAID', 'SUCCESS', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY);

INSERT INTO user_balance_log (user_id, change_type, change_amount, balance_before, balance_after, biz_type, biz_id, created_at)
VALUES
    (1, '消费', -29.90, 328.90, 299.00, '模板购买', 5001, NOW() - INTERVAL 1 DAY);

INSERT INTO template_use_log (use_log_id, user_id, template_id, version_id, order_id, input_summary, use_source, used_at)
VALUES
    (7001, 1, 102, 1003, 5001, '对订单事务代码进行评审', 'WEB', NOW() - INTERVAL 6 HOUR),
    (7002, 1, 101, 1001, NULL, '润色数据库设计章节', 'WEB', NOW() - INTERVAL 1 DAY);

INSERT INTO template_review (user_id, template_id, use_log_id, score, review_content, review_status, created_at)
VALUES
    (1, 102, 7001, 5, '结构清晰，适合直接用于代码评审场景。', 'VISIBLE', NOW() - INTERVAL 3 DAY),
    (1, 101, 7002, 4, '对论文语言润色很有帮助。', 'VISIBLE', NOW() - INTERVAL 1 DAY);

INSERT INTO free_campaign (campaign_id, campaign_name, start_time, end_time, campaign_status)
VALUES
    (9001, '毕业季限时免费', NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 2 DAY, 'RUNNING');

INSERT INTO campaign_template (campaign_template_id, campaign_id, template_id, total_quota, remaining_quota, per_user_limit)
VALUES
    (9101, 9001, 102, 500, 173, 1);
