import React, { createContext, useContext, useState, useEffect } from 'react';
import api from '../services/api';

interface User {
  id: string;
  username: string;
  email: string;
  role: string;
}

interface AuthContextType {
  user: User | null;
  loading: boolean;
  login: (username: string, password: string) => Promise<void>;
  signup: (username: string, email: string, password: string, role: string, organizationName?: string) => Promise<void>;
  logout: () => void;
  isAdmin: boolean;
  isDeveloper: boolean;
  isViewer: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const storedUser = localStorage.getItem('user');
    if (storedUser) {
      setUser(JSON.parse(storedUser));
    }
    setLoading(false);
  }, []);

  const login = async (username: string, password: string) => {
    const res = await api.post('/api/auth/signin', { username, password });
    const { token, refreshToken, id, email, role } = res.data;
    
    const loggedUser = { id, username, email, role };
    localStorage.setItem('accessToken', token);
    localStorage.setItem('refreshToken', refreshToken);
    localStorage.setItem('user', JSON.stringify(loggedUser));
    setUser(loggedUser);
  };

  const signup = async (username: string, email: string, password: string, role: string, organizationName?: string) => {
    await api.post('/api/auth/signup', { username, email, password, role, organizationName });
  };

  const logout = () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    setUser(null);
  };

  const isAdmin = user?.role === 'ADMIN';
  const isDeveloper = user?.role === 'DEVELOPER';
  const isViewer = user?.role === 'VIEWER';

  return (
    <AuthContext.Provider value={{ user, loading, login, signup, logout, isAdmin, isDeveloper, isViewer }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
