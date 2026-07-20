-- Historical migration marker: user identity/admin fields are included in V1 for fresh installs.
-- Existing databases should use docs/sql/user_identity_patch.sql and docs/sql/user_admin_patch.sql.
SELECT 1;
