import { api } from "../configs/axiosConfig";

export const studyApi = {
  // 내 학습 상태 조회
  getStudyStatus: async () => {
    const response = await api.get("/api/study/status");
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
    const response = await api.post("/api/study/chat/simple", { message }); // [Backend URL 수정됨]
    return response.data;
  },

  // 학습 목록 조회
  getStudyList: async () => {
    const response = await api.get("/api/study/list");
    return response.data;
  },

  // 수업 시작하기
  startClass: async ({ planId, dayCount, personaName, dailyMood }) => {
    const response = await api.post("/api/tutor/class/start", {
      planId,
      dayCount,
      personaName,
      dailyMood,   
    });
    return response.data;
  },

  // [New] 음성 인식 (STT) - 오디오 파일 전송
  uploadAudio: async (audioBlob) => {
    const formData = new FormData();
    // 파일명은 mp3 확장자로 지정
    formData.append("audio", audioBlob, "speech.mp3");
    
    // Content-Type은 axios가 자동으로 multipart/form-data로 설정함
    const response = await api.post("/api/tutor/stt", formData);
    return response.data; // 변환된 텍스트 반환
  },

  // [New] 복습 자료 PDF 다운로드
  downloadReviewPdf: async (planId, dayCount) => {
    const response = await api.get(`/api/study/review/download`, {
        params: { planId, dayCount },
        responseType: 'blob', // 파일 다운로드를 위해 blob 타입 지정
    });
    return response.data;
  }
};