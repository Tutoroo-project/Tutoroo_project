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

  completeSocialSignup: async ({ data, profileImage }) => {
    const formData = new FormData();

    formData.append(
      "data",
      new Blob([JSON.stringify(data)], { type:"application/json" })
    );

    if (profileImage) formData.append("profileImage", profileImage);

    const res = await api.post("/api/auth/oauth/complete", formData);
    return res.data;
  },

  //  아이디 중복 확인 추가
  checkId: async (username) => {
    const res = await api.get("/api/auth/check-id", { params: { username } });
    return res.data; // boolean
  },

  sendEmailVerification: async (email) => {
    const res = await api.post("/api/auth/email/send-verification", null, {
      params: { email },
    });
    return res.data; // "인증번호가 메일로 발송되었습니다."
  },

  verifyEmailCode: async (email, code) => {
    const res = await api.post("/api/auth/email/verify", null, {
      params: { email, code },
    });
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

  findId: async ({ name, email, phone }) => {
    const res = await api.post("/api/auth/find-id", { name, email, phone });
    return res.data;
  },

  findPassword: async ({ username, email }) => {
    const res = await api.post("/api/auth/find-password", { username, email });
    return res.data;
  },
};

export const userApi = {
  getDashboard: async () => {
    const res = await api.get("/api/user/dashboard");
    return res.data;
  },

  // --------------------------------------------
  // mypage api

  getProfile: async() => {
    const res = await api.get("/api/user/profile");
    return res.data;
  },

  updateProfile: async({data, profileImage}) => {
    const formData = new FormData();
    formData.append(
      "data",
      new Blob([JSON.stringify(data)], {type: "application/json"})
    );

    if (profileImage) {
      formData.append("image", profileImage);
    } else {
      formData.append("image", new Blob([], { type: "application/json" }), "");
    }
    
    const accessToken = localStorage.getItem("accessToken");

    
    const res = await api.patch("/api/user/update", formData, {
      headers: {
        Authorization: `Bearer ${accessToken}`, 
       
      },
    });
    return res.data;
  },

  withdraw: async(password, reason) => {
    const res = await api.post("/api/user/withdraw", { password, reason });
    return res.data;
  },

  verifyPassword: async (password) => {
    const res = await api.post("/api/user/verify-password", { password });
    return res.data;
  },

  changePassword: async ({ currentPassword, newPassword, confirmPassword}) => {
    const res = await api.patch("/api/user/change-password", {
      currentPassword,
      newPassword,
      confirmPassword
    });
    return res.data;
  }
};
