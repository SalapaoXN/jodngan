export interface User {
  id: string;
  email: string;
  timeZone: string;
}

export interface AuthResponse {
  user: User;
}

export interface LoginInput {
  email: string;
  password: string;
}

export interface RegisterInput extends LoginInput {
  timeZone: string;
}
