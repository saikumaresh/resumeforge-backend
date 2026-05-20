-- V5: user plan tier, Google OAuth ID, subscriptions table

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS google_id     VARCHAR(255),
    ADD COLUMN IF NOT EXISTS plan          VARCHAR(20) NOT NULL DEFAULT 'FREE',
    ADD COLUMN IF NOT EXISTS picture_url   VARCHAR(512);

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_google_id ON users (google_id) WHERE google_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS subscriptions (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    razorpay_order_id   VARCHAR(100),
    razorpay_payment_id VARCHAR(100),
    razorpay_signature  VARCHAR(512),
    plan           VARCHAR(20)  NOT NULL DEFAULT 'PRO',
    amount_paise   INTEGER      NOT NULL,
    currency       VARCHAR(10)  NOT NULL DEFAULT 'INR',
    status         VARCHAR(30)  NOT NULL DEFAULT 'CREATED',  -- CREATED | PAID | FAILED
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_subscriptions_user_id ON subscriptions(user_id);
