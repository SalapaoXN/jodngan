import { Navigate, Route, Routes } from 'react-router';

import { TodayPage } from '../features/tasks/TodayPage';

export function App() {
  return (
    <Routes>
      <Route path="/" element={<TodayPage />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
