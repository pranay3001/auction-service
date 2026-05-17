-- ============================================================
-- V2__seed_data.sql — Seed data for auction-service
-- ============================================================

-- Internal users
INSERT INTO internal_users (id, name, email, password_hash, role) VALUES
('71234567-0000-0000-0000-000000000001', 'Arjun Mehta',   'ops@optimile.com',          '$2a$10$dummy', 'OPS'),
('71234567-0000-0000-0000-000000000002', 'Naina Kapoor',  'procurement@optimile.com',  '$2a$10$dummy', 'PROCUREMENT'),
('71234567-0000-0000-0000-000000000003', 'Samar Verma',   'admin@optimile.com',         '$2a$10$dummy', 'ADMIN');

-- Vendors
INSERT INTO vendors (id, name, score) VALUES
('a1000000-0000-0000-0000-000000000001', 'SwiftHaul Logistics', 92),
('a1000000-0000-0000-0000-000000000002', 'BlueAxle Freight',    88),
('a1000000-0000-0000-0000-000000000003', 'RoadBridge Carriers', 84),
('a1000000-0000-0000-0000-000000000004', 'MetroFleet Movers',   79);

-- Bookings
INSERT INTO bookings (id, lane, vehicle_type, commodity, quantity, uom, loading_date, status) VALUES
('b0000000-0000-0000-0000-000000000001', 'Mumbai → Delhi',  '20 MT Open Body',  'FMCG',               18, 'MT',    '2026-06-01', 'PENDING_AUCTION'),
('b0000000-0000-0000-0000-000000000002', 'Pune → Jaipur',   '32 FT Closed Body','Consumer Durables',  22, 'MT',    '2026-06-05', 'PENDING_AUCTION'),
('b0000000-0000-0000-0000-000000000003', 'Delhi → Lucknow', '32 FT Closed Body','Industrial Goods',   30, 'MT',    '2026-06-10', 'PENDING_AUCTION');

-- Auctions
-- AUC-1: SPOT COMPLETED (ready for award)
INSERT INTO auctions (id, title, type, status, created_by_id, created_by_role, start_at, completed_at, booking_id,
    min_bid_decrement, extension_trigger_minutes, extension_duration_minutes, max_extensions, bidding_window_minutes, award_deadline)
VALUES (
    'c0000000-0000-0000-0000-000000000001',
    'Spot | BK-2026-11203 | Mumbai → Delhi',
    'SPOT', 'COMPLETED',
    '71234567-0000-0000-0000-000000000001', 'OPS',
    NOW() - INTERVAL '3 hours', NOW() - INTERVAL '1 hour',
    'b0000000-0000-0000-0000-000000000001',
    200, 5, 5, 3, 120,
    NOW() + INTERVAL '1 hour'
);

-- AUC-2: BULK COMPLETED (ready for award, 2 lanes)
INSERT INTO auctions (id, title, type, status, created_by_id, created_by_role, start_at, completed_at,
    contract_start_date, contract_end_date,
    min_bid_decrement, extension_trigger_minutes, extension_duration_minutes, max_extensions, bidding_window_minutes, award_deadline)
VALUES (
    'c0000000-0000-0000-0000-000000000002',
    'Bulk | South Corridor Contract Rates',
    'BULK', 'COMPLETED',
    '71234567-0000-0000-0000-000000000002', 'PROCUREMENT',
    NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day',
    '2026-07-01', '2026-12-31',
    200, 5, 5, 3, 1440,
    NOW() + INTERVAL '4 days'
);

-- AUC-3: LOT COMPLETED
INSERT INTO auctions (id, title, type, status, created_by_id, created_by_role, start_at, completed_at, region,
    contract_start_date, contract_end_date,
    min_bid_decrement, extension_trigger_minutes, extension_duration_minutes, max_extensions, bidding_window_minutes, award_deadline)
VALUES (
    'c0000000-0000-0000-0000-000000000003',
    'Lot | North India Regional Procurement',
    'LOT', 'COMPLETED',
    '71234567-0000-0000-0000-000000000001', 'OPS',
    NOW() - INTERVAL '5 days', NOW() - INTERVAL '4 days',
    'North India',
    '2026-07-01', '2026-09-30',
    250, 5, 5, 3, 1440,
    NOW() + INTERVAL '1 day'
);

-- AUC-4: LIVE SPOT
INSERT INTO auctions (id, title, type, status, created_by_id, created_by_role, start_at, booking_id,
    min_bid_decrement, extension_trigger_minutes, extension_duration_minutes, max_extensions, bidding_window_minutes, award_deadline)
VALUES (
    'c0000000-0000-0000-0000-000000000004',
    'Spot | BK-2026-11261 | Pune → Jaipur',
    'SPOT', 'LIVE',
    '71234567-0000-0000-0000-000000000002', 'PROCUREMENT',
    NOW() - INTERVAL '30 minutes',
    'b0000000-0000-0000-0000-000000000002',
    200, 5, 5, 3, 120,
    NOW() + INTERVAL '8 hours'
);

-- AUC-5: DRAFT BULK
INSERT INTO auctions (id, title, type, status, created_by_id, created_by_role,
    contract_start_date, contract_end_date,
    min_bid_decrement, extension_trigger_minutes, extension_duration_minutes, max_extensions, bidding_window_minutes)
VALUES (
    'c0000000-0000-0000-0000-000000000005',
    'Bulk | West Zone Replenishment Q3',
    'BULK', 'DRAFT',
    '71234567-0000-0000-0000-000000000003', 'ADMIN',
    '2026-08-01', '2026-10-31',
    200, 5, 5, 3, 1440
);

-- Lanes for AUC-1 (SPOT, 1 lane)
INSERT INTO auction_lanes (id, auction_id, lane, vehicle_type, capacity_mt, rate_unit, ceiling_rate, estimated_trips,
    allocation_mode, l1_allocation_pct, extension_count, bid_count, timer_ends_at)
VALUES ('d0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001',
    'Mumbai → Delhi', '20 MT Open Body', 20, 'PER_TRIP', 52000, 1,
    'SINGLE', 100, 1, 9, NOW() - INTERVAL '1 hour');

-- Lanes for AUC-2 (BULK, 2 lanes)
INSERT INTO auction_lanes (id, auction_id, lane, vehicle_type, capacity_mt, rate_unit, ceiling_rate, estimated_trips,
    allocation_mode, l1_allocation_pct, l2_allocation_pct, l3_allocation_pct, extension_count, bid_count, timer_ends_at)
VALUES
('d0000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000002',
    'Mumbai → Bangalore', '20 MT Open Body', 20, 'PER_TRIP', 10000, 400,
    'SPLIT', 60, 30, 10, 0, 6, NOW() - INTERVAL '1 day'),
('d0000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000002',
    'Bangalore → Chennai', '20 MT Open Body', 20, 'PER_KM', 11000, 300,
    'SINGLE', 100, 0, 0, 0, 4, NOW() - INTERVAL '1 day');

-- Lanes for AUC-3 (LOT, 1 lane)
INSERT INTO auction_lanes (id, auction_id, lane, vehicle_type, capacity_mt, rate_unit, ceiling_rate, estimated_trips,
    allocation_mode, l1_allocation_pct, l2_allocation_pct, l3_allocation_pct, extension_count, bid_count, timer_ends_at)
VALUES ('d0000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000003',
    'Delhi → Lucknow', '32 FT Closed Body', 32, 'PER_MT', 14500, 220,
    'SPLIT', 70, 20, 10, 0, 3, NOW() - INTERVAL '4 days');

-- Lanes for AUC-4 (LIVE SPOT, 1 lane)
INSERT INTO auction_lanes (id, auction_id, lane, vehicle_type, capacity_mt, rate_unit, ceiling_rate, estimated_trips,
    allocation_mode, l1_allocation_pct, extension_count, bid_count, timer_ends_at)
VALUES ('d0000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000004',
    'Pune → Jaipur', '32 FT Closed Body', 32, 'PER_TRIP', 38000, 1,
    'SINGLE', 100, 0, 2, NOW() + INTERVAL '90 minutes');

-- Lanes for AUC-5 (DRAFT BULK)
INSERT INTO auction_lanes (id, auction_id, lane, vehicle_type, capacity_mt, rate_unit, ceiling_rate, estimated_trips,
    allocation_mode, l1_allocation_pct, l2_allocation_pct, l3_allocation_pct, extension_count, bid_count)
VALUES ('d0000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000005',
    'Ahmedabad → Surat', '20 MT Open Body', 20, 'PER_TRIP', 8500, 200,
    'SPLIT', 60, 30, 10, 0, 0);

-- Invited vendors for auctions
INSERT INTO auction_invited_vendors (auction_id, vendor_id) VALUES
('c0000000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000001'),
('c0000000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000002'),
('c0000000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000003'),
('c0000000-0000-0000-0000-000000000002', 'a1000000-0000-0000-0000-000000000001'),
('c0000000-0000-0000-0000-000000000002', 'a1000000-0000-0000-0000-000000000002'),
('c0000000-0000-0000-0000-000000000002', 'a1000000-0000-0000-0000-000000000003'),
('c0000000-0000-0000-0000-000000000002', 'a1000000-0000-0000-0000-000000000004'),
('c0000000-0000-0000-0000-000000000003', 'a1000000-0000-0000-0000-000000000001'),
('c0000000-0000-0000-0000-000000000003', 'a1000000-0000-0000-0000-000000000002'),
('c0000000-0000-0000-0000-000000000003', 'a1000000-0000-0000-0000-000000000003'),
('c0000000-0000-0000-0000-000000000004', 'a1000000-0000-0000-0000-000000000001'),
('c0000000-0000-0000-0000-000000000004', 'a1000000-0000-0000-0000-000000000002');

-- Bids for AUC-1 lane (SPOT COMPLETED - 3 ranked bids)
INSERT INTO auction_bids (id, auction_id, lane_id, vendor_id, vendor_name, amount, bid_rank, is_current, placed_at) VALUES
('e0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
 'a1000000-0000-0000-0000-000000000002', 'BlueAxle Freight',    48600, 1, true, NOW() - INTERVAL '90 minutes'),
('e0000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
 'a1000000-0000-0000-0000-000000000001', 'SwiftHaul Logistics', 48900, 2, true, NOW() - INTERVAL '95 minutes'),
('e0000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
 'a1000000-0000-0000-0000-000000000003', 'RoadBridge Carriers', 49300, 3, true, NOW() - INTERVAL '100 minutes');

-- Bids for AUC-2 lane 1 (BULK lane 1 - 3 bids)
INSERT INTO auction_bids (id, auction_id, lane_id, vendor_id, vendor_name, amount, bid_rank, is_current, placed_at) VALUES
('e0000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000002',
 'a1000000-0000-0000-0000-000000000001', 'SwiftHaul Logistics', 8200, 1, true, NOW() - INTERVAL '25 hours'),
('e0000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000002',
 'a1000000-0000-0000-0000-000000000002', 'BlueAxle Freight',    8500, 2, true, NOW() - INTERVAL '26 hours'),
('e0000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000002',
 'a1000000-0000-0000-0000-000000000004', 'MetroFleet Movers',   9100, 3, true, NOW() - INTERVAL '27 hours');

-- Bids for AUC-2 lane 2
INSERT INTO auction_bids (id, auction_id, lane_id, vendor_id, vendor_name, amount, bid_rank, is_current, placed_at) VALUES
('e0000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000003',
 'a1000000-0000-0000-0000-000000000003', 'RoadBridge Carriers', 9400, 1, true, NOW() - INTERVAL '25 hours'),
('e0000000-0000-0000-0000-000000000008', 'c0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000003',
 'a1000000-0000-0000-0000-000000000001', 'SwiftHaul Logistics', 9800, 2, true, NOW() - INTERVAL '26 hours');

-- Bids for AUC-3 lane (LOT - 3 bids)
INSERT INTO auction_bids (id, auction_id, lane_id, vendor_id, vendor_name, amount, bid_rank, is_current, placed_at) VALUES
('e0000000-0000-0000-0000-000000000009', 'c0000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000004',
 'a1000000-0000-0000-0000-000000000002', 'BlueAxle Freight',    11800, 1, true, NOW() - INTERVAL '4 days 2 hours'),
('e0000000-0000-0000-0000-000000000010', 'c0000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000004',
 'a1000000-0000-0000-0000-000000000001', 'SwiftHaul Logistics', 12200, 2, true, NOW() - INTERVAL '4 days 3 hours'),
('e0000000-0000-0000-0000-000000000011', 'c0000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000004',
 'a1000000-0000-0000-0000-000000000003', 'RoadBridge Carriers', 13100, 3, true, NOW() - INTERVAL '4 days 4 hours');

-- Bids for AUC-4 lane (LIVE - 2 bids so far)
INSERT INTO auction_bids (id, auction_id, lane_id, vendor_id, vendor_name, amount, bid_rank, is_current, placed_at) VALUES
('e0000000-0000-0000-0000-000000000012', 'c0000000-0000-0000-0000-000000000004', 'd0000000-0000-0000-0000-000000000005',
 'a1000000-0000-0000-0000-000000000001', 'SwiftHaul Logistics', 34500, 1, true, NOW() - INTERVAL '20 minutes'),
('e0000000-0000-0000-0000-000000000013', 'c0000000-0000-0000-0000-000000000004', 'd0000000-0000-0000-0000-000000000005',
 'a1000000-0000-0000-0000-000000000002', 'BlueAxle Freight',    35200, 2, true, NOW() - INTERVAL '25 minutes');

-- Auction events
INSERT INTO auction_events (auction_id, event_type, message, actor) VALUES
('c0000000-0000-0000-0000-000000000001', 'CREATED',   'Auction created', 'Arjun Mehta'),
('c0000000-0000-0000-0000-000000000001', 'LAUNCHED',  'Auction launched', 'Arjun Mehta'),
('c0000000-0000-0000-0000-000000000001', 'EXTENDED',  'Timer extended - bid received in final window', 'system'),
('c0000000-0000-0000-0000-000000000001', 'COMPLETED', 'Auction completed - bidding window closed', 'system'),
('c0000000-0000-0000-0000-000000000002', 'CREATED',   'Auction created', 'Naina Kapoor'),
('c0000000-0000-0000-0000-000000000002', 'LAUNCHED',  'Auction launched', 'Naina Kapoor'),
('c0000000-0000-0000-0000-000000000002', 'COMPLETED', 'Auction completed', 'system'),
('c0000000-0000-0000-0000-000000000003', 'CREATED',   'Auction created', 'Arjun Mehta'),
('c0000000-0000-0000-0000-000000000003', 'LAUNCHED',  'Auction launched', 'Arjun Mehta'),
('c0000000-0000-0000-0000-000000000003', 'COMPLETED', 'Auction completed', 'system'),
('c0000000-0000-0000-0000-000000000004', 'CREATED',   'Auction created', 'Naina Kapoor'),
('c0000000-0000-0000-0000-000000000004', 'LAUNCHED',  'Auction launched', 'Naina Kapoor'),
('c0000000-0000-0000-0000-000000000005', 'CREATED',   'Auction draft saved', 'Samar Verma');

-- Contracts (2 ACTIVE, 2 EXPIRING_SOON)
INSERT INTO contracts (id, source_auction_id, contract_type, vendor_id, vendor_name, lane, vehicle_type,
    contracted_rate, rate_unit, volume_allocation_percent, allocation_rank,
    start_date, end_date, estimated_trips, status, l1_override_reason) VALUES
('f0000000-0000-0000-0000-000000000001', NULL, 'BULK',
 'a1000000-0000-0000-0000-000000000001', 'SwiftHaul Logistics',
 'Mumbai → Delhi', '20 MT Open Body', 46500, 'PER_TRIP', 60, 'L1',
 '2026-01-01', '2026-12-31', 800, 'ACTIVE', NULL),
('f0000000-0000-0000-0000-000000000002', NULL, 'LOT',
 'a1000000-0000-0000-0000-000000000002', 'BlueAxle Freight',
 'Pune → Bangalore', '32 FT Closed Body', 72000, 'PER_TRIP', 100, 'L1',
 '2026-02-01', '2026-09-30', 300, 'ACTIVE', NULL),
('f0000000-0000-0000-0000-000000000003', NULL, 'BULK',
 'a1000000-0000-0000-0000-000000000003', 'RoadBridge Carriers',
 'Chennai → Hyderabad', '20 MT Open Body', 38000, 'PER_TRIP', 100, 'L1',
 '2025-07-01', (CURRENT_DATE + INTERVAL '20 days')::date, 500, 'EXPIRING_SOON', NULL),
('f0000000-0000-0000-0000-000000000004', NULL, 'LOT',
 'a1000000-0000-0000-0000-000000000004', 'MetroFleet Movers',
 'Delhi → Chandigarh', '32 FT Closed Body', 28000, 'PER_TRIP', 70, 'L1',
 '2025-08-01', (CURRENT_DATE + INTERVAL '15 days')::date, 200, 'EXPIRING_SOON', NULL);

-- RFI
INSERT INTO rfis (id, title, description, deadline, status, message_to_vendor, template_file_name, created_by_id) VALUES
('91000000-0000-0000-0000-000000000001',
 'Q3 Pan-India Fleet Discovery',
 'Looking for vendors with 32ft closed body capacity across South India routes.',
 NOW() + INTERVAL '5 days',
 'PUBLISHED',
 'Dear Vendor, Please fill out the attached matrix with your fleet capacity, current utilisation, and base locations. Deadline for submission is 5 days from now.',
 'RFI_Template_v2.xlsx',
 '71234567-0000-0000-0000-000000000002');

INSERT INTO rfi_target_emails (rfi_id, email) VALUES
('91000000-0000-0000-0000-000000000001', 'vendor1@example.com'),
('91000000-0000-0000-0000-000000000001', 'transport2@example.com'),
('91000000-0000-0000-0000-000000000001', 'logistics3@example.com');

INSERT INTO rfi_vendor_tracking (rfi_id, vendor_id_or_email, status) VALUES
('91000000-0000-0000-0000-000000000001', 'vendor1@example.com',    'RESPONDED'),
('91000000-0000-0000-0000-000000000001', 'transport2@example.com', 'PENDING'),
('91000000-0000-0000-0000-000000000001', 'logistics3@example.com', 'PENDING');

-- RFQ
INSERT INTO rfqs (id, title, deadline, status, message_to_vendor, template_file_name, created_by_id) VALUES
('92000000-0000-0000-0000-000000000001',
 'Dedicated Capacity for Q3 South India',
 NOW() + INTERVAL '3 days',
 'PUBLISHED',
 'Dear Vendor, Please quote your best per-trip rates for the lanes listed in the attached template. Include breakup for fuel, toll, and driver allowance.',
 'RFQ_Lane_Pricing_Q3.xlsx',
 '71234567-0000-0000-0000-000000000002');

INSERT INTO rfq_target_emails (rfq_id, email) VALUES
('92000000-0000-0000-0000-000000000001', 'vendor1@example.com'),
('92000000-0000-0000-0000-000000000001', 'vendor2@example.com'),
('92000000-0000-0000-0000-000000000001', 'external_vendor@example.com');

INSERT INTO rfq_vendor_tracking (rfq_id, vendor_id_or_email, status) VALUES
('92000000-0000-0000-0000-000000000001', 'vendor1@example.com',          'PENDING'),
('92000000-0000-0000-0000-000000000001', 'vendor2@example.com',          'RESPONDED'),
('92000000-0000-0000-0000-000000000001', 'external_vendor@example.com',  'PENDING');

-- RFQ Response
INSERT INTO rfq_responses (id, rfq_id, vendor_name, file_name, uploaded_by, uploaded_at) VALUES
('93000000-0000-0000-0000-000000000001', '92000000-0000-0000-0000-000000000001',
 'Fast Logistics', 'FastLogistics_Q3_Response.xlsx', 'Naina Kapoor', NOW() - INTERVAL '1 day');

INSERT INTO rfq_response_rows (response_id, lane, vehicle_type, price) VALUES
('93000000-0000-0000-0000-000000000001', 'Mumbai → Pune',   '20ft Container', 8500),
('93000000-0000-0000-0000-000000000001', 'Mumbai → Nashik', '20ft Container', 12000),
('93000000-0000-0000-0000-000000000001', 'Pune → Nagpur',   '32ft SXL',       18500);

-- Notifications (for user 71234567-0000-0000-0000-000000000001)
INSERT INTO notifications (user_id, category, title, message, is_read, deep_link) VALUES
('71234567-0000-0000-0000-000000000001', 'AWARDS',    'Award deadline approaching',    'Auction "Spot | Mumbai → Delhi" award window closes in 1 hour.',           false, '/auction/auctions/c0000000-0000-0000-0000-000000000001'),
('71234567-0000-0000-0000-000000000001', 'AUCTIONS',  'Auction completed',             'Spot | BK-2026-11203 | Mumbai → Delhi has ended with 3 valid bids.',       false, '/auction/auctions/c0000000-0000-0000-0000-000000000001'),
('71234567-0000-0000-0000-000000000001', 'AWARDS',    'Pending award: Bulk auction',   'Bulk | South Corridor Contract Rates awaits award selection.',               false, '/auction/auctions/c0000000-0000-0000-0000-000000000002'),
('71234567-0000-0000-0000-000000000001', 'CONTRACTS', 'Contract expiring soon',        'Contract with RoadBridge Carriers (Chennai → Hyderabad) expires in 20 days.', true, '/auction/contracts/f0000000-0000-0000-0000-000000000003'),
('71234567-0000-0000-0000-000000000001', 'CONTRACTS', 'Contract expiring soon',        'Contract with MetroFleet Movers (Delhi → Chandigarh) expires in 15 days.',    true, '/auction/contracts/f0000000-0000-0000-0000-000000000004'),
('71234567-0000-0000-0000-000000000001', 'SOURCING',  'New RFI published',             'Q3 Pan-India Fleet Discovery has been sent to 3 vendors.',                    true, '/auction/sourcing/rfi/91000000-0000-0000-0000-000000000001'),
('71234567-0000-0000-0000-000000000001', 'SOURCING',  'New RFQ published',             'Dedicated Capacity for Q3 South India has been sent to 3 vendors.',          true, '/auction/sourcing/rfq/92000000-0000-0000-0000-000000000001'),
('71234567-0000-0000-0000-000000000001', 'AUCTIONS',  'Auction launched',              'Spot | Pune → Jaipur is now live and accepting bids.',                        true, '/auction/auctions/c0000000-0000-0000-0000-000000000004');
