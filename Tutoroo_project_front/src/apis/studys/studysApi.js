import { api } from "../configs/axiosConfig";

export const studyApi = {
  // 1. 내 학습 상태 조회 (몇 일차인지, 현재 진행중인 플랜이 있는지)
  getStudyStatus: async () => {
    const response = await api.get("/api/study/status"); 
    return response.data; // { day_count: 2, has_active_plan: false, ... }
  },

  // 2. 학습 플랜 생성 (튜터 선택 완료 시 호출)
  createStudyPlan: async (planData) => {
    // planData 예시: { persona: "KANGAROO", goal: "...", custom_requirement: "..." }
    const response = await api.post("/api/study/plans", planData);
    return response.data;
  },

  // 3. 학습 로그 저장 (세션 종료/변경 시 호출)
  saveStudyLog: async (logData) => {
    const response = await api.post("/api/study/logs", logData);
    return response.data;
  }
};