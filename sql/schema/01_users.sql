-- 01_users.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `users` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '사용자 ID',
  `username` varchar(255) NOT NULL COMMENT '로그인 아이디',
  `password` varchar(255) NOT NULL COMMENT 'BCrypt 해시 비밀번호',
  `role` varchar(50) DEFAULT NULL COMMENT '권한 (기본 USER)',
  `refresh_token` varchar(512) DEFAULT NULL COMMENT 'JWT refresh token',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자'
