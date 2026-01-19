import { api } from "../configs/axiosConfig";

export const levelTestApi = {
  // 첫 질문 받기
  start: async () => {
    const res = await api.post("/api/level-test/start");
    return res.data;
  },

  // 사용자 답변을 포함한 히스토리 보내고 다음 질문 받기
  next: async (history) => {
    const res = await api.post("/api/level-test/next", { history });
    return res.data;
  },
};