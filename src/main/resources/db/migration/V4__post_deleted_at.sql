-- =========================================================================
-- 社内つぶやきボード V4: 投稿の論理削除
-- Oracle XE 21c および H2(MODE=Oracle) の双方で動く DDL
-- =========================================================================

ALTER TABLE posts ADD deleted_at TIMESTAMP(6);

CREATE INDEX posts_deleted_at_created_at_idx ON posts (deleted_at, created_at);
