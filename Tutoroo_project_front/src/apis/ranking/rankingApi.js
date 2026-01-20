import { api } from "../configs/axiosConfig";

export const rankingApi = {
  // 1. 랭킹 리스트 조회
  getRankings: async (gender, age) => {
    const params = {};
    if (gender && gender !== "전체") params.gender = gender;
    if (age && age !== "전체") params.ageGroup = parseInt(age, 10);

    const response = await api.get("/api/ranking/list", { params });
    return response.data;
  },

  // 2. [추가] 내 프로필 정보 조회 (이미지, 이름)
  getMyProfile: async () => {
    const response = await api.get("/api/user/profile");
    return response.data;
  },

  // 3. [추가] 내 점수/랭킹 조회 (대시보드)
  getMyDashboard: async () => {
    const response = await api.get("/api/user/dashboard");
    return response.data;
  }
};