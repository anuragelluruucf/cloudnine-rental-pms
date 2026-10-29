CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_app_users_email UNIQUE (email)
);

CREATE TABLE properties (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES app_users (id),
    name VARCHAR(255) NOT NULL,
    address VARCHAR(500) NOT NULL,
    description VARCHAR(4000),
    house_rules VARCHAR(4000),
    amenities VARCHAR(4000),
    check_in_info VARCHAR(4000),
    check_out_info VARCHAR(4000),
    wifi_network VARCHAR(255),
    wifi_password VARCHAR(255),
    faq_notes VARCHAR(4000),
    export_token UUID NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_properties_export_token UNIQUE (export_token)
);

CREATE TABLE channel_sources (
    id UUID PRIMARY KEY,
    property_id UUID NOT NULL REFERENCES properties (id),
    channel VARCHAR(32) NOT NULL,
    import_url VARCHAR(1000),
    last_synced_at TIMESTAMP(6) WITH TIME ZONE,
    last_sync_message VARCHAR(1000),
    enabled BOOLEAN NOT NULL,
    CONSTRAINT uq_channel_property UNIQUE (property_id, channel)
);

CREATE TABLE reservations (
    id UUID PRIMARY KEY,
    property_id UUID NOT NULL REFERENCES properties (id),
    channel_source_id UUID REFERENCES channel_sources (id),
    channel VARCHAR(32) NOT NULL,
    guest_name VARCHAR(255),
    guest_email VARCHAR(255),
    check_in DATE NOT NULL,
    check_out DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    external_uid VARCHAR(512),
    conflict BOOLEAN NOT NULL,
    notes VARCHAR(4000),
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_res_property_dates ON reservations (property_id, check_in, check_out);

CREATE UNIQUE INDEX uq_channel_uid ON reservations (channel_source_id, external_uid)
    WHERE external_uid IS NOT NULL AND channel_source_id IS NOT NULL;

CREATE TABLE guest_messages (
    id UUID PRIMARY KEY,
    property_id UUID NOT NULL REFERENCES properties (id),
    guest_name VARCHAR(255) NOT NULL,
    body VARCHAR(4000) NOT NULL,
    draft_reply VARCHAR(8000),
    sent_reply VARCHAR(8000),
    evidence VARCHAR(8000),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    sent_at TIMESTAMP(6) WITH TIME ZONE
);

CREATE TABLE knowledge_chunks (
    id UUID PRIMARY KEY,
    property_id UUID NOT NULL REFERENCES properties (id),
    source VARCHAR(64) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    position INTEGER NOT NULL
);

CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE reservations
    ADD CONSTRAINT reservations_no_overlap
    EXCLUDE USING gist (
        property_id WITH =,
        daterange(check_in, check_out, '[)') WITH &&
    )
    WHERE (status IN ('CONFIRMED', 'BLOCKED'));
