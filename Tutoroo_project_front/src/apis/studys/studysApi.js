import { api } from "../configs/axiosConfig";

export const studyApi = {
  // 1. 내 학습 상태 조회 (대시보드/메인)
  getStudyStatus: async (planId) => {
    const params = planId ? { planId } : {};
    const response = await api.get("/api/study/status", { params });
    return response.data;
  },

  // 2. 새로운 학습 플랜 생성
  createStudyPlan: async ({ planData }) => {
    const response = await api.post("/api/study/plans", planData);
    return response.data;
  },

  // 3. 학습 플랜 삭제
  deleteStudyPlan: async (planId) => {
    const response = await api.delete(`/api/study/plans/${planId}`);
    return response.data;
  },

  // 4. 학습 로그 저장 (단순 저장용)
  saveStudyLog: async (logData) => {
    const response = await api.post("/api/study/logs", logData);
    return response.data;
  },

  // 5. 메시지 전송 (채팅) - 이미지 지원 추가
  sendChatMessage: async ({ planId, message, needsTts, imageFile }) => {
    const formData = new FormData();
    
    const requestData = { planId, message, needsTts };
    formData.append(
      "data",
      new Blob([JSON.stringify(requestData)], { type: "application/json" })
    );

    if (imageFile) {
      formData.append("image", imageFile);
    }

    const response = await api.post("/api/tutor/feedback/chat", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    return response.data;
  },

  // 6. 진행 중인 학습 목록 조회 (대시보드 리스트)
  getStudyList: async () => {
    const response = await api.get("/api/study/list");
    return response.data;
  },

  // 7. 수업 시작 (오프닝)
  startClass: async ({
    planId,
    dayCount,
    personaName,
    dailyMood,
    customOption,
    needsTts,
  }) => {
    const response = await api.post("/api/tutor/class/start", {
      planId,
      dayCount,
      personaName,
      dailyMood,
      customOption,
      needsTts,
    });
    return response.data;
  },

  // 8. 세션(모드) 변경 알림 및 AI 멘트 요청
  startSessionMode: async ({
    planId,
    sessionMode,
    personaName,
    dayCount,
    needsTts,
  }) => {
    const response = await api.post("/api/tutor/session/start", {
      planId,
      sessionMode,
      personaName,
      dayCount,
      needsTts,
    });
    return response.data;
  },

  // 9. 음성 인식 (STT)
  uploadAudio: async (audioBlob) => {
    const formData = new FormData();
    formData.append("audio", audioBlob, "recording.webm");

    const response = await api.post("/api/tutor/stt", formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    });
    return response.data;
  },

  // 10. 데일리 테스트 문제 생성 요청
  generateDailyTest: async (planId, dayCount) => {
    const response = await api.get("/api/tutor/test/generate", {
      params: { planId, dayCount },
    });
    return response.data;
  },

  // 11. 테스트 답안 제출
  submitDailyTest: async ({ planId, textAnswer, imageFile }) => {
    const formData = new FormData();
    const requestData = { planId, textAnswer };
    
    formData.append(
      "data",
      new Blob([JSON.stringify(requestData)], { type: "application/json" }),
    );

    if (imageFile) {
      formData.append("image", imageFile);
    }

    const response = await api.post("/api/tutor/test/submit", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    return response.data;
  },

  // 12. 학생 피드백 제출 (튜터 평가)
  submitStudentFeedback: async ({ planId, dayCount, feedback, rating }) => {
    const response = await api.post("/api/tutor/review", {
      planId,
      dayCount,
      feedback,
      rating,
    });
    return response.data;
  },

  // 13. 복습 자료(PDF) 다운로드
  downloadReviewPdf: async (planId, dayCount) => {
    const response = await api.get("/api/study/review/download", {
      params: { planId, dayCount },
      responseType: "blob",
    });
    return response.data;
  },

  // 14. 학습 플랜 상세 정보 (로드맵 등)
  getPlanDetail: async (planId) => {
    const response = await api.get(`/api/study/plans/${planId}`);
    return response.data;
  },

  // 15. 월간 캘린더 데이터 조회
  getMonthlyCalendar: async ({ year, month, planId }) => {
    const params = { year, month };
    if (planId != null) params.planId = planId;
    const response = await api.get("/api/study/calendar", { params });
    return response.data;
  },

  // 16. AI 피드백 생성 (하루 학습 마감 처리)
  generateAiFeedback: async (planId) => {
    const response = await api.post(`/api/study/plans/${planId}/ai-feedback`);
    return response.data;
  },
};