-- MySQL initializing data, collaborate with bootstrap to work

-- venue basic (Bootstrapcan over write)
INSERT INTO venue (venue_id, city) VALUES 
('Venue1', 'Vancouver'), 
('venue-test', 'Beijing') 
ON DUPLICATE KEY UPDATE city=VALUES(city);

-- Vent basic (Bootstrapcan over write)
INSERT INTO event (event_id, venue_id, name, type, event_date) VALUES 
('Event1', 'Venue1', 'Spring Concert 2025', 'Concert', '2025-12-25'),
('event-test', 'venue-test', 'Test Event', 'Test', '2025-11-15') 
ON DUPLICATE KEY UPDATE name=VALUES(name), type=VALUES(type), event_date=VALUES(event_date);