import { create } from "zustand";

const useLevelTestStore = create((set) => ({
  subject: null,
  level: null,
  summary: null,
  roadmap: [],

  //  AI 로드맵 이미지 URL (추가)
  roadmapImageUrl: null,

  // 결과 세팅 (AI 연동 시 그대로 사용)
  setResult: (result) =>
    set({
      subject: result.subject ?? null,
      level: result.level ?? null,
      summary: result.summary ?? null,
      roadmap: result.roadmap ?? [],
      roadmapImageUrl: result.roadmapImageUrl ?? null,
    }),

  // 초기화
  reset: () =>
    set({
      subject: null,
      level: null,
      summary: null,
      roadmap: [],
      roadmapImageUrl: null,
    }),
}));

export default useLevelTestStore;
