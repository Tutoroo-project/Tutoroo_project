import { api } from "../configs/axiosConfig";

export const authApi = {
  login: async ({ username, password }) => {
    const res = await api.post("/api/auth/login", { username, password });
    return res.data;
  },

  reissue: async (refreshToken) => {
    const res = await api.post("/api/auth/reissue", null, {
      headers: { RefreshToken: refreshToken },
    });
    return res.data;
  },

  //  아이디 중복 확인 추가
  checkId: async (username) => {
    const res = await api.get("/api/auth/check-id", { params: { username } });
    return res.data; // boolean
  },

  //  회원가입 추가 (multipart)
  join: async ({ data, profileImage }) => {
    const formData = new FormData();

    formData.append(
      "data",
      new Blob([JSON.stringify(data)], { type: "application/json" })
    );

    if (profileImage) formData.append("profileImage", profileImage);
    const res = await api.post("/api/auth/join", formData);
    return res.data;
  },
};

export const userApi = {
  getDashboard: async () => {
    const res = await api.get("/api/user/dashboard");
    return res.data;
  },
};
