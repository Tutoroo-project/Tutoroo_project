import axios from "axios";

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL, //.env 값 가져오기
});

const AUTH_FREE_PATHS = [
  "/api/auth/login",
  "/api/auth/join",
  "/api/auth/reissue",
  "/api/auth/email/send-verification",
  "/api/auth/email/verify",
  
];

// url이 auth-free 인지 검사
const isAuthFree = (url = "") => AUTH_FREE_PATHS.some((p) => url.includes(p));

api.interceptors.request.use((config) => {
  const url = config?.url ?? "";

  //  로그인/회원가입/재발급 같은 요청에는 토큰을 붙이지 않음
  if (isAuthFree(url)) return config;

  const accessToken = localStorage.getItem("accessToken");
  if (accessToken) {
    config.headers = config.headers ?? {};
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

api.interceptors.response.use(
  (res) => {
    // [임시 확인용] 서버가 JSON이 아니라 HTML(로그인창)을 줬는지 검사
    if (typeof res.data === 'string' && res.data.includes('<!DOCTYPE html>')) {
        console.error("토큰 문제로 로그인 페이지로 리다이렉트 되었습니다.");
        // 강제로 에러를 발생시켜서 catch 블록으로 보내거나 로그아웃 처리
        return Promise.reject(new Error("Token Invalid - Redirected to Login"));
    }
    return res;
  },
  async (error) => {
    const original = error.config;
    const url = original?.url ?? "";

    //  auth-free 요청(로그인/회원가입 등)은 reissue 로직 타지 않게
    if (isAuthFree(url)) {
      return Promise.reject(error);
    }

    // accessToken 만료 등으로 401 발생 + 무한루프 방지
    if (error?.response?.status === 401 && !original?._retry) {
      original._retry = true;

      const refreshToken = localStorage.getItem("refreshToken");
      if (!refreshToken) return Promise.reject(error);

      try {
        //  백엔드: RefreshToken 헤더로 재발급
        const reissueRes = await api.post("/api/auth/reissue", null, {
          headers: { RefreshToken: refreshToken},
        });

        const newAccessToken = reissueRes.data?.accessToken;
        if (!newAccessToken) return Promise.reject(error);

        localStorage.setItem("accessToken", newAccessToken);

        // 원래 요청에 새 토큰 붙여서 재요청
        original.headers = original.headers ?? {};
        original.headers.Authorization = `Bearer ${newAccessToken}`;
        return api(original);
      } catch (e) {
        // refresh도 실패하면 강제 로그아웃 처리 권장
        localStorage.removeItem("accessToken");
        localStorage.removeItem("refreshToken");
        return Promise.reject(error);
      }
    }

    return Promise.reject(error);
  }
);
