import { create } from "zustand";
import { studyApi } from "../apis/studys/studysApi";

/* 백엔드 DB/Enum과 매칭될 세션 상태 상수 */
export const SESSION_MODES = {
  CLASS: { label: "수업", defaultTime: 50 * 60 },      // 50분
  BREAK: { label: "휴식", defaultTime: 10 * 60 },      // 10분
  TEST: { label: "테스트", defaultTime: 20 * 60 },     // 20분
  FEEDBACK: { label: "피드백", defaultTime: 0 },       // 시간제한 없음
  EXPLANATION: { label: "해설", defaultTime: 15 * 60 },// 15분
};

const useStudyStore = create((set, get) => ({
  // --- [Data] 유저 & 튜터 정보 ---
  studyDay: 1, // 기본 1일차 (API 로드 전)
  isLoading: false,
  selectedTutorId: "kangaroo", // 기본 튜터
  
  // --- [Action] 초기 데이터 로드 ---
  loadUserStatus: async () => {
    set({ isLoading: true });
    try {
      const data = await studyApi.getStudyStatus();
      // 백엔드에서 user.day_count를 준다고 가정
      set({ studyDay: data.day_count || 1 }); 
    } catch (error) {
      console.error("상태 로드 실패(기본값 사용):", error);
      set({ studyDay: 1 }); // 에러 시 1일차로 간주
    } finally {
      set({ isLoading: false });
    }
  },

  setTutorId: (id) => set({ selectedTutorId: id }),

  // --- [Action] 튜터 선택 완료 및 학습 시작 ---
  startStudyPlan: async (customReq, navigate) => {
    set({ isLoading: true });
    const { selectedTutorId } = get();

    // 백엔드 DB (study_plans.persona) 형식에 맞춰 데이터 가공
    // 커스텀이 없으면 "KANGAROO", 있으면 "BASE:KANGAROO|CUSTOM:사투리..."
    const personaString = customReq 
      ? `BASE:${selectedTutorId}|CUSTOM:${customReq}`
      : selectedTutorId;

    try {
      // API 호출
      await studyApi.createStudyPlan({
        persona: personaString,
        goal: "Daily Study", // 기본 목표
        start_date: new Date().toISOString().split('T')[0]
      });
      
      // 성공 시 페이지 이동
      navigate("/study");
      
      // 수업 모드로 타이머 시작
      get().setSessionMode("CLASS");

    } catch (error) {
      alert("학습 시작에 실패했습니다.");
      console.error(error);
    } finally {
      set({ isLoading: false });
    }
  },

  // --- [Timer] 학습 타이머 로직 ---
  currentMode: "CLASS",
  timeLeft: SESSION_MODES.CLASS.defaultTime,
  isTimerRunning: false,

  setSessionMode: (modeKey) => {
    const config = SESSION_MODES[modeKey];
    set({ 
      currentMode: modeKey, 
      timeLeft: config.defaultTime,
      isTimerRunning: true // 모드 변경 시 자동 시작
    });
  },

  tick: () => set((state) => ({ 
    timeLeft: state.timeLeft > 0 ? state.timeLeft - 1 : 0 
  })),
}));

export default useStudyStore;