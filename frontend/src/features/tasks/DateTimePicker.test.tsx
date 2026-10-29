import { cleanup, fireEvent, render, screen, within } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { DateTimePicker } from './DateTimePicker';

afterEach(() => {
  cleanup();
  vi.useRealTimers();
});

describe('DateTimePicker', () => {
  it('sets today and the next quarter-hour when Today is pressed without a time', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date(2026, 9, 29, 12, 7, 0));
    const onChange = vi.fn();

    render(
      <DateTimePicker
        id="due-at"
        value=""
        onChange={onChange}
      />,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Choose due date and time' }));
    const picker = screen.getByRole('group', { name: 'Due date and time picker' });
    fireEvent.click(within(picker).getByRole('button', { name: 'Today' }));

    expect(within(picker).getByLabelText('Hour')).toHaveValue('12');
    expect(within(picker).getByLabelText('Minute')).toHaveValue('15');
    expect(onChange).toHaveBeenLastCalledWith('2026-10-29T12:15');
  });

  it('preserves an existing valid time when Today changes the date', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date(2026, 9, 29, 9, 5, 0));
    const onChange = vi.fn();

    render(
      <DateTimePicker
        id="due-at"
        value="2026-11-10T18:30"
        onChange={onChange}
      />,
    );

    fireEvent.click(screen.getByRole('button', { name: /18:30/ }));
    const picker = screen.getByRole('group', { name: 'Due date and time picker' });
    fireEvent.click(within(picker).getByRole('button', { name: 'Today' }));

    expect(within(picker).getByLabelText('Hour')).toHaveValue('18');
    expect(within(picker).getByLabelText('Minute')).toHaveValue('30');
    expect(onChange).toHaveBeenLastCalledWith('2026-10-29T18:30');
  });
});
