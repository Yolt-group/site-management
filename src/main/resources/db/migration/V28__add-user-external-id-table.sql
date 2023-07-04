CREATE TABLE user_external_id
(
    user_id          uuid NOT NULL,
    provider         text NOT NULL,
    external_user_id text NOT NULL,
    primary key (user_id, provider)
);
