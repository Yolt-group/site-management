CREATE TABLE IF NOT EXISTS user_batch_state (
    id uuid,
    fetch_size int,
    next_page text,
    PRIMARY KEY (id)
);