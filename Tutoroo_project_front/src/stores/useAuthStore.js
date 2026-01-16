import { create } from "zustand";

/**
 * ✅ AuthStore (Zustand)
 * - 로그인/로그아웃 상태 관리
 * - accessToken / refreshToken localStorage 저장
 * - user(최소 사용자 정보) localStorage 저장
 *
 */

const USER_KEY = "user"; // localStorage에 저장될 유저 정보 key

const useAuthStore = create((set) => ({
  /**
   * 초기 user 상태
   * - 새로고침해도 로그인 상태 유지하려고 localStorage에서 복원
   */
  user: (() => {
    try {
      return JSON.parse(localStorage.getItem(USER_KEY)) ?? null;
    } catch {
      return null;
    }
  })(),

  /**
   *  login(payload)
   * payload가 LoginResponse(= accessToken이 있음)이면:
   *  1) accessToken/refreshToken 저장
   *  2) user 최소 정보 추려서 저장
   *
   * payload가 더미 로그인(토큰 없음)이면:
   *  - 기존 호환용으로 user만 저장
   */
  login: (payload) => {
    //  실제 백엔드 로그인 응답(LoginResponse) 처리
    if (payload?.accessToken) {
      // 1) 토큰 저장 (axiosConfig 인터셉터가 여기서 accessToken을 읽어서 Authorization 붙임)
      localStorage.setItem("accessToken", payload.accessToken);
      if (payload.refreshToken) {
        localStorage.setItem("refreshToken", payload.refreshToken);
      }

      // 2) 프론트에서 필요한 최소 유저 정보만 저장
      // (원하면 여기 필드 더 추가해도 됨)
      const user = {
        username: payload.username,
        name: payload.name,
        role: payload.role,
        isNewUser: payload.isNewUser,
      };

      localStorage.setItem(USER_KEY, JSON.stringify(user));
      set({ user });
      return;
    }

    //  기존 코드 호환(더미 로그인 등)
    localStorage.setItem(USER_KEY, JSON.stringify(payload));
    set({ user: payload });
  },

  /**
   *  logout()
   * - 토큰/유저정보 제거하고 상태 초기화
   */
  logout: () => {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
    localStorage.removeItem(USER_KEY);
    set({ user: null });
  },

  /**
   *   setUser(partial)
   * - (선택) 마이페이지에서 이름/프로필 같은 user 값만 갱신할 때 유용
   * - 토큰은 건드리지 않음
   */
  setUser: (partial) => {
    set((state) => {
      const next = { ...(state.user ?? {}), ...(partial ?? {}) };
      localStorage.setItem(USER_KEY, JSON.stringify(next));
      return { user: next };
    });
  },

  /**
   *   isLoggedIn()
   * - (선택) 로그인 여부를 간단히 체크하고 싶을 때
   * - accessToken 존재 여부 기준
   */
  isLoggedIn: () => {
    return !!localStorage.getItem("accessToken");
  },
}));

export default useAuthStore;
