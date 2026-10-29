import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate } from 'react-router';

import { logout } from '../features/auth/api';
import { currentUserQueryKey, useCurrentUser } from '../features/auth/queries';

type ActivePage = 'today' | 'tasks';

export function AppHeader({ active }: { active: ActivePage }) {
  const currentUserQuery = useCurrentUser();
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const logoutMutation = useMutation({
    mutationFn: logout,
    onSuccess: () => {
      queryClient.setQueryData(currentUserQueryKey, null);
      queryClient.removeQueries({ queryKey: ['tasks'] });
      queryClient.removeQueries({ queryKey: ['courses'] });
      queryClient.removeQueries({ queryKey: ['today'] });
      navigate('/login', { replace: true });
    },
  });

  return (
    <header className="app-header">
      <Link className="brand-lockup" to="/" aria-label="JodNgan home">
        <span className="brand-mark" aria-hidden="true">J</span>
        <span className="brand-copy">
          <strong>JodNgan</strong>
          <span>Student workload planner</span>
        </span>
      </Link>

      <div className="app-header-actions">
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

        <div className="account-actions">
          {currentUserQuery.data && (
            <span className="account-email" title={currentUserQuery.data.email}>{currentUserQuery.data.email}</span>
          )}
          <button
            type="button"
            className="text-button account-logout"
            disabled={logoutMutation.isPending}
            onClick={() => logoutMutation.mutate()}
          >
            {logoutMutation.isPending ? 'Signing out…' : 'Log out'}
          </button>
        </div>
      </div>
    </header>
  );
}
