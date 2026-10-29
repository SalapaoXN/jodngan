import { Navigate, Route, Routes } from 'react-router';

import { TaskDetailPage } from '../features/tasks/TaskDetailPage';
import { TaskListPage } from '../features/tasks/TaskListPage';
import { TodayPage } from '../features/tasks/TodayPage';

export function App() {
  return (
    <Routes>
      <Route path="/" element={<TodayPage />} />
      <Route path="/tasks" element={<TaskListPage />} />
      <Route path="/tasks/:taskId" element={<TaskDetailPage />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
