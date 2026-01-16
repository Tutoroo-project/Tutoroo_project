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

  // (메시지 전송)
  sendChatMessage: async (message) => {
    // 백엔드 엔드포인트: POST /api/study/chat
    // 보낼 데이터: { message: "유저가 쓴 말" }
    const response = await api.post("/api/study/chat", { message });
    return response.data; // { reply: "AI의 대답" } 이라고 가정
  }
};