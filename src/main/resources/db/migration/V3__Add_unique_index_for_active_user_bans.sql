-- Индекс для обеспечения уникальности id-шников для забаненных пользователей
-- Другими словами, для пользователя может существовать один и только один активный бан
CREATE UNIQUE INDEX idx_user_ban_active_unique
ON user_bans (banned_user_id)
WHERE unbanned_at IS NULL;