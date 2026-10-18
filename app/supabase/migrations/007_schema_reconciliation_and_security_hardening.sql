-- ==============================================================================
-- UNIMAID StudentMarket — Migration 007: Complete Schema Reconciliation,
-- Atomic Order Placement, and Security Hardening
-- ==============================================================================

-- 1. Ensure public.orders has all required fields with proper constraints
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'orders' AND column_name = 'listing_id') THEN
        ALTER TABLE public.orders ADD COLUMN listing_id UUID REFERENCES public.listings(id) ON DELETE SET NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'orders' AND column_name = 'quantity') THEN
        ALTER TABLE public.orders ADD COLUMN quantity INTEGER NOT NULL DEFAULT 1 CHECK (quantity > 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'orders' AND column_name = 'total_amount') THEN
        ALTER TABLE public.orders ADD COLUMN total_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00 CHECK (total_amount >= 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'orders' AND column_name = 'meeting_location') THEN
        ALTER TABLE public.orders ADD COLUMN meeting_location TEXT DEFAULT 'UNIMAID Main Campus';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'orders' AND column_name = 'meeting_time') THEN
        ALTER TABLE public.orders ADD COLUMN meeting_time TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'orders' AND column_name = 'buyer_note') THEN
        ALTER TABLE public.orders ADD COLUMN buyer_note TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'orders' AND column_name = 'seller_note') THEN
        ALTER TABLE public.orders ADD COLUMN seller_note TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'orders' AND column_name = 'payment_status') THEN
        ALTER TABLE public.orders ADD COLUMN payment_status TEXT DEFAULT 'PHYSICAL_PENDING';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'orders' AND column_name = 'completed_at') THEN
        ALTER TABLE public.orders ADD COLUMN completed_at TIMESTAMPTZ;
    END IF;
END $$;

-- 2. Ensure public.order_status_history table exists
CREATE TABLE IF NOT EXISTS public.order_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES public.orders(id) ON DELETE CASCADE,
    new_status TEXT NOT NULL,
    changed_by UUID NOT NULL REFERENCES public.profiles(id),
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);
CREATE INDEX IF NOT EXISTS idx_order_status_history_order ON public.order_status_history(order_id);
ALTER TABLE public.order_status_history ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Participants can view order status history" ON public.order_status_history;
CREATE POLICY "Participants can view order status history"
    ON public.order_status_history FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.orders o
            WHERE o.id = order_id
              AND (o.buyer_id = auth.uid() OR o.seller_id = auth.uid())
        )
    );

DROP POLICY IF EXISTS "Participants can insert order status history" ON public.order_status_history;
CREATE POLICY "Participants can insert order status history"
    ON public.order_status_history FOR INSERT
    WITH CHECK (
        auth.uid() = changed_by
        AND EXISTS (
            SELECT 1 FROM public.orders o
            WHERE o.id = order_id
              AND (o.buyer_id = auth.uid() OR o.seller_id = auth.uid())
        )
    );

-- 3. Ensure public.notifications has data_payload and body
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'notifications' AND column_name = 'data_payload') THEN
        ALTER TABLE public.notifications ADD COLUMN data_payload JSONB DEFAULT '{}'::jsonb;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'notifications' AND column_name = 'body') THEN
        ALTER TABLE public.notifications ADD COLUMN body TEXT;
    END IF;
END $$;

-- 4. Ensure public.profiles has email, phone_number, rating, and review_count
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'profiles' AND column_name = 'email') THEN
        ALTER TABLE public.profiles ADD COLUMN email TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'profiles' AND column_name = 'phone_number') THEN
        ALTER TABLE public.profiles ADD COLUMN phone_number TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'profiles' AND column_name = 'rating') THEN
        ALTER TABLE public.profiles ADD COLUMN rating NUMERIC(3, 2) DEFAULT 0.00;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'profiles' AND column_name = 'review_count') THEN
        ALTER TABLE public.profiles ADD COLUMN review_count INTEGER DEFAULT 0;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'profiles' AND column_name = 'is_suspended') THEN
        ALTER TABLE public.profiles ADD COLUMN is_suspended BOOLEAN DEFAULT FALSE;
    END IF;
END $$;

-- 5. Safe Administrator Setup for jd267904@gmail.com
-- Only assigns SUPER_ADMIN if the user genuinely exists in auth.users
DO $$
DECLARE
    v_admin_id UUID;
BEGIN
    SELECT id INTO v_admin_id FROM auth.users WHERE email = 'jd267904@gmail.com' LIMIT 1;
    IF v_admin_id IS NOT NULL THEN
        -- Ensure profile exists
        INSERT INTO public.profiles (id, full_name, is_verified)
        VALUES (v_admin_id, 'Campus Administrator', TRUE)
        ON CONFLICT (id) DO UPDATE SET is_verified = TRUE;

        -- Grant SUPER_ADMIN
        INSERT INTO public.admin_roles (user_id, role)
        VALUES (v_admin_id, 'SUPER_ADMIN')
        ON CONFLICT (user_id, role) DO NOTHING;
    END IF;
END $$;

-- 6. Server-side Atomic Order Placement Function
-- Locks listing row, verifies availability & stock, derives buyer from auth.uid(),
-- checks no self-purchase, derives authoritative price, creates order, and returns it.
CREATE OR REPLACE FUNCTION public.place_order_atomic(
    p_listing_id UUID,
    p_quantity INTEGER DEFAULT 1,
    p_meeting_location TEXT DEFAULT 'UNIMAID Main Campus',
    p_buyer_note TEXT DEFAULT NULL
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth
AS $$
DECLARE
    v_buyer_id UUID := auth.uid();
    v_listing RECORD;
    v_order_id UUID;
    v_total_amount NUMERIC(12, 2);
    v_result JSONB;
BEGIN
    -- 1. Validate authenticated caller
    IF v_buyer_id IS NULL THEN
        RAISE EXCEPTION 'Authentication required to place a purchase request.';
    END IF;

    -- 2. Validate input parameters
    IF p_quantity <= 0 THEN
        RAISE EXCEPTION 'Quantity must be at least 1.';
    END IF;

    -- 3. Lock listing row for update to prevent concurrent inventory race condition
    SELECT * INTO v_listing
    FROM public.listings
    WHERE id = p_listing_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'The requested campus item was not found.';
    END IF;

    -- 4. Validate listing availability
    IF v_listing.status <> 'ACTIVE' THEN
        RAISE EXCEPTION 'This item is no longer available.';
    END IF;

    -- 5. Prevent self-purchase
    IF v_listing.seller_id = v_buyer_id THEN
        RAISE EXCEPTION 'You cannot submit a purchase request for your own listing.';
    END IF;

    -- 6. Validate available stock
    IF v_listing.quantity < p_quantity THEN
        RAISE EXCEPTION 'Only % items available in stock.', v_listing.quantity;
    END IF;

    -- 7. Calculate authoritative total amount from server listing price
    v_total_amount := v_listing.price * p_quantity;
    v_order_id := gen_random_uuid();

    -- 8. Insert order
    INSERT INTO public.orders (
        id,
        buyer_id,
        seller_id,
        listing_id,
        quantity,
        total_amount,
        status,
        meeting_location,
        buyer_note,
        created_at,
        updated_at
    )
    VALUES (
        v_order_id,
        v_buyer_id,
        v_listing.seller_id,
        p_listing_id,
        p_quantity,
        v_total_amount,
        'PENDING',
        COALESCE(p_meeting_location, 'UNIMAID Main Campus'),
        p_buyer_note,
        now(),
        now()
    );

    -- 9. Record initial status history
    INSERT INTO public.order_status_history (
        order_id,
        new_status,
        changed_by,
        note
    )
    VALUES (
        v_order_id,
        'PENDING',
        v_buyer_id,
        COALESCE(p_buyer_note, 'Purchase request submitted for physical campus handover.')
    );

    -- 10. Build and return created order JSON
    SELECT to_jsonb(o.*) INTO v_result
    FROM public.orders o
    WHERE o.id = v_order_id;

    RETURN v_result;
END;
$$;

-- Grant execution permission on atomic order placement to authenticated students
GRANT EXECUTE ON FUNCTION public.place_order_atomic(UUID, INTEGER, TEXT, TEXT) TO authenticated;

-- 7. Verification and Audit Comments
COMMENT ON FUNCTION public.place_order_atomic IS 'Atomically places an order with row-level locking on listing inventory, authoritative pricing, and self-purchase protection.';
