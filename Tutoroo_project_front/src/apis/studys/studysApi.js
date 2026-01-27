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

  // 3. 학습 로그 저장 (단순 저장)
  saveStudyLog: async (logData) => {
    const response = await api.post("/api/study/logs", logData);
    return response.data;
  },

  // 4. 메시지 전송 (채팅)
  // [수정] needsTts 파라미터 추가
  sendChatMessage: async ({ planId, message, needsTts }) => {
    const response = await api.post("/api/tutor/feedback/chat", {
      planId,
      message,
      needsTts, // [New] TTS 생성 여부 (true/false)
    });
    return response.data;
  },

  // 5. 진행 중인 학습 목록 조회 (사이드바 등)
  getStudyList: async () => {
    const response = await api.get("/api/study/list");
    return response.data;
  },

  // 6. 수업 시작하기 (오프닝 + 스케줄 생성)
  // [수정] needsTts 파라미터 추가
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
      customOption, // 커스텀 요구사항
      needsTts, // [New] TTS 생성 여부
    });
    return response.data;
  },

  // 7. [New] 세션(모드) 변경 알림 및 AI 멘트 요청
  // BREAK, TEST, GRADING 등 모드가 바뀔 때 호출
  startSessionMode: async ({
    planId,
    sessionMode,
    personaName,
    dayCount,
    needsTts,
  }) => {
    const response = await api.post("/api/tutor/session/start", {
      planId,
      sessionMode, // CLASS, BREAK, TEST ...
      personaName,
      dayCount,
      needsTts,
    });
    return response.data;
  },

  // 8. 음성 인식 (STT) - 오디오 파일 전송
  uploadAudio: async (audioBlob) => {
    const formData = new FormData();
    // 파일명은 확장자를 맞추기 위해 임의로 지정 (백엔드에서 확장자 감지함)
    formData.append("audio", audioBlob, "recording.webm");

    const response = await api.post("/api/tutor/stt", formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    });
    return response.data; // 변환된 텍스트 반환
  },

  // 9. 데일리 테스트 문제 생성 요청
  generateDailyTest: async (planId, dayCount) => {
    const response = await api.get("/api/tutor/test/generate", {
      params: { planId, dayCount },
    });
    return response.data;
  },

  // 10. 테스트 답안 제출 (이미지 포함 가능)
  submitDailyTest: async ({ planId, textAnswer, imageFile }) => {
    const formData = new FormData();

    // JSON 데이터는 Blob으로 감싸서 전달
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

  // 11. 복습 자료(PDF) 다운로드
  downloadReviewPdf: async (planId, dayCount) => {
    const response = await api.get("/api/study/review/download", {
      params: { planId, dayCount },
      responseType: "blob", // 파일 다운로드 설정
    });
    return response.data;
  },

  // 12. 대시보드 일자별 캘린더안에 들어갈 roadmap 값
  getPlanDetail: async (planId) => {
    const response = await api.get(`/api/study/plans/${planId}`);
    return response.data;
  },

  getMonthlyCalendar: async (year, month) => {
    const res = await api.get(
      `/api/study/calendar?year=${year}&month=${month}`,
    );
    return res.data;
  },

  // 13. 월간 캘린더(점수/완료여부) 조회
  getMonthlyCalendar: async ({ year, month, planId }) => {
    const params = { year, month };
    if (planId) params.planId = planId;

    const response = await api.get("/api/study/calendar", { params });
    return response.data;
  },

  generateAiFeedback: (planId) =>
    instance.post(`/api/study/plans/${planId}/ai-feedback`).then(res => res.data),
};
