import { create } from "zustand";

const useAuthStore = create((set) => ({
  user: null, // { id, name, ... }

  login: (user) => set({ user }),
  logout: () => set({ user: null }),
}));

export default useAuthStore;
