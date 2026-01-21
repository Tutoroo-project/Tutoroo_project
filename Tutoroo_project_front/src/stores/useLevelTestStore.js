import { create } from "zustand";

const useLevelTestStore = create((set) => ({

  studyInfo: null, // { goal, availableTime, deadline }

  subject: null,
  level: null,
  summary: null,
  roadmap: [],

  setStudyInfo: (info) => 
    set({
      studyInfo: {
        goal: info.goal ?? "",
        availableTime: info.availableTime ?? "",
        deadline: info.deadline ?? "",
      },
    }),
  
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
      studyInfo: null,
      subject: null,
      level: null,
      summary: null,
      roadmap: [],
      roadmapImageUrl: null,
    }),
}));

export default useLevelTestStore;
