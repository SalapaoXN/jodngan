import { Navigate, Outlet, useLocation } from 'react-router';

import { useCurrentUser } from './queries';

export function AuthGuard() {
  const currentUserQuery = useCurrentUser();
  const location = useLocation();

  if (currentUserQuery.isPending) {
    return (
      <main className="auth-state-shell" aria-busy="true">
        <div className="auth-state-card" role="status">
          <span className="brand-mark" aria-hidden="true">J</span>
          <p>Checking your session…</p>
        </div>
      </main>
    );
  }

  if (currentUserQuery.isError) {
    return (
      <main className="auth-state-shell">
        <div className="auth-state-card error-state" role="alert">
          <h1>JodNgan could not check your session</h1>
          <p>{currentUserQuery.error.message}</p>
          <button type="button" className="secondary-button" onClick={() => void currentUserQuery.refetch()}>
            Try again
          </button>
        </div>
      </main>
    );
  }

  if (!currentUserQuery.data) {
    const returnTo = `${location.pathname}${location.search}`;
    return <Navigate to="/login" replace state={{ returnTo }} />;
  }

  return <Outlet />;
}
