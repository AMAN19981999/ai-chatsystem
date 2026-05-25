import { FormEvent, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { authService } from '../services/authService';
import { useAuthStore } from '../store/authStore';

export const LoginPage = () => {
  const navigate = useNavigate();
  const setSession = useAuthStore((state) => state.setSession);
  const [email, setEmail] = useState('demo@aichat.local');
  const [password, setPassword] = useState('password');
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setIsLoading(true);

    try {
      const response = await authService.login({ email, password });
      setSession(response.token, response.user);
      navigate('/chat');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <main className="grid min-h-screen place-items-center bg-surface px-4">
      <form onSubmit={handleSubmit} className="w-full max-w-sm rounded-lg bg-white p-6 shadow-panel">
        <h1 className="text-xl font-semibold text-ink">Sign in</h1>
        <div className="mt-6 space-y-4">
          <label className="block text-sm font-medium">
            Email
            <input
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              className="mt-1 h-10 w-full rounded-md border border-stone-300 px-3 outline-none focus:border-accent"
              type="email"
            />
          </label>
          <label className="block text-sm font-medium">
            Password
            <input
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              className="mt-1 h-10 w-full rounded-md border border-stone-300 px-3 outline-none focus:border-accent"
              type="password"
            />
          </label>
          <button
            type="submit"
            className="h-10 w-full rounded-md bg-accent px-4 text-sm font-semibold text-white hover:bg-teal-800"
            disabled={isLoading}
          >
            {isLoading ? 'Signing in...' : 'Sign in'}
          </button>
        </div>
      </form>
    </main>
  );
};
