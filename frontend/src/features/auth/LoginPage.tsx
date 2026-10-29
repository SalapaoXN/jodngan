import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { Link, Navigate, useLocation, useNavigate } from 'react-router';

import { login } from './api';
import { currentUserQueryKey, useCurrentUser } from './queries';

interface LoginValues {
  email: string;
  password: string;
}

interface LoginLocationState {
  returnTo?: string;
}

export function LoginPage() {
  const currentUserQuery = useCurrentUser();
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const location = useLocation();
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginValues>({ defaultValues: { email: '', password: '' } });

  const returnTo = (location.state as LoginLocationState | null)?.returnTo;
  const safeReturnTo = returnTo?.startsWith('/') && !returnTo.startsWith('//') ? returnTo : '/';

  const loginMutation = useMutation({
    mutationFn: login,
    onSuccess: (user) => {
      queryClient.setQueryData(currentUserQueryKey, user);
      navigate(safeReturnTo, { replace: true });
    },
  });

  if (currentUserQuery.data) {
    return <Navigate to={safeReturnTo} replace />;
  }

  return (
    <main className="auth-shell">
      <section className="auth-card" aria-labelledby="login-heading">
        <Link className="auth-brand" to="/login" aria-label="JodNgan sign in">
          <span className="brand-mark" aria-hidden="true">J</span>
          <span>
            <strong>JodNgan</strong>
            <small>Student workload planner</small>
          </span>
        </Link>

        <div className="auth-heading">
          <p className="section-kicker">Welcome back</p>
          <h1 id="login-heading">Sign in</h1>
          <p>Pick up where you left off and see what needs your attention next.</p>
        </div>

        <form onSubmit={handleSubmit((values) => loginMutation.mutate(values))} noValidate>
          <div className="field-group">
            <label htmlFor="login-email">Email</label>
            <input
              id="login-email"
              type="email"
              autoComplete="email"
              aria-invalid={Boolean(errors.email)}
              aria-describedby={errors.email ? 'login-email-error' : undefined}
              {...register('email', {
                required: 'Enter your email.',
                pattern: { value: /^\S+@\S+\.\S+$/, message: 'Enter a valid email.' },
              })}
            />
            {errors.email && <p id="login-email-error" className="field-error" role="alert">{errors.email.message}</p>}
          </div>

          <div className="field-group">
            <label htmlFor="login-password">Password</label>
            <input
              id="login-password"
              type="password"
              autoComplete="current-password"
              aria-invalid={Boolean(errors.password)}
              aria-describedby={errors.password ? 'login-password-error' : undefined}
              {...register('password', { required: 'Enter your password.' })}
            />
            {errors.password && <p id="login-password-error" className="field-error" role="alert">{errors.password.message}</p>}
          </div>

          {loginMutation.isError && (
            <p className="form-error auth-form-error" role="alert">{loginMutation.error.message}</p>
          )}

          <button type="submit" className="primary-action auth-submit" disabled={loginMutation.isPending}>
            {loginMutation.isPending ? 'Signing in…' : 'Sign in'}
          </button>
        </form>

        <p className="auth-switch-copy">
          New to JodNgan? <Link to="/register">Create an account</Link>
        </p>
      </section>
    </main>
  );
}
