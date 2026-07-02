-- =========================================================================
-- 社内つぶやきボード V3: 投稿者アバター色
-- Oracle XE 21c および H2(MODE=Oracle) の双方で動く DDL
-- =========================================================================

ALTER TABLE posts ADD avatar_color VARCHAR2(20 CHAR);

ALTER TABLE posts ADD CONSTRAINT posts_avatar_color_ck
    CHECK (avatar_color IS NULL OR avatar_color IN ('blue', 'green', 'pink', 'yellow'));
