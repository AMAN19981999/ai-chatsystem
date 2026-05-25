export type LoginRequest = {
  email: string;
  password: string;
};

export type AuthUser = {
  id: string;
  email: string;
  displayName: string;
};

export type LoginResponse = {
  token: string;
  user: AuthUser;
};
