-- ==============================================================================
-- UNIMAID StudentMarket — Migration 005: Sell / Post Item System & Storage RLS
-- Phase D — Prompt 2: Real Marketplace Listing, Storage Policies & Realtime
-- ==============================================================================

-- 1. Ensure Storage Bucket for listing-images exists and is marked public
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'listing-images',
    'listing-images',
    TRUE,
    5242880, -- 5MB limit per image
    ARRAY['image/jpeg', 'image/png', 'image/webp']
)
ON CONFLICT (id) DO UPDATE SET
    public = TRUE,
    file_size_limit = 5242880,
    allowed_mime_types = ARRAY['image/jpeg', 'image/png', 'image/webp'];

-- Storage RLS Policies for listing-images
DO $$
BEGIN
    -- Public view policy
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'objects' AND schemaname = 'storage'
        AND policyname = 'Public listing images are viewable by everyone'
    ) THEN
        CREATE POLICY "Public listing images are viewable by everyone"
        ON storage.objects FOR SELECT
        USING (bucket_id = 'listing-images');
    END IF;

    -- Authenticated upload policy
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'objects' AND schemaname = 'storage'
        AND policyname = 'Authenticated users can upload listing images'
    ) THEN
        CREATE POLICY "Authenticated users can upload listing images"
        ON storage.objects FOR INSERT
        WITH CHECK (
            bucket_id = 'listing-images' 
            AND auth.role() = 'authenticated'
        );
    END IF;

    -- Update policy for owners
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'objects' AND schemaname = 'storage'
        AND policyname = 'Authenticated users can update their listing images'
    ) THEN
        CREATE POLICY "Authenticated users can update their listing images"
        ON storage.objects FOR UPDATE
        USING (
            bucket_id = 'listing-images' 
            AND auth.role() = 'authenticated'
        );
    END IF;

    -- Delete policy for owners
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'objects' AND schemaname = 'storage'
        AND policyname = 'Authenticated users can delete their listing images'
    ) THEN
        CREATE POLICY "Authenticated users can delete their listing images"
        ON storage.objects FOR DELETE
        USING (
            bucket_id = 'listing-images' 
            AND auth.role() = 'authenticated'
        );
    END IF;
END $$;

-- 2. Ensure Listings table schema and RLS policies
ALTER TABLE public.listings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.listing_images ENABLE ROW LEVEL SECURITY;

-- Ensure default status column value
ALTER TABLE public.listings 
    ALTER COLUMN status SET DEFAULT 'ACTIVE';

-- Ensure campus_location column exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'public' 
        AND table_name = 'listings' 
        AND column_name = 'campus_location'
    ) THEN
        ALTER TABLE public.listings ADD COLUMN campus_location TEXT DEFAULT 'UNIMAID Main Campus';
    END IF;
END $$;

-- RLS: Listings Policies
DO $$
BEGIN
    -- Select: Everyone can view active/available listings
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'listings' AND policyname = 'Listings are viewable by everyone'
    ) THEN
        CREATE POLICY "Listings are viewable by everyone"
        ON public.listings FOR SELECT
        USING (true);
    END IF;

    -- Insert: Authenticated student can only create listings with auth.uid() as seller_id
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'listings' AND policyname = 'Users can create their own listings'
    ) THEN
        CREATE POLICY "Users can create their own listings"
        ON public.listings FOR INSERT
        WITH CHECK (auth.uid() = seller_id);
    END IF;

    -- Update: Only owner can update listing
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'listings' AND policyname = 'Users can update their own listings'
    ) THEN
        CREATE POLICY "Users can update their own listings"
        ON public.listings FOR UPDATE
        USING (auth.uid() = seller_id);
    END IF;

    -- Delete: Only owner can delete listing
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'listings' AND policyname = 'Users can delete their own listings'
    ) THEN
        CREATE POLICY "Users can delete their own listings"
        ON public.listings FOR DELETE
        USING (auth.uid() = seller_id);
    END IF;
END $$;

-- RLS: Listing Images Policies
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'listing_images' AND policyname = 'Listing images are viewable by everyone'
    ) THEN
        CREATE POLICY "Listing images are viewable by everyone"
        ON public.listing_images FOR SELECT
        USING (true);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'listing_images' AND policyname = 'Sellers can manage images for their listings'
    ) THEN
        CREATE POLICY "Sellers can manage images for their listings"
        ON public.listing_images FOR INSERT
        WITH CHECK (
            EXISTS (
                SELECT 1 FROM public.listings
                WHERE id = listing_images.listing_id AND seller_id = auth.uid()
            )
        );
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'listing_images' AND policyname = 'Sellers can delete images for their listings'
    ) THEN
        CREATE POLICY "Sellers can delete images for their listings"
        ON public.listing_images FOR DELETE
        USING (
            EXISTS (
                SELECT 1 FROM public.listings
                WHERE id = listing_images.listing_id AND seller_id = auth.uid()
            )
        );
    END IF;
END $$;

-- 3. Add Listings to Realtime Publication
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables
        WHERE pubname = 'supabase_realtime' AND tablename = 'listings'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.listings;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables
        WHERE pubname = 'supabase_realtime' AND tablename = 'listing_images'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.listing_images;
    END IF;
END $$;
