-- ==============================================================================
-- UNIMAID StudentMarket — Migration 004: Grant Administrator Privileges
-- Designated Admin User: jd267904@gmail.com
-- UID:                   fdb54fd6-7cf3-4a64-9b86-92cbe55a50bc
-- ==============================================================================

-- 1. Ensure profile exists for the administrator
INSERT INTO public.profiles (
    id,
    email,
    full_name,
    is_verified,
    is_suspended,
    account_status,
    created_at,
    updated_at
)
VALUES (
    'fdb54fd6-7cf3-4a64-9b86-92cbe55a50bc',
    'jd267904@gmail.com',
    'Campus Administrator',
    TRUE,
    FALSE,
    'ACTIVE',
    NOW(),
    NOW()
)
ON CONFLICT (id) DO UPDATE SET
    email = EXCLUDED.email,
    is_verified = TRUE,
    is_suspended = FALSE,
    updated_at = NOW();

-- 2. Grant SUPER_ADMIN role in public.admin_roles
INSERT INTO public.admin_roles (
    user_id,
    role,
    granted_at
)
VALUES (
    'fdb54fd6-7cf3-4a64-9b86-92cbe55a50bc',
    'SUPER_ADMIN',
    NOW()
)
ON CONFLICT (user_id, role) DO NOTHING;

-- 3. Also grant standard ADMIN role for compatibility
INSERT INTO public.admin_roles (
    user_id,
    role,
    granted_at
)
VALUES (
    'fdb54fd6-7cf3-4a64-9b86-92cbe55a50bc',
    'ADMIN',
    NOW()
)
ON CONFLICT (user_id, role) DO NOTHING;
