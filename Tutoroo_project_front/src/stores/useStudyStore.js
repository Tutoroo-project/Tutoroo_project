import { create } from "zustand";
import { studyApi } from "../apis/studys/studysApi";

// [수정] 모드 확장 및 시간 설정
export const SESSION_MODES = {
  CLASS: { label: "수업", defaultTime: 50 * 60, hasTimer: true },
  BREAK: { label: "쉬는 시간", defaultTime: 10 * 60, hasTimer: true },   
  TEST: { label: "테스트", defaultTime: 0, hasTimer: false }, // 시간 제한 없음
  GRADING: { label: "채점 중", defaultTime: 0, hasTimer: false },
  FEEDBACK: { label: "피드백", defaultTime: 0, hasTimer: false },
  REVIEW: { label: "복습", defaultTime: 0, hasTimer: false },
};

const useStudyStore = create((set, get) => ({
  // --- 상태 변수 ---
  studyDay: 1,      
  planId: null,     
  isLoading: false,
  selectedTutorId: "tiger",
  
  // 채팅 관련
  messages: [],
  isChatLoading: false,

  // [New] TTS/STT 제어
  isSpeakerOn: true, // 기본값: 켜짐

  // 타이머 관련
  currentMode: "CLASS",
  timeLeft: SESSION_MODES.CLASS.defaultTime,
  isTimerRunning: false,

  // --- 액션 ---

  // 1. 초기 상태 로드
  loadUserStatus: async () => {
    set({ isLoading: true });
    try {
      const data = await studyApi.getStudyStatus();
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

  // 2. 수업 시작
  startClassSession: async (tutorInfo, navigate) => {
    set({ isLoading: true });
    const { planId, studyDay } = get();

    if (!planId) {
        alert("학습 정보를 불러오는 중입니다.");
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
      navigate("/study");

    } catch (error) {
      console.error("수업 시작 실패:", error);
      alert("오류가 발생했습니다.");
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

  // [New] 모드 변경 및 시간 수동 설정
  setSessionMode: (modeKey, customTime = null) => {
    const config = SESSION_MODES[modeKey];
    set({ 
      currentMode: modeKey, 
      timeLeft: customTime !== null ? customTime : config.defaultTime,
      isTimerRunning: config.hasTimer 
    });
  },

  // [New] 타이머 시간 강제 조정 (유저가 설정 변경 시)
  updateTimeLeft: (newTime) => {
    set({ timeLeft: newTime });
  },

  // [New] 스피커 토글
  toggleSpeaker: () => {
    set((state) => ({ isSpeakerOn: !state.isSpeakerOn }));
  },

  tick: () => {
    const { timeLeft, currentMode, isTimerRunning } = get();
    if (!isTimerRunning) return;

    if (timeLeft > 0) {
      set({ timeLeft: timeLeft - 1 });
    } else {
      get().handleSessionEnd(currentMode);
    }
  },

  handleSessionEnd: (mode) => {
    // 수업 종료 -> 쉬는 시간 전환 자동 제안
    if (mode === "CLASS") {
      set((state) => ({
        messages: [...state.messages, { type: 'AI', content: "수업 시간이 끝났어요! 10분간 쉬는 시간을 가질까요?" }]
      }));
      get().setSessionMode("BREAK");
    } else if (mode === "BREAK") {
        set((state) => ({
            messages: [...state.messages, { type: 'AI', content: "쉬는 시간이 끝났습니다. 다음 학습을 시작해볼까요?" }]
        }));
        set({ isTimerRunning: false });
    }
  },
}));

export default useStudyStore;