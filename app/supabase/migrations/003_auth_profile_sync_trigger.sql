-- ==============================================================================
-- UNIMAID StudentMarket — Migration 003: Auth Profile Synchronization Trigger
-- Automatically creates a public.profiles row whenever a user is added to auth.users.
-- Idempotent, safe against missing metadata, and backfills any unprofiled users.
-- ==============================================================================

-- 1. Ensure required columns exist on public.profiles without modifying existing data
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS is_suspended BOOLEAN DEFAULT FALSE;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS is_verified BOOLEAN DEFAULT FALSE;

-- 2. Create or replace secure trigger function
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
BEGIN
    INSERT INTO public.profiles (
        id,
        email,
        full_name,
        avatar_url,
        is_verified,
        is_suspended,
        matric_number,
        department,
        faculty,
        level,
        bio,
        created_at,
        updated_at
    )
    VALUES (
        NEW.id,
        NEW.email,
        COALESCE(
            NULLIF(TRIM(NEW.raw_user_meta_data->>'full_name'), ''),
            NULLIF(TRIM(NEW.raw_user_meta_data->>'name'), ''),
            NULLIF(SPLIT_PART(NEW.email, '@', 1), ''),
            'UNIMAID Student'
        ),
        NULLIF(TRIM(NEW.raw_user_meta_data->>'avatar_url'), ''),
        FALSE,
        FALSE,
        NULLIF(TRIM(NEW.raw_user_meta_data->>'matric_number'), ''),
        NULLIF(TRIM(NEW.raw_user_meta_data->>'department'), ''),
        NULLIF(TRIM(NEW.raw_user_meta_data->>'faculty'), ''),
        COALESCE(NULLIF(TRIM(NEW.raw_user_meta_data->>'level'), ''), '100L'),
        NULLIF(TRIM(NEW.raw_user_meta_data->>'bio'), ''),
        NOW(),
        NOW()
    )
    ON CONFLICT (id) DO NOTHING;

    RETURN NEW;
EXCEPTION
    WHEN OTHERS THEN
        -- Prevent unhandled trigger failure from blocking auth.users insertion
        RAISE WARNING 'handle_new_user trigger error for user %: %', NEW.id, SQLERRM;
        RETURN NEW;
END;
$$;

-- 3. Bind trigger to auth.users (idempotent: drops if already exists)
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;

CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_new_user();

-- 4. Backfill any existing users in auth.users who do not yet have a profile
INSERT INTO public.profiles (
    id,
    email,
    full_name,
    avatar_url,
    is_verified,
    is_suspended,
    matric_number,
    department,
    faculty,
    level,
    bio,
    created_at,
    updated_at
)
SELECT 
    u.id,
    u.email,
    COALESCE(
        NULLIF(TRIM(u.raw_user_meta_data->>'full_name'), ''),
        NULLIF(TRIM(u.raw_user_meta_data->>'name'), ''),
        NULLIF(SPLIT_PART(u.email, '@', 1), ''),
        'UNIMAID Student'
    ),
    NULLIF(TRIM(u.raw_user_meta_data->>'avatar_url'), ''),
    FALSE,
    FALSE,
    NULLIF(TRIM(u.raw_user_meta_data->>'matric_number'), ''),
    NULLIF(TRIM(u.raw_user_meta_data->>'department'), ''),
    NULLIF(TRIM(u.raw_user_meta_data->>'faculty'), ''),
    COALESCE(NULLIF(TRIM(u.raw_user_meta_data->>'level'), ''), '100L'),
    NULLIF(TRIM(u.raw_user_meta_data->>'bio'), ''),
    COALESCE(u.created_at, NOW()),
    NOW()
FROM auth.users u
LEFT JOIN public.profiles p ON p.id = u.id
WHERE p.id IS NULL
ON CONFLICT (id) DO NOTHING;

-- 5. Review and enforce RLS policies on public.profiles
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

-- Drop legacy or overly permissive policies
DROP POLICY IF EXISTS "Public profiles are viewable by all authenticated users" ON public.profiles;
DROP POLICY IF EXISTS "Authenticated users can view profiles" ON public.profiles;
DROP POLICY IF EXISTS "Users can insert their own profile" ON public.profiles;
DROP POLICY IF EXISTS "Users can update their own profile" ON public.profiles;

-- SELECT policy: Authenticated users can view profiles according to app model
CREATE POLICY "Authenticated users can view profiles"
ON public.profiles FOR SELECT
TO authenticated
USING (auth.role() = 'authenticated');

-- INSERT policy: Authenticated user may insert ONLY their own profile
CREATE POLICY "Users can insert their own profile"
ON public.profiles FOR INSERT
TO authenticated
WITH CHECK (auth.uid() = id);

-- UPDATE policy: Authenticated user may update ONLY their own profile
CREATE POLICY "Users can update their own profile"
ON public.profiles FOR UPDATE
TO authenticated
USING (auth.uid() = id)
WITH CHECK (auth.uid() = id);
