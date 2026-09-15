CREATE TABLE dice.roll
(
    id         UUID PRIMARY KEY,
    rolled_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    expression TEXT                     NOT NULL,
    dice       INTEGER[]                NOT NULL,
    advantage  TEXT                     NOT NULL,
    total      INTEGER                  NOT NULL,
    source     TEXT                     NOT NULL,
    reason     TEXT
)