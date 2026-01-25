import { api } from "../configs/axiosConfig";

export const studyApi = {
  // 내 학습 상태 조회
  getStudyStatus: async (planId) => {
    const params = planId ? { planId } : {};
    const response = await api.get("/api/study/status", { params });
    return response.data; 
  },

  // 학습 플랜 생성
  createStudyPlan: async ({ planData }) => {
    const response = await api.post("/api/study/plans", planData);
    return response.data;
  },

  // 학습 로그 저장
  saveStudyLog: async (logData) => {
    const response = await api.post("/api/study/logs", logData);
    return response.data;
  },

  // 메시지 전송
  sendChatMessage: async (message) => {
    const response = await api.post("/api/study/chat/simple", { message });
    return response.data;
  },

  // 학습 목록 조회
  getStudyList: async () => {
    const response = await api.get("/api/study/list");
    return response.data;
  },

  // 수업 시작하기
  startClass: async ({ planId, dayCount, personaName, dailyMood, customOption }) => {
    const response = await api.post("/api/tutor/class/start", {
      planId,
      dayCount,
      personaName,
      dailyMood,
      customOption, // [New] 커스텀 요구사항 전송
    });
    return response.data;
  },

  //음성 인식 (STT) - 오디오 파일 전송
  uploadAudio: async (audioBlob) => {
    const formData = new FormData();
    formData.append("audio", audioBlob, "speech.mp3");
    
    const response = await api.post("/api/tutor/stt", formData);
    return response.data; 
  },

  downloadReviewPdf: async (planId, dayCount) => {
    const response = await api.get(`/api/study/review/download`, {
        params: { planId, dayCount },
        responseType: 'blob', 
    });
    return response.data;
  }
};