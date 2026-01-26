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

  // 메시지 전송 (채팅) - [수정] needsTts 지원을 위해 TutorController 엔드포인트 사용
  sendChatMessage: async ({ planId, message, needsTts }) => {
    // 기존 /api/study/chat/simple 은 needsTts를 받지 못하므로
    // DTO 구조가 변경된 /api/tutor/feedback/chat 을 사용합니다.
    const response = await api.post("/api/tutor/feedback/chat", { 
      planId, 
      message, 
      needsTts 
    });
    return response.data;
  },

  // 학습 목록 조회
  getStudyList: async () => {
    const response = await api.get("/api/study/list");
    return response.data;
  },

  // 수업 시작하기 - [수정] needsTts 파라미터 추가
  startClass: async ({ planId, dayCount, personaName, dailyMood, customOption, needsTts }) => {
    const response = await api.post("/api/tutor/class/start", {
      planId,
      dayCount,
      personaName,
      dailyMood,
      customOption, // 커스텀 요구사항
      needsTts,     // [추가] TTS 생성 여부 (true/false)
    });
    return response.data;
  },

  // 음성 인식 (STT) - 오디오 파일 전송
  uploadAudio: async (audioBlob) => {
    const formData = new FormData();
    formData.append("audio", audioBlob, "speech.mp3");
    
    const response = await api.post("/api/tutor/stt", formData);
    return response.data; 
  },

  // 복습 자료 PDF 다운로드
  downloadReviewPdf: async (planId, dayCount) => {
    const response = await api.get(`/api/study/review/download`, {
        params: { planId, dayCount },
        responseType: 'blob', 
    });
    return response.data;
  }
};