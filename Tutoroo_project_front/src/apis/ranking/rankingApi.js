import { api } from "../configs/axiosConfig";

export const rankingApi = {
  getRankings: async (gender, age) => {
    const params = {};
    
    // "전체"가 아닐 때만 파라미터 포함
    if (gender && gender !== "전체") {
        params.gender = gender;
    }
    
    // 백엔드는 Integer 타입을 기대하므로 변환
    if (age && age !== "전체") {
        params.ageGroup = parseInt(age, 10); 
    }

    const response = await api.get("/api/ranking/list", { params });
    // [중요] 여기서는 response.data (객체)를 그대로 반환하고
    // RankingPage.jsx 에서 .allRankers 를 꺼내는 방식이 구조상 명확합니다.
    return response.data;
  },

  getMyRanking: async () => {
    // RankingController에 없는 API라면 UserController의 대시보드를 활용
    const response = await api.get("/api/user/dashboard");
    return response.data; 
  }
};