import { api } from "../configs/axiosConfig";

export const practiceApi = {
  // 1) 문제 세트 생성
  generateTest: async ({
    planId,
    questionCount = 5,
    difficulty = "NORMAL",
    isWeaknessMode = false,
  }) => {
    const res = await api.post("/api/practice/generate", {
      planId,
      questionCount,
      difficulty,
      isWeaknessMode,
    });
    return res.data;
  },

  // 2) 제출 + AI 채점
  submitTest: async ({ planId, answers }) => {
    const res = await api.post("/api/practice/submit", {
      planId,
      answers,
    });
    return res.data;
  },

  // 3) 오답 클리닉(약점 분석)
  getWeaknessAnalysis: async (planId) => {
    const res = await api.get("/api/practice/weakness", {
      params: { planId },
    });
    return res.data;
  },
};