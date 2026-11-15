CREATE DATABASE IF NOT EXISTS room_booking CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS app_user (
  id BINARY(16) PRIMARY KEY,
  external_id VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(50) NOT NULL,
  employee_no VARCHAR(30) NOT NULL UNIQUE,
  member_type VARCHAR(16) NOT NULL,
  phone VARCHAR(20) NOT NULL,
  identity_status VARCHAR(16) NOT NULL,
  manager BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL
);
CREATE TABLE IF NOT EXISTS room (
  id BINARY(16) PRIMARY KEY,
  room_no VARCHAR(16) NOT NULL UNIQUE,
  location VARCHAR(100),
  capacity INT NULL,
  equipment VARCHAR(500),
  restriction_text VARCHAR(500),
  info_status VARCHAR(20) NOT NULL,
  open_time TIME NOT NULL,
  close_time TIME NOT NULL
);
CREATE TABLE IF NOT EXISTS identity_review (
  id BINARY(16) PRIMARY KEY,
  user_id BINARY(16) NOT NULL,
  name_snapshot VARCHAR(50) NOT NULL,
  employee_no_snapshot VARCHAR(30) NOT NULL,
  member_type_snapshot VARCHAR(16) NOT NULL,
  phone_snapshot VARCHAR(20) NOT NULL,
  status VARCHAR(16) NOT NULL,
  reason VARCHAR(200),
  reviewer_id BINARY(16),
  submitted_at DATETIME(6) NOT NULL,
  handled_at DATETIME(6),
  CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES app_user(id)
);
CREATE TABLE IF NOT EXISTS reservation (
  id BINARY(16) PRIMARY KEY,
  room_id BINARY(16) NOT NULL,
  applicant_id BINARY(16) NOT NULL,
  applicant_name VARCHAR(50) NOT NULL,
  applicant_phone VARCHAR(20) NOT NULL,
  reservation_date DATE NOT NULL,
  start_minute SMALLINT NOT NULL,
  end_minute SMALLINT NOT NULL,
  reason VARCHAR(200) NOT NULL,
  attendee_count INT NOT NULL,
  note VARCHAR(500),
  status VARCHAR(16) NOT NULL,
  request_id VARCHAR(64) NOT NULL,
  handled_by BINARY(16),
  handled_at DATETIME(6),
  result_reason VARCHAR(200),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_res_room FOREIGN KEY (room_id) REFERENCES room(id),
  CONSTRAINT fk_res_user FOREIGN KEY (applicant_id) REFERENCES app_user(id),
  CONSTRAINT uk_res_request UNIQUE (applicant_id, request_id),
  INDEX idx_res_overlap (room_id, reservation_date, start_minute, end_minute, status)
);
CREATE TABLE IF NOT EXISTS closure (
  id BINARY(16) PRIMARY KEY,
  room_id BINARY(16) NOT NULL,
  start_at DATETIME(6) NOT NULL,
  end_at DATETIME(6) NOT NULL,
  reason VARCHAR(200) NOT NULL,
  manager_id BINARY(16) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_closure_room FOREIGN KEY (room_id) REFERENCES room(id),
  CONSTRAINT fk_closure_manager FOREIGN KEY (manager_id) REFERENCES app_user(id)
);
CREATE TABLE IF NOT EXISTS notification (
  id BINARY(16) PRIMARY KEY,
  recipient_id BINARY(16) NOT NULL,
  type VARCHAR(32) NOT NULL,
  business_id BINARY(16),
  content VARCHAR(500) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  read_at DATETIME(6),
  CONSTRAINT fk_notice_user FOREIGN KEY (recipient_id) REFERENCES app_user(id)
);
CREATE TABLE IF NOT EXISTS operation_log (
  id BINARY(16) PRIMARY KEY,
  operator_id BINARY(16),
  object_type VARCHAR(32) NOT NULL,
  object_id BINARY(16),
  action VARCHAR(32) NOT NULL,
  result VARCHAR(16) NOT NULL,
  summary VARCHAR(1000),
  created_at DATETIME(6) NOT NULL
);

INSERT IGNORE INTO room (id, room_no, location, capacity, equipment, restriction_text, info_status, open_time, close_time)
VALUES
(UUID_TO_BIN(UUID()), '7060', '综合楼七楼', NULL, '待核实', '待核实', 'TO_VERIFY', '08:00:00', '22:00:00'),
(UUID_TO_BIN(UUID()), '7081', '综合楼七楼', 12, '待核实', '待核实', 'PARTIAL', '08:00:00', '22:00:00'),
(UUID_TO_BIN(UUID()), '7091', '综合楼七楼', 12, '待核实', '待核实', 'PARTIAL', '08:00:00', '22:00:00'),
(UUID_TO_BIN(UUID()), '7119', '综合楼七楼', 80, '待核实', '待核实', 'PARTIAL', '08:00:00', '22:00:00'),
(UUID_TO_BIN(UUID()), '7052', '综合楼七楼', 35, '待核实', '待核实', 'PARTIAL', '08:00:00', '22:00:00');

INSERT IGNORE INTO app_user (id, external_id, name, employee_no, member_type, phone, identity_status, manager, created_at, updated_at)
VALUES
(UUID_TO_BIN(UUID()), 'student-demo', '演示学生', 'S-DEMO', 'STUDENT', '13800000001', 'APPROVED', FALSE, NOW(6), NOW(6)),
(UUID_TO_BIN(UUID()), 'teacher-demo', '演示教师', 'T-DEMO', 'TEACHER', '13800000002', 'APPROVED', FALSE, NOW(6), NOW(6)),
(UUID_TO_BIN(UUID()), 'manager-demo', '演示负责人', 'M-DEMO', 'TEACHER', '13800000003', 'APPROVED', TRUE, NOW(6), NOW(6));
