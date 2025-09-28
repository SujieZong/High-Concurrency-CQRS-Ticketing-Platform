-- mysql -u root -p < schema.sql

CREATE DATABASE IF NOT EXISTS ticket_platform
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
USE ticket_platform;

-- 1. 场馆表（Venue）
CREATE TABLE venue (
                       venue_id    VARCHAR(64)  PRIMARY KEY,
                       city        VARCHAR(64)  NOT NULL
    -- 还可以加 address、capacity 等
);

-- 2. 分区表（Zone）
CREATE TABLE zone (
                      venue_id      VARCHAR(64)   NOT NULL,
                      zone_id       INT            NOT NULL,
                      ticket_price  DECIMAL(10,2)  NOT NULL,
                      row_count     INT            NOT NULL,
                      col_count     INT            NOT NULL,
                      PRIMARY KEY (venue_id, zone_id),
                      FOREIGN KEY (venue_id) REFERENCES venue(venue_id)
);

-- 3. 活动表（Event）
CREATE TABLE event (
                       event_id   VARCHAR(64)  PRIMARY KEY,
                       venue_id   VARCHAR(64)  NOT NULL,
                       name       VARCHAR(128) NOT NULL,
                       type       VARCHAR(64),
                       event_date DATE         NOT NULL,
                       FOREIGN KEY (venue_id) REFERENCES venue(venue_id)
);

-- 4. 购票表（Ticket）
CREATE TABLE ticket (
                        ticket_id   VARCHAR(64)    PRIMARY KEY,
                        venue_id    VARCHAR(64)    NOT NULL,
                        event_id    VARCHAR(64)    NOT NULL,
                        zone_id     INT            NOT NULL,
                        row_label   VARCHAR(8)     NOT NULL,
                        col_label   VARCHAR(8)     NOT NULL,
                        created_on  DATETIME       NOT NULL,
                        FOREIGN KEY (venue_id) REFERENCES venue(venue_id),
                        FOREIGN KEY (event_id) REFERENCES event(event_id),
                        FOREIGN KEY (venue_id, zone_id) REFERENCES zone(venue_id, zone_id)
);