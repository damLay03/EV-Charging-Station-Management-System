# 🚀 Quick Start - Wallet Migration

## Your Situation: Got "auto column" error

**✅ Solution: Run the RESUME script**

```bash
# In your database client (DataGrip, MySQL Workbench, etc.)
source docs/sql/2025-11-21_resume_wallet_migration.sql
```

This script will:
1. Remove AUTO_INCREMENT from `id` column (fixes the error!)
2. Drop old PRIMARY KEY
3. Drop old `id` column
4. Set `wallet_id` as new PRIMARY KEY
5. Add `wallet_id` to `drivers` table
6. Link all drivers to their wallets
7. Create foreign key constraint

---

## ⚠️ If Something Goes Wrong

**Rollback to original:**
```bash
source docs/sql/2025-11-21_rollback_wallet_migration.sql
```

---

## 📊 Verify After Migration

```sql
-- Check wallets structure
DESCRIBE wallets;
-- Should show: wallet_id (VARCHAR 36) as PRIMARY KEY

-- Check drivers linked
SELECT COUNT(*) as total, 
       SUM(CASE WHEN wallet_id IS NOT NULL THEN 1 ELSE 0 END) as linked
FROM drivers;
-- All should be linked!
```

---

## 📁 Files Guide

| When | Use This File |
|------|---------------|
| 🟢 **NOW** (you have wallet_id but got error) | `2025-11-21_resume_wallet_migration.sql` |
| 🔴 Need to undo | `2025-11-21_rollback_wallet_migration.sql` |
| 🔵 Fresh start (no wallet_id yet) | `2025-11-21_migrate_wallet_id_to_uuid.sql` |
| 🟡 After migration done | `2025-11-21_create_wallets_for_existing_users.sql` |

---

## ✅ Done! What's Next?

1. Restart your Spring Boot app
2. Test creating new user → should have wallet automatically
3. Test Google OAuth login → should have wallet automatically
4. Check logs for any errors

**Backup table `wallets_backup` is kept for safety. You can drop it later.**

