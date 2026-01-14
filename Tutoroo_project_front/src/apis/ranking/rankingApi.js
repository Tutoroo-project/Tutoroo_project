import { api } from "../configs/axiosConfig";

export const rankingApi = {
  //  전체 랭킹 리스트 조회 GET /api/ranking/list
  getRankings: async (gender, age) => {
    const params = {};
    if (gender && gender !== "전체") params.gender = gender;
    if (age && age !== "전체") params.age = age;
    const response = await api.get("/api/ranking/list", { params });
    return response.data;
  },

  // 내 랭킹 및 정보 조회 GET /api/ranking/me
  getMyRanking: async () => {
    const response = await api.get("/api/ranking/me");
    return response.data; 
  }
};