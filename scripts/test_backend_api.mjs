import crypto from 'node:crypto';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
const { request } = require('playwright');

const BASE_URL = 'http://127.0.0.1:8080/api/v1/';
const TEST_DATE = '2027-01-15';

function id(prefix) {
  return `${prefix}-${crypto.randomUUID()}`;
}

async function expectJson(response, expected, label) {
  const body = await response.text();
  let parsed;
  try {
    parsed = body ? JSON.parse(body) : null;
  } catch {
    parsed = body;
  }
  if (response.status() !== expected) {
    throw new Error(`${label}: expected HTTP ${expected}, got ${response.status()}: ${body}`);
  }
  return parsed;
}

const checks = [];
const context = await request.newContext({ baseURL: BASE_URL });
const student = { 'X-User-Id': 'student-demo' };
const teacher = { 'X-User-Id': 'teacher-demo' };
const manager = { 'X-User-Id': 'manager-demo' };
const createdReservationIds = [];

try {
  const roomsResponse = await context.get('rooms', { headers: student });
  const rooms = await expectJson(roomsResponse, 200, 'list rooms');
  if (rooms.length !== 5) throw new Error(`expected 5 rooms, got ${rooms.length}`);
  if (new Set(rooms.map((item) => item.roomNo)).size !== 5) throw new Error('room numbers are not unique');
  checks.push('room list returns five seeded rooms');

  const schedule = await expectJson(
    await context.get('rooms/7081/schedule', { params: { date: TEST_DATE }, headers: student }),
    200,
    'empty schedule',
  );
  if (!Array.isArray(schedule)) throw new Error('schedule response is not an array');
  checks.push('schedule query succeeds');

  const studentRequestId = id('pw-student');
  const studentPayload = {
    roomNo: '7081', date: TEST_DATE, start: '09:00', end: '10:00',
    reason: 'Playwright接口验证', attendeeCount: 4, phone: '13800000001', note: '', requestId: studentRequestId,
  };
  const created = await expectJson(await context.post('reservations', { data: studentPayload, headers: student }), 201, 'student reservation');
  if (created.status !== 'PENDING') throw new Error(`expected PENDING, got ${created.status}`);
  const reservationId = created.id;
  createdReservationIds.push(reservationId);
  checks.push('student reservation creates PENDING');

  const retried = await expectJson(await context.post('reservations', { data: studentPayload, headers: student }), 201, 'idempotent retry');
  if (retried.id !== reservationId) throw new Error('same requestId did not return original reservation');
  checks.push('same requestId returns the original reservation');

  const teacherPayload = { ...studentPayload, phone: '13800000002', requestId: id('pw-teacher-conflict') };
  const conflict = await expectJson(await context.post('reservations', { data: teacherPayload, headers: teacher }), 409, 'teacher conflict');
  if (conflict.code !== 'TIME_CONFLICT') throw new Error(`expected TIME_CONFLICT, got ${conflict.code}`);
  checks.push('overlapping teacher reservation is rejected');

  const publicSchedule = await expectJson(
    await context.get('rooms/7081/schedule', { params: { date: TEST_DATE }, headers: teacher }),
    200,
    'public schedule privacy',
  );
  const ownEntries = publicSchedule.filter((entry) => entry.own);
  const otherEntries = publicSchedule.filter((entry) => !entry.own && entry.status === 'PENDING');
  if (ownEntries.length || !otherEntries.length || otherEntries[0].reservationId !== null) {
    throw new Error('public schedule did not hide another user reservation id');
  }
  checks.push('public schedule hides another user reservation id');

  const pending = await expectJson(await context.get('admin/reservations', { params: { status: 'PENDING' }, headers: manager }), 200, 'manager pending ledger');
  if (!pending.some((item) => item.id === reservationId)) throw new Error('manager pending ledger omitted reservation');
  checks.push('manager can query pending ledger');

  const approved = await expectJson(await context.post(`admin/reservations/${reservationId}/approve`, { headers: manager }), 200, 'approve reservation');
  if (approved.status !== 'CONFIRMED') throw new Error(`expected CONFIRMED, got ${approved.status}`);
  checks.push('manager approval changes reservation to CONFIRMED');

  const cancelled = await expectJson(await context.post(`reservations/${reservationId}/cancel`, { data: { reason: 'Playwright清理' }, headers: student }), 200, 'cancel reservation');
  if (cancelled.status !== 'CANCELLED') throw new Error(`expected CANCELLED, got ${cancelled.status}`);
  checks.push('owner can cancel before start');

  const teacherCreated = await expectJson(await context.post('reservations', {
    data: { roomNo: '7091', date: TEST_DATE, start: '11:00', end: '12:00', reason: '教师接口验证', attendeeCount: 2, phone: '13800000002', note: '', requestId: id('pw-teacher') },
    headers: teacher,
  }), 201, 'teacher reservation');
  if (teacherCreated.status !== 'CONFIRMED') throw new Error(`expected teacher CONFIRMED, got ${teacherCreated.status}`);
  createdReservationIds.push(teacherCreated.id);
  const teacherCancelled = await expectJson(await context.post(`reservations/${teacherCreated.id}/cancel`, { data: { reason: 'Playwright清理' }, headers: teacher }), 200, 'teacher cancel');
  if (teacherCancelled.status !== 'CANCELLED') throw new Error(`expected teacher CANCELLED, got ${teacherCancelled.status}`);
  checks.push('teacher reservation creates CONFIRMED and can be cancelled');

  const notifications = await expectJson(await context.get('notifications', { headers: student }), 200, 'notifications');
  if (!Array.isArray(notifications)) throw new Error('notification response is not an array');
  checks.push('notification list succeeds');

  console.log('PASS');
  for (const check of checks) console.log(`- ${check}`);
} finally {
  await context.dispose();
}
