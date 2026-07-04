CREATE DATABASE IF NOT EXISTS ai_prompt_template_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ai_prompt_template_platform;

CREATE TABLE IF NOT EXISTS sys_user (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户主键',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password_hash VARCHAR(100) NOT NULL COMMENT '密码哈希',
    phone VARCHAR(20) UNIQUE COMMENT '手机号',
    email VARCHAR(100) UNIQUE COMMENT '邮箱',
    creator_score INT NOT NULL DEFAULT 0 COMMENT '创作者积分，用于计算等级',
    creator_level VARCHAR(20) DEFAULT NULL COMMENT '创作者等级，升级后设置',
    balance DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '账户余额',
    user_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '账户状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='用户表：保存账户基础信息与展示用等级字段';

CREATE TABLE IF NOT EXISTS user_balance_log (
    log_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '余额流水主键',
    user_id BIGINT NOT NULL COMMENT '关联用户',
    change_type VARCHAR(30) NOT NULL COMMENT '变动类型，如消费、收入、退款',
    change_amount DECIMAL(10, 2) NOT NULL COMMENT '变动金额',
    balance_before DECIMAL(10, 2) NOT NULL COMMENT '变动前余额',
    balance_after DECIMAL(10, 2) NOT NULL COMMENT '变动后余额',
    biz_type VARCHAR(30) NOT NULL COMMENT '业务类型',
    biz_id BIGINT COMMENT '关联业务主键',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    CONSTRAINT fk_balance_log_user FOREIGN KEY (user_id) REFERENCES sys_user(user_id)
) COMMENT='余额流水表：用于说明购买扣款与收入入账的审计链';

CREATE TABLE IF NOT EXISTS prompt_template (
    template_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '模板主键',
    creator_user_id BIGINT NOT NULL COMMENT '创建人用户 ID',
    title VARCHAR(120) NOT NULL COMMENT '模板标题',
    scene_desc VARCHAR(255) NOT NULL COMMENT '适用场景描述',
    current_version_id BIGINT COMMENT '当前生效版本 ID',
    price_type VARCHAR(20) NOT NULL DEFAULT 'PAID' COMMENT '价格类型，FREE 或 PAID',
    price DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '模板售价',
    shelf_status VARCHAR(20) NOT NULL DEFAULT 'ON_SHELF' COMMENT '上下架状态',
    use_count INT NOT NULL DEFAULT 0 COMMENT '累计使用次数，冗余统计字段',
    favorite_count INT NOT NULL DEFAULT 0 COMMENT '收藏数，冗余统计字段',
    avg_score DECIMAL(3, 1) NOT NULL DEFAULT 0.0 COMMENT '平均评分，冗余统计字段',
    review_count INT NOT NULL DEFAULT 0 COMMENT '评价数量',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT fk_template_user FOREIGN KEY (creator_user_id) REFERENCES sys_user(user_id)
) COMMENT='模板主表：保存模板的当前展示信息和统计字段';

CREATE TABLE IF NOT EXISTS prompt_template_version (
    version_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '版本主键',
    template_id BIGINT NOT NULL COMMENT '所属模板',
    version_no VARCHAR(20) NOT NULL COMMENT '版本号',
    prompt_content TEXT NOT NULL COMMENT 'Prompt 具体内容',
    change_note VARCHAR(255) NOT NULL COMMENT '版本变更说明',
    editor_user_id BIGINT NOT NULL COMMENT '编辑人',
    source_version_id BIGINT COMMENT '基于哪个版本继续修改',
    rollback_from_version_id BIGINT COMMENT '若是回滚生成，则记录被回滚的版本',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    CONSTRAINT uk_template_version UNIQUE (template_id, version_no),
    CONSTRAINT fk_version_template FOREIGN KEY (template_id) REFERENCES prompt_template(template_id),
    CONSTRAINT fk_version_editor FOREIGN KEY (editor_user_id) REFERENCES sys_user(user_id)
) COMMENT='模板版本表：每次编辑新增记录，满足历史查看、对比与回滚';

ALTER TABLE prompt_template
    ADD CONSTRAINT fk_template_current_version FOREIGN KEY (current_version_id) REFERENCES prompt_template_version(version_id);

CREATE TABLE IF NOT EXISTS tag (
    tag_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '标签主键',
    tag_name VARCHAR(50) NOT NULL COMMENT '标签名称',
    parent_tag_id BIGINT COMMENT '父标签 ID',
    tag_level INT NOT NULL DEFAULT 1 COMMENT '层级深度',
    tag_path VARCHAR(255) COMMENT '层级路径，便于快速展示',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    CONSTRAINT uk_tag_name_parent UNIQUE (tag_name, parent_tag_id),
    CONSTRAINT fk_tag_parent FOREIGN KEY (parent_tag_id) REFERENCES tag(tag_id)
) COMMENT='标签表：通过自关联支持多级分类';

CREATE TABLE IF NOT EXISTS template_tag_rel (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关系主键',
    template_id BIGINT NOT NULL COMMENT '模板 ID',
    tag_id BIGINT NOT NULL COMMENT '标签 ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    CONSTRAINT uk_template_tag UNIQUE (template_id, tag_id),
    CONSTRAINT fk_rel_template FOREIGN KEY (template_id) REFERENCES prompt_template(template_id),
    CONSTRAINT fk_rel_tag FOREIGN KEY (tag_id) REFERENCES tag(tag_id)
) COMMENT='模板标签关联表：用于多标签检索';

CREATE TABLE IF NOT EXISTS template_favorite (
    favorite_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '收藏主键',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    template_id BIGINT NOT NULL COMMENT '模板 ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    CONSTRAINT uk_user_template_favorite UNIQUE (user_id, template_id),
    CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES sys_user(user_id),
    CONSTRAINT fk_favorite_template FOREIGN KEY (template_id) REFERENCES prompt_template(template_id)
) COMMENT='模板收藏表：既保留谁收藏了什么，也支持统计收藏数';

CREATE TABLE IF NOT EXISTS template_order (
    order_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单主键',
    order_no VARCHAR(40) NOT NULL UNIQUE COMMENT '订单编号',
    user_id BIGINT NOT NULL COMMENT '购买用户 ID',
    template_id BIGINT NOT NULL COMMENT '模板 ID',
    origin_amount DECIMAL(10, 2) NOT NULL COMMENT '原始金额',
    pay_amount DECIMAL(10, 2) NOT NULL COMMENT '实际支付金额',
    pay_status VARCHAR(20) NOT NULL COMMENT '支付状态',
    order_status VARCHAR(20) NOT NULL COMMENT '订单状态',
    pay_time DATETIME COMMENT '支付时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES sys_user(user_id),
    CONSTRAINT fk_order_template FOREIGN KEY (template_id) REFERENCES prompt_template(template_id)
) COMMENT='订单表：模板购买成功后生成交易记录';

CREATE TABLE IF NOT EXISTS template_use_log (
    use_log_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '使用日志主键',
    user_id BIGINT NOT NULL COMMENT '使用用户',
    template_id BIGINT NOT NULL COMMENT '模板 ID',
    version_id BIGINT NOT NULL COMMENT '当次使用的模板版本',
    order_id BIGINT COMMENT '如为付费模板可关联订单',
    input_summary VARCHAR(255) NOT NULL COMMENT '输入参数摘要',
    use_source VARCHAR(30) NOT NULL DEFAULT 'WEB' COMMENT '使用来源',
    used_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '使用时间',
    CONSTRAINT fk_use_user FOREIGN KEY (user_id) REFERENCES sys_user(user_id),
    CONSTRAINT fk_use_template FOREIGN KEY (template_id) REFERENCES prompt_template(template_id),
    CONSTRAINT fk_use_version FOREIGN KEY (version_id) REFERENCES prompt_template_version(version_id),
    CONSTRAINT fk_use_order FOREIGN KEY (order_id) REFERENCES template_order(order_id)
) COMMENT='模板使用日志表：用于近 7 天使用量与趋势统计';

CREATE TABLE IF NOT EXISTS template_review (
    review_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评价主键',
    user_id BIGINT NOT NULL COMMENT '评价用户',
    template_id BIGINT NOT NULL COMMENT '模板 ID',
    use_log_id BIGINT NOT NULL COMMENT '关联使用日志，保证先使用后评价',
    score TINYINT NOT NULL COMMENT '评分 1-5',
    review_content VARCHAR(500) NOT NULL COMMENT '文字评价',
    review_status VARCHAR(20) NOT NULL DEFAULT 'VISIBLE' COMMENT '评价状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT chk_review_score CHECK (score BETWEEN 1 AND 5),
    CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES sys_user(user_id),
    CONSTRAINT fk_review_template FOREIGN KEY (template_id) REFERENCES prompt_template(template_id),
    CONSTRAINT fk_review_use_log FOREIGN KEY (use_log_id) REFERENCES template_use_log(use_log_id)
) COMMENT='评分评价表：用户使用模板后可评分并撰写评价';

CREATE TABLE IF NOT EXISTS free_campaign (
    campaign_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '活动主键',
    campaign_name VARCHAR(100) NOT NULL COMMENT '活动名称',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',
    campaign_status VARCHAR(20) NOT NULL COMMENT '活动状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) COMMENT='限时免费活动主表：管理活动时间段与状态';

CREATE TABLE IF NOT EXISTS campaign_template (
    campaign_template_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '活动模板关系主键',
    campaign_id BIGINT NOT NULL COMMENT '活动 ID',
    template_id BIGINT NOT NULL COMMENT '模板 ID',
    total_quota INT NOT NULL COMMENT '总份数',
    remaining_quota INT NOT NULL COMMENT '剩余份数',
    per_user_limit INT NOT NULL DEFAULT 1 COMMENT '每用户限领次数',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    CONSTRAINT uk_campaign_template UNIQUE (campaign_id, template_id),
    CONSTRAINT fk_campaign_template_campaign FOREIGN KEY (campaign_id) REFERENCES free_campaign(campaign_id),
    CONSTRAINT fk_campaign_template_template FOREIGN KEY (template_id) REFERENCES prompt_template(template_id)
) COMMENT='活动模板表：控制限时免费库存';

CREATE TABLE IF NOT EXISTS campaign_claim (
    claim_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '领取记录主键',
    campaign_template_id BIGINT NOT NULL COMMENT '活动模板关系 ID',
    user_id BIGINT NOT NULL COMMENT '领取用户',
    order_id BIGINT COMMENT '零元订单或权益记录',
    claim_status VARCHAR(20) NOT NULL COMMENT '领取状态',
    claim_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
    CONSTRAINT uk_campaign_user UNIQUE (campaign_template_id, user_id),
    CONSTRAINT fk_claim_campaign_template FOREIGN KEY (campaign_template_id) REFERENCES campaign_template(campaign_template_id),
    CONSTRAINT fk_claim_user FOREIGN KEY (user_id) REFERENCES sys_user(user_id),
    CONSTRAINT fk_claim_order FOREIGN KEY (order_id) REFERENCES template_order(order_id)
) COMMENT='活动领取表：防止重复领取与超发';

CREATE INDEX idx_template_filter ON prompt_template(shelf_status, avg_score, price);
CREATE INDEX idx_template_hot_sort ON prompt_template(shelf_status, use_count, favorite_count, avg_score);
CREATE INDEX idx_template_creator ON prompt_template(creator_user_id, created_at);
CREATE INDEX idx_template_current_version ON prompt_template(current_version_id);
CREATE INDEX idx_version_query ON prompt_template_version(template_id, created_at);
CREATE INDEX idx_tag_name ON tag(tag_name);
CREATE INDEX idx_rel_tag_template ON template_tag_rel(tag_id, template_id);
CREATE INDEX idx_rel_template_tag ON template_tag_rel(template_id, tag_id);
CREATE INDEX idx_order_query ON template_order(user_id, pay_status, created_at);
CREATE INDEX idx_use_query ON template_use_log(template_id, used_at);
CREATE INDEX idx_use_user_time ON template_use_log(user_id, used_at);
CREATE INDEX idx_review_query ON template_review(template_id, score, created_at);
CREATE INDEX idx_campaign_time ON free_campaign(start_time, end_time, campaign_status);
