CREATE TABLE subscription_addons (
    id              UUID PRIMARY KEY,
    subscription_id UUID NOT NULL REFERENCES subscriptions(id),
    addon_code      VARCHAR(50) NOT NULL,
    added_at        TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_subscription_addons_subscription_id ON subscription_addons(subscription_id);
