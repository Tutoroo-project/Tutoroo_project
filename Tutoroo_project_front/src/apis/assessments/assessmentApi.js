import { api } from "../configs/axiosConfig";

export const consultAssessment = async ({
  studyInfo,
  history,
  lastUserMessage,
}) => {
  const res = await api.post("/api/assessment/consult", {
    studyInfo,
    history,
    lastUserMessage,
  });
  return res.data; // { aiMessage, audioUrl, isFinished }
};

export const generateRoadmap = async ({ studyInfo, history }) => {
  const res = await api.post("/api/assessment/generate", {
    studyInfo,
    history,
  });
  return res.data; // { analyzedLevel, analysisReport, overview, message, planId ... }
};
