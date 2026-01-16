import axios from "axios";

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL, //.env 값 가져오기
});

api.interceptors.request.use((config) => {
  const accessToken = localStorage.getItem("accessToken");
  if (accessToken) {
    config.headers = config.headers ?? {};
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

api.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config;

    // accessToken 만료 등으로 401 발생 + 무한루프 방지
    if (error?.response?.status === 401 && !original?._retry) {
      original._retry = true;

      const refreshToken = localStorage.getItem("refreshToken");
      if (!refreshToken) return Promise.reject(error);

      try {
        //  너희 백엔드: RefreshToken 헤더로 재발급
        const reissueRes = await api.post("/api/auth/reissue", null, {
          headers: { RefreshToken: refreshToken },
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
