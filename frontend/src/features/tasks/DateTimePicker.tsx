import { useEffect, useMemo, useState } from 'react';

interface DateTimePickerProps {
  id: string;
  value: string;
  onChange: (value: string) => void;
  onBlur?: () => void;
  invalid?: boolean;
  describedBy?: string;
}

const WEEKDAYS = ['Su', 'Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa'];

function pad(value: number): string {
  return String(value).padStart(2, '0');
}

function datePartFromDate(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

function splitLocalDateTime(value: string): { datePart: string; timePart: string } {
  if (!value.includes('T')) {
    return { datePart: '', timePart: '' };
  }
  const [datePart, time = ''] = value.split('T');
  return { datePart, timePart: time.slice(0, 5) };
}

function dateFromPart(value: string): Date | null {
  const [year, month, day] = value.split('-').map(Number);
  if (!year || !month || !day) {
    return null;
  }
  return new Date(year, month - 1, day);
}

function monthLabel(date: Date): string {
  return new Intl.DateTimeFormat(undefined, { month: 'long', year: 'numeric' }).format(date);
}

function summaryLabel(value: string): string {
  if (!value) {
    return 'Choose due date and time';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return 'Choose due date and time';
  }
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date);
}

function validTimePart(value: string): boolean {
  const match = /^(\d{2}):(\d{2})$/.exec(value);
  if (!match) {
    return false;
  }
  return Number(match[1]) <= 23 && Number(match[2]) <= 59;
}

function nextQuarterHour(date: Date): string {
  const minutesSinceMidnight = date.getHours() * 60 + date.getMinutes();
  const roundedMinutes = Math.min(Math.ceil(minutesSinceMidnight / 15) * 15, 23 * 60 + 45);
  return `${pad(Math.floor(roundedMinutes / 60))}:${pad(roundedMinutes % 60)}`;
}

export function DateTimePicker({
  id,
  value,
  onChange,
  onBlur,
  invalid = false,
  describedBy,
}: DateTimePickerProps) {
  const initial = splitLocalDateTime(value);
  const [isOpen, setIsOpen] = useState(false);
  const [datePart, setDatePart] = useState(initial.datePart);
  const [timePart, setTimePart] = useState(initial.timePart);
  const [visibleMonth, setVisibleMonth] = useState(() => {
    const selected = dateFromPart(initial.datePart);
    const base = selected ?? new Date();
    return new Date(base.getFullYear(), base.getMonth(), 1);
  });

  useEffect(() => {
    const next = splitLocalDateTime(value);
    setDatePart(next.datePart);
    setTimePart(next.timePart);
    const selected = dateFromPart(next.datePart);
    if (selected) {
      setVisibleMonth(new Date(selected.getFullYear(), selected.getMonth(), 1));
    }
  }, [value]);

  const calendarDays = useMemo(() => {
    const year = visibleMonth.getFullYear();
    const month = visibleMonth.getMonth();
    const firstWeekday = new Date(year, month, 1).getDay();
    const start = new Date(year, month, 1 - firstWeekday);

    return Array.from({ length: 42 }, (_, index) => {
      const day = new Date(start.getFullYear(), start.getMonth(), start.getDate() + index);
      return {
        date: day,
        value: datePartFromDate(day),
        currentMonth: day.getMonth() === month,
      };
    });
  }, [visibleMonth]);

  function commit(nextDatePart: string, nextTimePart: string) {
    if (nextDatePart && nextTimePart) {
      onChange(`${nextDatePart}T${nextTimePart}`);
    } else {
      onChange('');
    }
  }

  function chooseDate(nextDate: Date) {
    const nextDatePart = datePartFromDate(nextDate);
    setDatePart(nextDatePart);
    setVisibleMonth(new Date(nextDate.getFullYear(), nextDate.getMonth(), 1));
    commit(nextDatePart, timePart);
  }

  function chooseToday() {
    const today = new Date();
    const nextDatePart = datePartFromDate(today);
    const nextTimePart = validTimePart(timePart) ? timePart : nextQuarterHour(today);
    setDatePart(nextDatePart);
    setTimePart(nextTimePart);
    setVisibleMonth(new Date(today.getFullYear(), today.getMonth(), 1));
    commit(nextDatePart, nextTimePart);
  }

  function changeTimePart(kind: 'hour' | 'minute', rawValue: string) {
    const digits = rawValue.replace(/\D/g, '').slice(0, 2);
    const [currentHour = '', currentMinute = ''] = timePart.split(':');
    const hour = kind === 'hour' ? digits : currentHour;
    const minute = kind === 'minute' ? digits : currentMinute;

    setTimePart(`${hour}:${minute}`);

    if (hour.length === 2 && minute.length === 2) {
      const hourNumber = Number(hour);
      const minuteNumber = Number(minute);
      if (hourNumber <= 23 && minuteNumber <= 59) {
        commit(datePart, `${hour}:${minute}`);
      }
    }
  }

  function normalizeTimePart(kind: 'hour' | 'minute') {
    const [hour = '', minute = ''] = timePart.split(':');
    const raw = kind === 'hour' ? hour : minute;
    if (!raw) {
      return;
    }

    const max = kind === 'hour' ? 23 : 59;
    const normalized = pad(Math.min(Number(raw), max));
    const nextHour = kind === 'hour' ? normalized : hour;
    const nextMinute = kind === 'minute' ? normalized : minute;
    const nextTimePart = `${nextHour}:${nextMinute}`;
    setTimePart(nextTimePart);

    if (nextHour.length === 2 && nextMinute.length === 2) {
      commit(datePart, nextTimePart);
    }
  }

  function clearValue() {
    setDatePart('');
    setTimePart('');
    onChange('');
  }

  const todayPart = datePartFromDate(new Date());

  return (
    <div className="date-time-picker">
      <button
        id={id}
        type="button"
        className={isOpen ? 'date-time-trigger open' : 'date-time-trigger'}
        aria-expanded={isOpen}
        aria-controls={`${id}-panel`}
        aria-invalid={invalid}
        aria-describedby={describedBy}
        onClick={() => setIsOpen((open) => !open)}
        onBlur={onBlur}
      >
        <span className="date-time-trigger-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="1.8">
            <rect x="3" y="5" width="18" height="16" rx="3" />
            <path d="M7 3v4M17 3v4M3 10h18" />
          </svg>
        </span>
        <span className={value ? 'date-time-trigger-value' : 'date-time-trigger-placeholder'}>
          {summaryLabel(value)}
        </span>
        <span className="date-time-trigger-chevron" aria-hidden="true">⌄</span>
      </button>

      {isOpen && (
        <div id={`${id}-panel`} className="date-time-panel" role="group" aria-label="Due date and time picker">
          <div className="calendar-toolbar">
            <button
              type="button"
              className="calendar-nav-button"
              aria-label="Previous month"
              onClick={() => setVisibleMonth((month) => new Date(month.getFullYear(), month.getMonth() - 1, 1))}
            >
              ←
            </button>
            <strong>{monthLabel(visibleMonth)}</strong>
            <button
              type="button"
              className="calendar-nav-button"
              aria-label="Next month"
              onClick={() => setVisibleMonth((month) => new Date(month.getFullYear(), month.getMonth() + 1, 1))}
            >
              →
            </button>
          </div>

          <div className="calendar-grid calendar-weekdays" aria-hidden="true">
            {WEEKDAYS.map((weekday) => <span key={weekday}>{weekday}</span>)}
          </div>

          <div className="calendar-grid" aria-label="Calendar">
            {calendarDays.map((day) => {
              const selected = day.value === datePart;
              const today = day.value === todayPart;
              return (
                <button
                  key={day.value}
                  type="button"
                  className={[
                    'calendar-day',
                    day.currentMonth ? '' : 'outside-month',
                    today ? 'today' : '',
                    selected ? 'selected' : '',
                  ].filter(Boolean).join(' ')}
                  aria-label={new Intl.DateTimeFormat(undefined, {
                    weekday: 'long',
                    year: 'numeric',
                    month: 'long',
                    day: 'numeric',
                  }).format(day.date)}
                  aria-pressed={selected}
                  onClick={() => chooseDate(day.date)}
                >
                  {day.date.getDate()}
                </button>
              );
            })}
          </div>

          <div className="date-time-footer">
            <div className="time-field">
              <span className="time-field-label">Time</span>
              <div className="time-inputs">
                <label>
                  <span>Hour</span>
                  <input
                    type="text"
                    inputMode="numeric"
                    autoComplete="off"
                    maxLength={2}
                    placeholder="HH"
                    value={timePart.split(':')[0] ?? ''}
                    onChange={(event) => changeTimePart('hour', event.target.value)}
                    onBlur={() => normalizeTimePart('hour')}
                  />
                </label>
                <span className="time-separator" aria-hidden="true">:</span>
                <label>
                  <span>Minute</span>
                  <input
                    type="text"
                    inputMode="numeric"
                    autoComplete="off"
                    maxLength={2}
                    placeholder="MM"
                    value={timePart.split(':')[1] ?? ''}
                    onChange={(event) => changeTimePart('minute', event.target.value)}
                    onBlur={() => normalizeTimePart('minute')}
                  />
                </label>
              </div>
            </div>
            <div className="date-time-actions">
              <button type="button" className="text-button" onClick={chooseToday}>Today</button>
              {(datePart || timePart) && (
                <button type="button" className="text-button muted" onClick={clearValue}>Clear</button>
              )}
              <button type="button" className="text-button done" onClick={() => setIsOpen(false)}>Done</button>
            </div>
          </div>

          <div className="date-time-selection" aria-live="polite">
            <span>{datePart ? new Intl.DateTimeFormat(undefined, { dateStyle: 'full' }).format(dateFromPart(datePart) ?? new Date()) : 'Choose a date'}</span>
            <span>{timePart || 'Choose a time'}</span>
          </div>
        </div>
      )}
    </div>
  );
}
