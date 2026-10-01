USE restart;

CREATE TABLE IF NOT EXISTS `sys_user`
(
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`   VARCHAR(50)  NOT NULL COMMENT '用户名',
    `password`   VARCHAR(255) NOT NULL COMMENT '密码',
    `nickname`   VARCHAR(50)           DEFAULT NULL COMMENT '昵称',
    `avatar`     VARCHAR(500)          DEFAULT NULL COMMENT '头像',
    `status`     TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1正常',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '系统用户表';

CREATE TABLE IF NOT EXISTS `moments`
(
    `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '朋友圈ID',
    `user_id`    BIGINT   NOT NULL COMMENT '发布用户ID',
    `content`    VARCHAR(2000)     DEFAULT NULL COMMENT '朋友圈文字内容',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_moments_user`
        FOREIGN KEY (`user_id`)
            REFERENCES `sys_user` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT ='朋友圈';

CREATE TABLE IF NOT EXISTS `moment_images`
(
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '图片ID',
    `moment_id`  BIGINT       NOT NULL COMMENT '朋友圈ID',
    `image_url`  VARCHAR(500) NOT NULL COMMENT '图片地址',
    `sort`       INT          NOT NULL DEFAULT 0 COMMENT '图片排序',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_moment_id` (`moment_id`),
    CONSTRAINT `fk_moment_images_moment`
        FOREIGN KEY (`moment_id`)
            REFERENCES `moments` (`id`)
            ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT ='朋友圈图片';
