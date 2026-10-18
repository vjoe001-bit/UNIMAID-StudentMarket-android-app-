-- ====================================================================
-- UNIMAID StudentMarket: Reviews, Ratings & Seller Reputation Migration
-- ====================================================================

-- 1. Ensure public.reviews table exists with full schema & constraints
CREATE TABLE IF NOT EXISTS public.reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES public.orders(id) ON DELETE CASCADE,
    listing_id UUID REFERENCES public.listings(id) ON DELETE SET NULL,
    reviewer_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    reviewed_user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    CONSTRAINT uq_review_per_order UNIQUE (order_id)
);

-- 2. Indexes for fast lookups on seller profile, listing details, and orders
CREATE INDEX IF NOT EXISTS idx_reviews_reviewed_user ON public.reviews(reviewed_user_id);
CREATE INDEX IF NOT EXISTS idx_reviews_listing ON public.reviews(listing_id);
CREATE INDEX IF NOT EXISTS idx_reviews_reviewer ON public.reviews(reviewer_id);
CREATE INDEX IF NOT EXISTS idx_reviews_order ON public.reviews(order_id);

-- 3. Enable RLS on reviews
ALTER TABLE public.reviews ENABLE ROW LEVEL SECURITY;

-- 4. RLS: Everyone can read reviews
DROP POLICY IF EXISTS "Public can view all reviews" ON public.reviews;
CREATE POLICY "Public can view all reviews"
    ON public.reviews FOR SELECT
    USING (true);

-- 5. RLS: Authenticated buyer can insert review only for completed order they participated in as buyer
DROP POLICY IF EXISTS "Buyers can review completed orders" ON public.reviews;
CREATE POLICY "Buyers can review completed orders"
    ON public.reviews FOR INSERT
    WITH CHECK (
        auth.uid() = reviewer_id
        AND reviewer_id != reviewed_user_id
        AND EXISTS (
            SELECT 1 FROM public.orders o
            WHERE o.id = order_id
              AND o.buyer_id = auth.uid()
              AND o.seller_id = reviewed_user_id
              AND o.status = 'COMPLETED'
        )
    );

-- 6. RLS: Reviewers can update their own review comment/rating
DROP POLICY IF EXISTS "Reviewers can update own reviews" ON public.reviews;
CREATE POLICY "Reviewers can update own reviews"
    ON public.reviews FOR UPDATE
    USING (auth.uid() = reviewer_id)
    WITH CHECK (
        auth.uid() = reviewer_id
        AND reviewer_id != reviewed_user_id
    );

-- 7. Add rating and review_count cached fields to profiles if missing
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_column_name WHERE table_schema = 'public' AND table_name = 'profiles' AND column_name = 'rating') THEN
        -- Safely alter
        BEGIN
            ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS rating NUMERIC(3, 2) DEFAULT 0.00;
        EXCEPTION WHEN duplicate_column THEN
            -- ignore
        END;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_column_name WHERE table_schema = 'public' AND table_name = 'profiles' AND column_name = 'review_count') THEN
        BEGIN
            ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS review_count INTEGER DEFAULT 0;
        EXCEPTION WHEN duplicate_column THEN
            -- ignore
        END;
    END IF;
END $$;
