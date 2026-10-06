-- ==============================================================================
-- UNIMAID StudentMarket — Supabase Database Foundation Migration
-- Target Users: University of Maiduguri (UNIMAID) students
-- Business Policy: Physical in-person campus handover, NO online payment gateway
-- ==============================================================================

-- Enable UUID extension if not already enabled
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ==============================================================================
-- 1. PROFILES TABLE
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT UNIQUE,
    full_name TEXT NOT NULL,
    matric_number TEXT,
    department TEXT,
    faculty TEXT,
    level TEXT DEFAULT '100L', -- 100L, 200L, 300L, 400L, 500L, Postgraduate
    phone_number TEXT,
    bio TEXT,
    avatar_url TEXT,
    is_verified BOOLEAN DEFAULT FALSE,
    student_id_card_url TEXT,
    account_status TEXT DEFAULT 'ACTIVE', -- ACTIVE, SUSPENDED, FLAGGED
    can_sell BOOLEAN DEFAULT TRUE,
    can_buy BOOLEAN DEFAULT TRUE,
    rating NUMERIC(3, 2) DEFAULT 5.00,
    review_count INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ==============================================================================
-- 2. CATEGORIES TABLE
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL UNIQUE,
    slug TEXT NOT NULL UNIQUE,
    description TEXT,
    icon_name TEXT,
    type TEXT DEFAULT 'PRODUCT', -- PRODUCT, SERVICE
    display_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Seed Essential UNIMAID Categories
INSERT INTO public.categories (name, slug, description, icon_name, type, display_order)
VALUES
    ('Textbooks & Handouts', 'textbooks', 'Course materials, past questions, and academic books', 'MenuBook', 'PRODUCT', 1),
    ('Laptops & Computers', 'laptops', 'Laptops, chargers, keyboards, and PC accessories', 'Computer', 'PRODUCT', 2),
    ('Phones & Tablets', 'phones', 'Smartphones, power banks, earphones, and chargers', 'Smartphone', 'PRODUCT', 3),
    ('Hostel & Living Essentials', 'hostel-essentials', 'Mattresses, reading lamps, buckets, fans, extension cords', 'Hotel', 'PRODUCT', 4),
    ('Electronics & Appliances', 'electronics', 'Refrigerators, electric kettles, hot plates, blenders', 'ElectricalServices', 'PRODUCT', 5),
    ('Fashion & Campus Wear', 'fashion', 'Clothing, shoes, bags, wristwatches, and glasses', 'Checkroom', 'PRODUCT', 6),
    ('Food & Provisions', 'food-provisions', 'Packaged provisions, snacks, and student groceries', 'Restaurant', 'PRODUCT', 7),
    ('Beauty & Personal Care', 'beauty-personal-care', 'Skincare, hair care, perfumes, grooming essentials', 'Spa', 'PRODUCT', 8),
    ('Campus Student Services', 'services', 'Typing, graphic design, tailoring, tutorials, device repair', 'Handyman', 'SERVICE', 9),
    ('Sports & Fitness', 'sports', 'Football boots, jerseys, gym gear, bicycles', 'SportsSoccer', 'PRODUCT', 10),
    ('Other Campus Items', 'other', 'Miscellaneous university items', 'Category', 'PRODUCT', 11)
ON CONFLICT (slug) DO NOTHING;

-- ==============================================================================
-- 3. PRODUCTS / LISTINGS TABLE
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.listings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    seller_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    category_id UUID REFERENCES public.categories(id) ON DELETE SET NULL,
    title TEXT NOT NULL,
    description TEXT,
    price NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
    condition TEXT NOT NULL DEFAULT 'GOOD', -- NEW, LIKE_NEW, GOOD, FAIR, USED
    quantity INTEGER NOT NULL DEFAULT 1 CHECK (quantity >= 0),
    location_campus TEXT NOT NULL DEFAULT 'UNIMAID Main Campus', -- e.g. Senate Building, Complex, Faculty Quarters, Library
    is_available BOOLEAN DEFAULT TRUE,
    is_sold BOOLEAN DEFAULT FALSE,
    is_featured BOOLEAN DEFAULT FALSE,
    view_count INTEGER DEFAULT 0,
    favorite_count INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ==============================================================================
-- 4. LISTING IMAGES TABLE
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.listing_images (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    listing_id UUID NOT NULL REFERENCES public.listings(id) ON DELETE CASCADE,
    image_url TEXT NOT NULL,
    is_primary BOOLEAN DEFAULT FALSE,
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ==============================================================================
-- 5. FAVORITES TABLE
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.favorites (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    listing_id UUID NOT NULL REFERENCES public.listings(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (user_id, listing_id)
);

-- ==============================================================================
-- 6. CARTS & CART ITEMS TABLES
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.carts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL UNIQUE REFERENCES public.profiles(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.cart_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    cart_id UUID REFERENCES public.carts(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    listing_id UUID NOT NULL REFERENCES public.listings(id) ON DELETE CASCADE,
    quantity INTEGER NOT NULL DEFAULT 1 CHECK (quantity > 0),
    added_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (user_id, listing_id)
);

-- ==============================================================================
-- 7. ORDERS / TRANSACTIONS TABLE (No online payment - physical campus handover)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    buyer_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    seller_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    total_amount NUMERIC(12, 2) NOT NULL CHECK (total_amount >= 0),
    status TEXT NOT NULL DEFAULT 'PENDING', -- PENDING, ACCEPTED, DECLINED, MEETING_ARRANGED, COMPLETED, CANCELLED
    payment_status TEXT NOT NULL DEFAULT 'PHYSICAL_PENDING', -- PHYSICAL_PENDING, PAID_ON_CAMPUS, CANCELLED
    delivery_method TEXT NOT NULL DEFAULT 'PHYSICAL_MEETUP', -- PHYSICAL_MEETUP, IN_PERSON_CAMPUS
    meetup_location TEXT NOT NULL DEFAULT 'UNIMAID Main Campus', -- Library, Senate Building, Complex, Faculty
    buyer_note TEXT,
    seller_note TEXT,
    meeting_time TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.order_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL REFERENCES public.orders(id) ON DELETE CASCADE,
    listing_id UUID NOT NULL REFERENCES public.listings(id) ON DELETE CASCADE,
    quantity INTEGER NOT NULL DEFAULT 1 CHECK (quantity > 0),
    unit_price NUMERIC(12, 2) NOT NULL CHECK (unit_price >= 0),
    subtotal NUMERIC(12, 2) NOT NULL CHECK (subtotal >= 0),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.order_status_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL REFERENCES public.orders(id) ON DELETE CASCADE,
    status TEXT NOT NULL,
    changed_by UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    note TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ==============================================================================
-- 8. CONVERSATIONS & REALTIME MESSAGES
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.conversations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    participant1_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    participant2_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    listing_id UUID REFERENCES public.listings(id) ON DELETE SET NULL,
    order_id UUID REFERENCES public.orders(id) ON DELETE SET NULL,
    last_message_text TEXT,
    last_message_at TIMESTAMPTZ DEFAULT NOW(),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (participant1_id, participant2_id, listing_id)
);

CREATE TABLE IF NOT EXISTS public.conversation_participants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    conversation_id UUID NOT NULL REFERENCES public.conversations(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    last_read_at TIMESTAMPTZ DEFAULT NOW(),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (conversation_id, user_id)
);

CREATE TABLE IF NOT EXISTS public.messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    conversation_id UUID NOT NULL REFERENCES public.conversations(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    receiver_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ==============================================================================
-- 9. NOTIFICATIONS TABLE
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    type TEXT NOT NULL DEFAULT 'SYSTEM', -- MESSAGE, ORDER, LISTING_FAVORITE, PRICE_DROP, VERIFICATION, SYSTEM
    is_read BOOLEAN DEFAULT FALSE,
    data_payload JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ==============================================================================
-- 10. REPORTS & SAFETY TABLE
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.reports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reporter_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    reported_user_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    reported_listing_id UUID REFERENCES public.listings(id) ON DELETE SET NULL,
    reason TEXT NOT NULL,
    description TEXT,
    status TEXT NOT NULL DEFAULT 'PENDING', -- PENDING, INVESTIGATING, RESOLVED, DISMISSED
    resolution_notes TEXT,
    reviewed_by UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ==============================================================================
-- 11. USER SETTINGS TABLE
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.user_settings (
    user_id UUID PRIMARY KEY REFERENCES public.profiles(id) ON DELETE CASCADE,
    push_notifications_enabled BOOLEAN DEFAULT TRUE,
    email_notifications_enabled BOOLEAN DEFAULT TRUE,
    show_phone_number BOOLEAN DEFAULT FALSE,
    theme_mode TEXT DEFAULT 'SYSTEM', -- SYSTEM, LIGHT, DARK
    campus_location_preference TEXT DEFAULT 'UNIMAID Main Campus',
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ==============================================================================
-- 12. VERIFICATION REQUESTS TABLE
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.verification_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    matric_number TEXT NOT NULL,
    student_id_card_url TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
    rejection_reason TEXT,
    reviewed_by UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    reviewed_at TIMESTAMPTZ,
    submitted_at TIMESTAMPTZ DEFAULT NOW()
);

-- ==============================================================================
-- 13. ADMIN ROLES TABLE
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.admin_roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    role TEXT NOT NULL DEFAULT 'MODERATOR', -- SUPER_ADMIN, ADMIN, MODERATOR
    granted_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (user_id, role)
);

-- ==============================================================================
-- 14. PERFORMANCE INDEXES
-- ==============================================================================
CREATE INDEX IF NOT EXISTS idx_listings_seller ON public.listings(seller_id);
CREATE INDEX IF NOT EXISTS idx_listings_category ON public.listings(category_id);
CREATE INDEX IF NOT EXISTS idx_listings_created_at ON public.listings(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_listings_price ON public.listings(price);
CREATE INDEX IF NOT EXISTS idx_listings_is_available ON public.listings(is_available);
CREATE INDEX IF NOT EXISTS idx_listing_images_listing_id ON public.listing_images(listing_id);

CREATE INDEX IF NOT EXISTS idx_favorites_user ON public.favorites(user_id);
CREATE INDEX IF NOT EXISTS idx_favorites_listing ON public.favorites(listing_id);

CREATE INDEX IF NOT EXISTS idx_cart_items_user ON public.cart_items(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_buyer ON public.orders(buyer_id);
CREATE INDEX IF NOT EXISTS idx_orders_seller ON public.orders(seller_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON public.orders(status);
CREATE INDEX IF NOT EXISTS idx_order_items_order ON public.order_items(order_id);

CREATE INDEX IF NOT EXISTS idx_conversations_p1 ON public.conversations(participant1_id);
CREATE INDEX IF NOT EXISTS idx_conversations_p2 ON public.conversations(participant2_id);
CREATE INDEX IF NOT EXISTS idx_messages_conversation ON public.messages(conversation_id);
CREATE INDEX IF NOT EXISTS idx_messages_created_at ON public.messages(created_at ASC);

CREATE INDEX IF NOT EXISTS idx_notifications_user_unread ON public.notifications(user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_reports_status ON public.reports(status);

-- ==============================================================================
-- 15. ROW LEVEL SECURITY (RLS) POLICIES
-- ==============================================================================

-- Enable RLS on all tables
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.listings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.listing_images ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.favorites ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.carts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.cart_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.order_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.order_status_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.conversations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.conversation_participants ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.verification_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.admin_roles ENABLE ROW LEVEL SECURITY;

-- Profiles Policies
CREATE POLICY "Public profiles are viewable by all authenticated users"
ON public.profiles FOR SELECT
USING (true);

CREATE POLICY "Users can insert their own profile"
ON public.profiles FOR INSERT
WITH CHECK (auth.uid() = id);

CREATE POLICY "Users can update their own profile"
ON public.profiles FOR UPDATE
USING (auth.uid() = id);

-- Categories Policies
CREATE POLICY "Categories are readable by everyone"
ON public.categories FOR SELECT
USING (true);

-- Listings Policies
CREATE POLICY "Listings are viewable by everyone"
ON public.listings FOR SELECT
USING (true);

CREATE POLICY "Users can create their own listings"
ON public.listings FOR INSERT
WITH CHECK (auth.uid() = seller_id);

CREATE POLICY "Users can update their own listings"
ON public.listings FOR UPDATE
USING (auth.uid() = seller_id);

CREATE POLICY "Users can delete their own listings"
ON public.listings FOR DELETE
USING (auth.uid() = seller_id);

-- Listing Images Policies
CREATE POLICY "Listing images are viewable by everyone"
ON public.listing_images FOR SELECT
USING (true);

CREATE POLICY "Sellers can manage images for their listings"
ON public.listing_images FOR INSERT
WITH CHECK (
    EXISTS (
        SELECT 1 FROM public.listings
        WHERE id = listing_images.listing_id AND seller_id = auth.uid()
    )
);

CREATE POLICY "Sellers can delete images for their listings"
ON public.listing_images FOR DELETE
USING (
    EXISTS (
        SELECT 1 FROM public.listings
        WHERE id = listing_images.listing_id AND seller_id = auth.uid()
    )
);

-- Favorites Policies
CREATE POLICY "Users can view their own favorites"
ON public.favorites FOR SELECT
USING (auth.uid() = user_id);

CREATE POLICY "Users can add favorites"
ON public.favorites FOR INSERT
WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can remove favorites"
ON public.favorites FOR DELETE
USING (auth.uid() = user_id);

-- Cart & Cart Items Policies
CREATE POLICY "Users can view their own cart"
ON public.carts FOR SELECT
USING (auth.uid() = user_id);

CREATE POLICY "Users can view their own cart items"
ON public.cart_items FOR SELECT
USING (auth.uid() = user_id);

CREATE POLICY "Users can insert their own cart items"
ON public.cart_items FOR INSERT
WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update their own cart items"
ON public.cart_items FOR UPDATE
USING (auth.uid() = user_id);

CREATE POLICY "Users can delete their own cart items"
ON public.cart_items FOR DELETE
USING (auth.uid() = user_id);

-- Orders Policies
CREATE POLICY "Buyers and sellers can view their own orders"
ON public.orders FOR SELECT
USING (auth.uid() = buyer_id OR auth.uid() = seller_id);

CREATE POLICY "Buyers can create orders"
ON public.orders FOR INSERT
WITH CHECK (auth.uid() = buyer_id);

CREATE POLICY "Buyers and sellers can update their orders"
ON public.orders FOR UPDATE
USING (auth.uid() = buyer_id OR auth.uid() = seller_id);

-- Order Items Policies
CREATE POLICY "Order items viewable by order participants"
ON public.order_items FOR SELECT
USING (
    EXISTS (
        SELECT 1 FROM public.orders
        WHERE id = order_items.order_id AND (buyer_id = auth.uid() OR seller_id = auth.uid())
    )
);

CREATE POLICY "Buyers can create order items"
ON public.order_items FOR INSERT
WITH CHECK (
    EXISTS (
        SELECT 1 FROM public.orders
        WHERE id = order_items.order_id AND buyer_id = auth.uid()
    )
);

-- Conversations Policies
CREATE POLICY "Participants can view their conversations"
ON public.conversations FOR SELECT
USING (auth.uid() = participant1_id OR auth.uid() = participant2_id);

CREATE POLICY "Users can start conversations"
ON public.conversations FOR INSERT
WITH CHECK (auth.uid() = participant1_id OR auth.uid() = participant2_id);

CREATE POLICY "Participants can update conversations"
ON public.conversations FOR UPDATE
USING (auth.uid() = participant1_id OR auth.uid() = participant2_id);

-- Messages Policies
CREATE POLICY "Participants can view messages in their conversations"
ON public.messages FOR SELECT
USING (
    auth.uid() = sender_id OR auth.uid() = receiver_id OR
    EXISTS (
        SELECT 1 FROM public.conversations
        WHERE id = messages.conversation_id AND (participant1_id = auth.uid() OR participant2_id = auth.uid())
    )
);

CREATE POLICY "Senders can send messages"
ON public.messages FOR INSERT
WITH CHECK (auth.uid() = sender_id);

CREATE POLICY "Receivers can update message read status"
ON public.messages FOR UPDATE
USING (auth.uid() = receiver_id);

-- Notifications Policies
CREATE POLICY "Users can view and manage their own notifications"
ON public.notifications FOR ALL
USING (auth.uid() = user_id);

-- Reports Policies
CREATE POLICY "Users can create reports"
ON public.reports FOR INSERT
WITH CHECK (auth.uid() = reporter_id);

CREATE POLICY "Reporters can view their own reports"
ON public.reports FOR SELECT
USING (auth.uid() = reporter_id);

-- User Settings Policies
CREATE POLICY "Users can view and update their own settings"
ON public.user_settings FOR ALL
USING (auth.uid() = user_id);

-- Verification Requests Policies
CREATE POLICY "Students can view their own verification requests"
ON public.verification_requests FOR SELECT
USING (auth.uid() = student_id);

CREATE POLICY "Students can submit verification requests"
ON public.verification_requests FOR INSERT
WITH CHECK (auth.uid() = student_id);

-- Admin Roles Policies
CREATE POLICY "Users can view their own admin roles"
ON public.admin_roles FOR SELECT
USING (auth.uid() = user_id);

-- ==============================================================================
-- 16. REALTIME PUBLICATION CONFIGURATION
-- ==============================================================================
-- Enable Realtime publication for relevant reactive tables
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables
        WHERE pubname = 'supabase_realtime' AND tablename = 'messages'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.messages;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables
        WHERE pubname = 'supabase_realtime' AND tablename = 'conversations'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.conversations;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables
        WHERE pubname = 'supabase_realtime' AND tablename = 'orders'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.orders;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables
        WHERE pubname = 'supabase_realtime' AND tablename = 'notifications'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.notifications;
    END IF;
END $$;
