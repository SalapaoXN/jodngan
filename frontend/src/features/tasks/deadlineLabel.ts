export function formatRemainingTime(remainingSeconds: number, overdue: boolean): string {
  if (!overdue && remainingSeconds <= 0) {
    return 'ถึงกำหนดแล้ว';
  }

  const seconds = Math.abs(remainingSeconds);
  const days = Math.floor(seconds / 86_400);
  const hours = Math.floor(seconds / 3_600);
  const minutes = Math.max(1, Math.ceil(seconds / 60));
  const duration = days > 0 ? `${days} วัน` : hours > 0 ? `${hours} ชั่วโมง` : `${minutes} นาที`;

  return overdue ? `เลยกำหนดแล้ว ${duration}` : `เหลือ ${duration}`;
}
