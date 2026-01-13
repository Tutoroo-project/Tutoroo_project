import { create } from "zustand";

const useModalStore = create((set) => ({
  isLoginOpen: false,
  isSignUpOpen: false,

  openLogin: () => set({ isLoginOpen: true, isSignUpOpen: false }),
  closeLogin: () => set({ isLoginOpen: false }),

  openSignUp: () => set({ isSignUpOpen: true, isLoginOpen: false }),
  closeSignUp: () => set({ isSignUpOpen: false }),

  closeAll: () => set({ isLoginOpen: false, isSignUpOpen: false }),
}));

export default useModalStore;
