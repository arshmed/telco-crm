ALTER TABLE subscriptions ADD COLUMN order_id UUID;

CREATE INDEX idx_subscriptions_order_id ON subscriptions(order_id);
