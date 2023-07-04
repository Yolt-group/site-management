CREATE TABLE activity_events
(
    event_id    uuid        NOT NULL PRIMARY KEY,
    user_id     uuid        NOT NULL,
    activity_id uuid        NOT NULL,
    event       jsonb       NOT NULL,
    event_time  timestamptz NOT NULL
);

CREATE INDEX idx_activity_events_event_time ON activity_events (event_time);
CREATE INDEX idx_event_time_asc ON activity_events (activity_id, event_time ASC);


CREATE TABLE activity
(
    id               uuid           PRIMARY KEY,
    user_id          uuid           NOT NULL,
    start_time       timestamptz    NOT NULL,
    end_time         timestamptz,
    start_event_type text           NOT NULL,
    user_site_ids    uuid[]         NOT NULL
);

CREATE INDEX idx_activity_start_time ON activity (start_time);
CREATE INDEX idx_activity_time_by_user_id_asc ON activity (user_id, start_time);
