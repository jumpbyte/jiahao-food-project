-- ============================================================
-- 区域管理后台 - 数据库初始化脚本 v1.0
-- 生成时间: 2026-04-29
-- 数据库: MySQL 8.0+
-- 字符集: utf8mb4
-- ============================================================

-- 创建数据库（如不存在）
CREATE DATABASE IF NOT EXISTS `jiahao_food_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `jiahao_food_db`;

-- ============================================================
-- 1. 行政区表（area）
-- ============================================================
DROP TABLE IF EXISTS `area`;
CREATE TABLE `area` (
  `id` int(10) unsigned NOT NULL COMMENT '主键 ID',
  `pid` int(10) unsigned NOT NULL DEFAULT 0 COMMENT '父级 ID',
  `level` int(10) unsigned NOT NULL DEFAULT 0 COMMENT '层级 1-省 2-市 3-区县 4-乡镇街道',
  `name` varchar(240) NOT NULL DEFAULT '' COMMENT '名称',
  `short_name` varchar(240) NOT NULL DEFAULT '' COMMENT '简称',
  `full_name` varchar(240) NOT NULL DEFAULT '' COMMENT '全路径名称',
  `pin_yin` varchar(240) NOT NULL DEFAULT '' COMMENT '拼音',
  `adcode` varchar(10) NOT NULL DEFAULT '' COMMENT '行政区划编码',
  `zip_code` varchar(10) NOT NULL DEFAULT '' COMMENT '邮编',
  `lng` decimal(10,7) unsigned NOT NULL DEFAULT 0.0000000 COMMENT '经度',
  `lat` decimal(10,7) unsigned NOT NULL DEFAULT 0.0000000 COMMENT '纬度',
  `path` varchar(500) NOT NULL DEFAULT '' COMMENT '路径，如 1/2/3',
  `version` varchar(64) NOT NULL DEFAULT '' COMMENT '版本号',
  `state` tinyint(1) NOT NULL DEFAULT 0 COMMENT '0-禁用 1-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_pid` (`pid`),
  KEY `idx_path` (`path`(255)),
  KEY `idx_adcode` (`adcode`),
  KEY `idx_level` (`level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行政区表';

-- ============================================================
-- 2. 组织架构表（organization）
-- ============================================================
DROP TABLE IF EXISTS `organization`;
CREATE TABLE `organization` (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `name` varchar(50) NOT NULL DEFAULT '' COMMENT '组织名称',
  `type` tinyint(2) NOT NULL DEFAULT 0 COMMENT '组织类型 1-大区 2-办事处 3-片区',
  `parent_id` int(10) NOT NULL DEFAULT 0 COMMENT '父级组织 ID',
  `level` int(5) NOT NULL DEFAULT 0 COMMENT '层级',
  `path` varchar(500) NOT NULL DEFAULT '' COMMENT '组织路径，如 1/10/100',
  `sort_index` int(10) NOT NULL DEFAULT 0 COMMENT '排序',
  `state` tinyint(1) NOT NULL DEFAULT 0 COMMENT '0-失效 1-正常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_type` (`type`),
  KEY `idx_path` (`path`(255)),
  KEY `idx_parent_type` (`parent_id`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织架构表（大区/办事处/片区统一）';

-- ============================================================
-- 3. 行政区与组织映射表（area_relation）
-- ============================================================
DROP TABLE IF EXISTS `area_relation`;
CREATE TABLE `area_relation` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `area_id` int(11) NOT NULL DEFAULT 0 COMMENT '行政区 ID',
  `org_id` int(11) NOT NULL DEFAULT 0 COMMENT '组织 ID',
  `del_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT '删除标志 0-正常 1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_area_org` (`area_id`, `org_id`),
  KEY `idx_area_id` (`area_id`),
  KEY `idx_org_id` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行政区与组织映射关系表';

-- ============================================================
-- 4. 系统用户表（sys_user）
-- ============================================================
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `username` varchar(50) NOT NULL DEFAULT '' COMMENT '用户名',
  `password` varchar(128) NOT NULL DEFAULT '' COMMENT '密码（BCrypt 加密）',
  `real_name` varchar(50) NOT NULL DEFAULT '' COMMENT '真实姓名',
  `phone` varchar(20) NOT NULL DEFAULT '' COMMENT '手机号',
  `email` varchar(100) NOT NULL DEFAULT '' COMMENT '邮箱',
  `role` varchar(20) NOT NULL DEFAULT 'operator' COMMENT '角色 admin-管理员 operator-运营人员 viewer-只读人员',
  `state` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_phone` (`phone`),
  KEY `idx_state` (`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- ============================================================
-- 5. API Key 存储表（sys_api_key）
-- ============================================================
DROP TABLE IF EXISTS `sys_api_key`;
CREATE TABLE `sys_api_key` (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `app_key` varchar(64) NOT NULL DEFAULT '' COMMENT '调用方标识（唯一）',
  `app_secret` varchar(128) NOT NULL DEFAULT '' COMMENT '调用方密钥（加密存储）',
  `app_name` varchar(100) NOT NULL DEFAULT '' COMMENT '应用名称',
  `state` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
  `remark` varchar(500) NOT NULL DEFAULT '' COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_key` (`app_key`),
  KEY `idx_state` (`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API Key 存储表';

-- ============================================================
-- 初始数据
-- ============================================================

-- 默认管理员账号 (密码: admin123, BCrypt 加密)
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `role`, `state`)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 'admin', 1);

-- 示例 API Key (app_key: demo, app_secret: demo_secret_123)
INSERT INTO `sys_api_key` (`app_key`, `app_secret`, `app_name`, `state`, `remark`)
VALUES ('demo', 'demo_secret_123', '测试应用', 1, '示例 API Key，生产环境请更换');

-- 示例组织架构
INSERT INTO `organization` (`id`, `name`, `type`, `parent_id`, `level`, `path`, `sort_index`, `state`)
VALUES
  (1, '华南大区', 1, 0, 1, '1', 1, 1),
  (2, '深圳办事处', 2, 1, 2, '1/2', 1, 1),
  (3, '南山片区', 3, 2, 3, '1/2/3', 1, 1);
