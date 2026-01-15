import { create } from "zustand";

// Zustand를 사용해 로그인/회원가입/계정 찾기 모달의 전역 상태를 관리하고,
// 동시에 하나의 모달만 활성화되도록 제어

const useModalStore = create((set) => ({
  // 각 모달의 열림 여부를 boolean 값으로 관리
  isLoginOpen: false,
  isFindIdOpen: false,
  isFindPwOpen: false,
  isSignUpOpen: false,

  // 한개의 모달을 열때 나머지 모달 상태는 모두 false로 설정하여
  // "동시에 여러 모달이 열리는 상황"을 방지함

  // 로그인 모달 열기
  openLogin: () =>
    set({
      isLoginOpen: true,
      isFindIdOpen: false,
      isFindPwOpen: false,
      isSignUpOpen: false,
    }),

  // 아이디 찾기 모달 열기
  openFindId: () =>
    set({
      isLoginOpen: false,
      isFindIdOpen: true,
      isFindPwOpen: false,
      isSignUpOpen: false,
    }),

  // 비밀번호 찾기 모달 열기
  openFindPw: () =>
    set({
      isLoginOpen: false,
      isFindIdOpen: false,
      isFindPwOpen: true,
      isSignUpOpen: false,
    }),

  // 회원가입 모달 열기
  openSignUp: () =>
    set({
      isLoginOpen: false,
      isFindIdOpen: false,
      isFindPwOpen: false,
      isSignUpOpen: true,
    }),

  // 개별 모달 닫기용 액션
  // (모달 내부의 닫기 버튼, ESC, 외부 클릭 등에서 사용)
  closeLogin: () => set({ isLoginOpen: false }),
  closeFindId: () => set({ isFindIdOpen: false }),
  closeFindPw: () => set({ isFindPwOpen: false }),
  closeSignUp: () => set({ isSignUpOpen: false }),

  // 모든 모달 닫기
  // (라우트 이동, 로그아웃, 전역 초기화 시 사용)
  closeAll: () =>
    set({
      isLoginOpen: false,
      isFindIdOpen: false,
      isFindPwOpen: false,
      isSignUpOpen: false,
    }),
}));

export default useModalStore;
