import { create } from "zustand";

const useModalStore = create((set) => ({
  // =========================
  // 🔹 modal states
  // =========================
  isLoginOpen: false,
  isFindIdOpen: false,
  isFindPwOpen: false,
  isSignUpOpen: false,

  // =========================
  // 🔹 open actions
  // =========================
  openLogin: () =>
    set({
      isLoginOpen: true,
      isFindIdOpen: false,
      isFindPwOpen: false,
      isSignUpOpen: false,
    }),

  openFindId: () =>
    set({
      isLoginOpen: false,
      isFindIdOpen: true,
      isFindPwOpen: false,
      isSignUpOpen: false,
    }),

  openFindPw: () =>
    set({
      isLoginOpen: false,
      isFindIdOpen: false,
      isFindPwOpen: true,
      isSignUpOpen: false,
    }),

  openSignUp: () =>
    set({
      isLoginOpen: false,
      isFindIdOpen: false,
      isFindPwOpen: false,
      isSignUpOpen: true,
    }),

  // =========================
  // 🔹 close actions
  // =========================
  closeLogin: () => set({ isLoginOpen: false }),
  closeFindId: () => set({ isFindIdOpen: false }),
  closeFindPw: () => set({ isFindPwOpen: false }),
  closeSignUp: () => set({ isSignUpOpen: false }),

  closeAll: () =>
    set({
      isLoginOpen: false,
      isFindIdOpen: false,
      isFindPwOpen: false,
      isSignUpOpen: false,
    }),
}));

export default useModalStore;
