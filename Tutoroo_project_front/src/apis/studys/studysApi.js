import { api } from "../configs/axiosConfig";

export const studyApi = {
  //  내 학습 상태 조회
  getStudyStatus: async () => {
    const response = await api.get("/api/study/status");
    return response.data;
  },

  //  학습 플랜 생성
  createStudyPlan: async (planData) => {
    const response = await api.post("/api/study/plans", planData);
    return response.data;
  },

  //  학습 로그 저장
  saveStudyLog: async (logData) => {
    const response = await api.post("/api/study/logs", logData);
    return response.data;
  },

  // 메시지 전송 POST /api/study/chat
  sendChatMessage: async (message) => {
    const response = await api.post("/api/study/chat", { message });
    return response.data;
  },

  getStudyList: async () => {
    const response = await api.get("/api/study/list");
    return response.data;
  },
};
