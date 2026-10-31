-- OutfitRating — initial schema.
-- Naming: snake_case tables/columns, surrogate bigserial keys everywhere (including join-like
-- tables) so JPA mappings stay free of composite identifiers.

create table users (
    id            bigserial primary key,
    username      varchar(32)  not null unique,
    email         varchar(160) not null unique,
    password_hash varchar(100) not null,
    display_name  varchar(64)  not null,
    bio           varchar(280),
    avatar_url    varchar(512),
    location      varchar(80),
    role          varchar(16)  not null default 'USER',
    created_at    timestamptz  not null default now()
);

create table brands (
    id          bigserial primary key,
    name        varchar(80) not null unique,
    slug        varchar(96) not null unique,
    country     varchar(64),
    logo_url    varchar(512),
    description varchar(600),
    created_at  timestamptz not null default now()
);

create table tags (
    id         bigserial primary key,
    name       varchar(40) not null unique,
    slug       varchar(48) not null unique,
    created_at timestamptz not null default now()
);

create table looks (
    id              bigserial primary key,
    slug            varchar(160) not null unique,
    title           varchar(120) not null,
    type            varchar(16)  not null,
    category        varchar(32)  not null,
    season          varchar(16),
    occasion        varchar(24),
    description     varchar(2000),
    image_url       varchar(512) not null,
    brand_id        bigint references brands (id) on delete set null,
    author_id       bigint       not null references users (id) on delete cascade,
    price_cents     integer,
    currency        varchar(3),
    release_date    date,
    featured        boolean      not null default false,
    ratings_count   integer      not null default 0,
    reviews_count   integer      not null default 0,
    avg_score       double precision,
    avg_style       double precision,
    avg_fit         double precision,
    avg_color       double precision,
    avg_originality double precision,
    avg_quality     double precision,
    created_at      timestamptz  not null default now(),
    constraint ck_looks_type check (type in ('OUTFIT', 'ITEM'))
);

create table look_tags (
    look_id bigint not null references looks (id) on delete cascade,
    tag_id  bigint not null references tags (id) on delete cascade,
    primary key (look_id, tag_id)
);

create table reviews (
    id          bigserial primary key,
    look_id     bigint      not null references looks (id) on delete cascade,
    author_id   bigint      not null references users (id) on delete cascade,
    style       smallint    not null,
    fit         smallint    not null,
    color       smallint    not null,
    originality smallint    not null,
    quality     smallint    not null,
    score       smallint    not null,
    title       varchar(140),
    body        varchar(5000),
    likes_count integer     not null default 0,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now(),
    constraint uq_reviews_look_author unique (look_id, author_id),
    constraint ck_reviews_style check (style between 1 and 10),
    constraint ck_reviews_fit check (fit between 1 and 10),
    constraint ck_reviews_color check (color between 1 and 10),
    constraint ck_reviews_originality check (originality between 1 and 10),
    constraint ck_reviews_quality check (quality between 1 and 10),
    constraint ck_reviews_score check (score between 0 and 100)
);

create table review_likes (
    id         bigserial primary key,
    review_id  bigint      not null references reviews (id) on delete cascade,
    user_id    bigint      not null references users (id) on delete cascade,
    created_at timestamptz not null default now(),
    constraint uq_review_likes_review_user unique (review_id, user_id)
);

create table comments (
    id         bigserial primary key,
    look_id    bigint        not null references looks (id) on delete cascade,
    author_id  bigint        not null references users (id) on delete cascade,
    parent_id  bigint references comments (id) on delete cascade,
    body       varchar(1500) not null,
    created_at timestamptz   not null default now()
);

create table favorites (
    id         bigserial primary key,
    user_id    bigint      not null references users (id) on delete cascade,
    look_id    bigint      not null references looks (id) on delete cascade,
    created_at timestamptz not null default now(),
    constraint uq_favorites_user_look unique (user_id, look_id)
);

create table follows (
    id           bigserial primary key,
    follower_id  bigint      not null references users (id) on delete cascade,
    following_id bigint      not null references users (id) on delete cascade,
    created_at   timestamptz not null default now(),
    constraint uq_follows_pair unique (follower_id, following_id),
    constraint ck_follows_not_self check (follower_id <> following_id)
);

create table refresh_tokens (
    id         bigserial primary key,
    token_hash varchar(64) not null unique,
    user_id    bigint      not null references users (id) on delete cascade,
    expires_at timestamptz not null,
    revoked    boolean     not null default false,
    created_at timestamptz not null default now()
);

-- Catalogue browsing: newest first, and the leaderboard ordering.
create index idx_looks_created_at on looks (created_at desc);
create index idx_looks_avg_score on looks (avg_score desc nulls last);
create index idx_looks_type on looks (type);
create index idx_looks_category on looks (category);
create index idx_looks_brand on looks (brand_id);
create index idx_looks_author on looks (author_id);

-- Review feed and per-look review listing.
create index idx_reviews_created_at on reviews (created_at desc);
create index idx_reviews_look on reviews (look_id);
create index idx_reviews_author on reviews (author_id);

create index idx_comments_look on comments (look_id, created_at);
create index idx_favorites_user on favorites (user_id, created_at desc);
create index idx_follows_following on follows (following_id);
create index idx_refresh_tokens_user on refresh_tokens (user_id);
