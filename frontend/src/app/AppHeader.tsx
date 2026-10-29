import { Link } from 'react-router';

type ActivePage = 'today' | 'tasks';

export function AppHeader({ active }: { active: ActivePage }) {
  return (
    <header className="app-header">
      <Link className="brand-lockup" to="/" aria-label="JodNgan home">
        <span className="brand-mark" aria-hidden="true">J</span>
        <span className="brand-copy">
          <strong>JodNgan</strong>
          <span>Student workload planner</span>
        </span>
      </Link>

      <nav className="app-nav" aria-label="Primary navigation">
        <Link to="/" aria-current={active === 'today' ? 'page' : undefined}>
          <span aria-hidden="true">◉</span>
          Today
        </Link>
        <Link to="/tasks" aria-current={active === 'tasks' ? 'page' : undefined}>
          <span aria-hidden="true">✓</span>
          Tasks
        </Link>
      </nav>
    </header>
  );
}
