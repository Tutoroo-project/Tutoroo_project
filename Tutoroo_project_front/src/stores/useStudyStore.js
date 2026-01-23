import { create } from "zustand";
import { studyApi } from "../apis/studys/studysApi";

export const SESSION_MODES = {
  CLASS: { label: "수업", defaultTime: 50 * 60 },
  BREAK: { label: "휴식", defaultTime: 10 * 60 },   
  TEST: { label: "테스트", defaultTime: 30 * 60}, 
  FEEDBACK: { label: "피드백", defaultTime: 0 },
};

const useStudyStore = create((set, get) => ({
  // --- 상태 변수 ---
  studyDay: 1,      
  planId: null,     
  isLoading: false,
  selectedTutorId: "tiger", // [중요] 기본값 설정
  
  // 채팅 관련
  messages: [],
  isChatLoading: false,

  // 타이머 관련
  currentMode: "CLASS",
  timeLeft: SESSION_MODES.CLASS.defaultTime,
  isTimerRunning: false,

  // --- 액션 ---

  // 1. 초기 상태 로드 (대시보드/페이지 진입 시)
  loadUserStatus: async () => {
    set({ isLoading: true });
    try {
      const data = await studyApi.getStudyStatus();
      
      // [수정] 백엔드 데이터(예: "TIGER")를 소문자로 변환해 스토어에 저장
      const tutorId = data.personaName ? data.personaName.toLowerCase() : "tiger";

      set({ 
        studyDay: data.currentDay || 1, 
        planId: data.planId,
        selectedTutorId: tutorId 
      }); 
    } catch (error) {
      console.error("로드 실패:", error);
      set({ studyDay: 1 });
    } finally {
      set({ isLoading: false });
    }
  },

  // 2. 수업 시작 (튜터 선택 페이지에서 호출)
  startClassSession: async (tutorInfo, navigate) => {
    set({ isLoading: true });
    const { planId, studyDay } = get();

    if (!planId) {
        alert("학습 정보를 불러오는 중입니다. 잠시만 기다려주세요.");
        await get().loadUserStatus();
        set({ isLoading: false });
        return;
    }

    try {
      const res = await studyApi.startClass({
        planId: planId, 
        dayCount: studyDay,
        personaName: tutorInfo.id.toUpperCase(), 
        dailyMood: "HAPPY" 
      });

      set({ 
        // [중요] 선택한 튜터 ID 저장 (이미지 표시용)
        selectedTutorId: tutorInfo.id,
        messages: [{
          type: 'AI',
          content: res.aiMessage,
          audioUrl: res.audioUrl
        }],
        currentMode: "CLASS",
        timeLeft: SESSION_MODES.CLASS.defaultTime,
        isTimerRunning: true
      });

      console.log("Navigating to /study...");
      navigate("/study"); // [수정] 올바른 경로로 이동

    } catch (error) {
      console.error("수업 시작 실패:", error);
      alert("수업을 시작할 수 없습니다. 다시 시도해주세요.");
    } finally {
      set({ isLoading: false });
    }
  },

  // 3. 채팅 메시지 전송
  sendMessage: async (text) => {
    set((state) => ({
      messages: [...state.messages, { type: 'USER', content: text }],
      isChatLoading: true
    }));

    try {
      const res = await studyApi.sendChatMessage(text);
      set((state) => ({
        messages: [...state.messages, { 
          type: 'AI', 
          content: res.aiMessage,
          audioUrl: res.audioUrl
        }],
        isChatLoading: false
      }));
    } catch (error) {
      console.error("메시지 전송 실패:", error);
      set((state) => ({
        messages: [...state.messages, { type: 'AI', content: "오류가 발생했습니다." }],
        isChatLoading: false
      }));
    }
  },

  setSessionMode: (modeKey) => {
    const config = SESSION_MODES[modeKey];
    set({ 
      currentMode: modeKey, 
      timeLeft: config.defaultTime,
      isTimerRunning: true 
    });
  },

  tick: () => {
    const { timeLeft, currentMode } = get();
    if (timeLeft > 0) {
      set({ timeLeft: timeLeft - 1 });
    } else {
      get().handleSessionEnd(currentMode);
    }
  },

  handleSessionEnd: (mode) => {
    if (mode === "CLASS") {
      set((state) => ({
        messages: [...state.messages, { type: 'AI', content: "수업 시간이 끝났어요! 잠시 쉬었다 할까요?" }]
      }));
      get().setSessionMode("BREAK");
    }
  },
}));

export default useStudyStore;