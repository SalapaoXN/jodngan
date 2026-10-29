import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { Link, Navigate, useNavigate } from 'react-router';

import { register as registerAccount } from './api';
import { currentUserQueryKey, useCurrentUser } from './queries';

interface RegisterValues {
  email: string;
  password: string;
}

function detectedTimeZone(): string {
  try {
    return Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';
  } catch {
    return 'UTC';
  }
}

export function RegisterPage() {
  const currentUserQuery = useCurrentUser();
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const timeZone = detectedTimeZone();
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<RegisterValues>({ defaultValues: { email: '', password: '' } });

  const registerMutation = useMutation({
    mutationFn: (values: RegisterValues) => registerAccount({ ...values, timeZone }),
    onSuccess: (user) => {
      queryClient.setQueryData(currentUserQueryKey, user);
      navigate('/', { replace: true });
    },
  });

  if (currentUserQuery.data) {
    return <Navigate to="/" replace />;
  }

  return (
    <main className="auth-shell">
      <section className="auth-card" aria-labelledby="register-heading">
        <Link className="auth-brand" to="/register" aria-label="JodNgan registration">
          <span className="brand-mark" aria-hidden="true">J</span>
          <span>
            <strong>JodNgan</strong>
            <small>Student workload planner</small>
          </span>
        </Link>

        <div className="auth-heading">
          <p className="section-kicker">Start planning</p>
          <h1 id="register-heading">Create your account</h1>
          <p>Your tasks and courses stay scoped to this account from now on.</p>
        </div>

        <form onSubmit={handleSubmit((values) => registerMutation.mutate(values))} noValidate>
          <div className="field-group">
            <label htmlFor="register-email">Email</label>
            <input
              id="register-email"
              type="email"
              autoComplete="email"
              maxLength={320}
              aria-invalid={Boolean(errors.email)}
              aria-describedby={errors.email ? 'register-email-error' : undefined}
              {...register('email', {
                required: 'Enter your email.',
                maxLength: { value: 320, message: 'Email must be 320 characters or fewer.' },
                pattern: { value: /^\S+@\S+\.\S+$/, message: 'Enter a valid email.' },
              })}
            />
            {errors.email && <p id="register-email-error" className="field-error" role="alert">{errors.email.message}</p>}
          </div>

          <div className="field-group">
            <label htmlFor="register-password">Password</label>
            <input
              id="register-password"
              type="password"
              autoComplete="new-password"
              aria-invalid={Boolean(errors.password)}
              aria-describedby={errors.password ? 'register-password-error' : 'register-password-help'}
              {...register('password', {
                required: 'Enter a password.',
                minLength: { value: 8, message: 'Use at least 8 characters.' },
              })}
            />
            <p id="register-password-help" className="field-help">At least 8 characters. No special composition rules.</p>
            {errors.password && <p id="register-password-error" className="field-error" role="alert">{errors.password.message}</p>}
          </div>

          <div className="auth-time-zone" aria-label="Detected time zone">
            <span>Time zone</span>
            <strong>{timeZone}</strong>
          </div>

          {registerMutation.isError && (
            <p className="form-error auth-form-error" role="alert">{registerMutation.error.message}</p>
          )}

          <button type="submit" className="primary-action auth-submit" disabled={registerMutation.isPending}>
            {registerMutation.isPending ? 'Creating account…' : 'Create account'}
          </button>
        </form>

        <p className="auth-switch-copy">
          Already have an account? <Link to="/login">Sign in</Link>
        </p>
      </section>
    </main>
  );
}
