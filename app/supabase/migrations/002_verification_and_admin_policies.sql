-- ==============================================================================
-- UNIMAID StudentMarket — Migration 002: Verification & Admin RLS Enhancements
-- Adds administrative review policies for verification requests
-- ==============================================================================

-- 1. Allow Campus Administrators to review and update verification requests
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'verification_requests' 
        AND policyname = 'Admins can view all verification requests'
    ) THEN
        CREATE POLICY "Admins can view all verification requests"
        ON public.verification_requests FOR SELECT
        USING (
            EXISTS (
                SELECT 1 FROM public.admin_roles
                WHERE user_id = auth.uid()
            )
        );
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'verification_requests' 
        AND policyname = 'Admins can update verification requests'
    ) THEN
        CREATE POLICY "Admins can update verification requests"
        ON public.verification_requests FOR UPDATE
        USING (
            EXISTS (
                SELECT 1 FROM public.admin_roles
                WHERE user_id = auth.uid()
            )
        );
    END IF;
END $$;
