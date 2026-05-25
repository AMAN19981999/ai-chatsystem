import { LogOut, MessageSquare } from 'lucide-react';
import type { ReactNode } from 'react';
import { useAuthStore } from '../store/authStore';

type AppLayoutProps = {
  children: ReactNode;
};

export const AppLayout = ({ children }: AppLayoutProps) => {
  const logout = useAuthStore((state) => state.logout);

  return (
    <div className="min-h-screen bg-surface">
      <header className="flex h-14 items-center justify-between border-b border-stone-200 bg-white px-5">
        <div className="flex items-center gap-2 font-semibold text-ink">
          <MessageSquare className="h-5 w-5 text-accent" />
          <span>AI Chat Platform</span>
        </div>
        <button
          type="button"
          onClick={logout}
          className="inline-flex h-9 items-center gap-2 rounded-md border border-stone-300 px-3 text-sm font-medium hover:bg-stone-50"
        >
          <LogOut className="h-4 w-4" />
          Sign out
        </button>
      </header>
      {children}
    </div>
  );
};
