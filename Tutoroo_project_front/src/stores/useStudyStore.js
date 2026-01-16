import { create } from "zustand";
import { studyApi } from "../apis/studys/studysApi";

export const SESSION_MODES = {
  CLASS: { label: "수업", defaultTime: 50 * 60 },
  BREAK: { label: "휴식", defaultTime: 10 * 60 },   
  TEST: { label: "테스트", defaultTime: 30 * 60}, 
  FEEDBACK: { label: "피드백", defaultTime: 0 },
};

const useStudyStore = create((set, get) => ({
  studyDay: 1,
  isLoading: false,
  selectedTutorId: "kangaroo",
  
  messages: [],  
  isChatLoading: false,

  loadUserStatus: async () => {
    set({ isLoading: true });
    try {
      const data = await studyApi.getStudyStatus();
      set({ studyDay: data.day_count || 1 }); 
    } catch (error) {
      set({ studyDay: 1 });
    } finally {
      set({ isLoading: false });
    }
  },

  setTutorId: (id) => set({ selectedTutorId: id }),

  // 학습 시작 
  startStudyPlan: async (customReq, navigate) => {
    set({ isLoading: true });
    const { selectedTutorId } = get();
    const personaString = customReq ? `BASE:${selectedTutorId}|CUSTOM:${customReq}` : selectedTutorId;

    try {
      await studyApi.createStudyPlan({
        persona: personaString,
        goal: "Daily Study", 
        start_date: new Date().toISOString().split('T')[0]
      });

      // 입장 메시지 추가
      set({ 
        messages: [{ type: 'AI', content: `안녕하세요! ${selectedTutorId} 선생님입니다. 50분 동안 수업을 진행하겠습니다!` }] 
      });

      navigate("/study");
      get().setSessionMode("CLASS");

    } catch (error) {
      alert("학습 시작 실패");
    } finally {
      set({ isLoading: false });
    }
  },

  // 채팅 전송 
  sendMessage: async (userText) => {
    if (!userText.trim()) return;

    // 내 말풍선 즉시 추가
    const prevMessages = get().messages;
    set({ 
      messages: [...prevMessages, { type: 'USER', content: userText }],
      isChatLoading: true 
    });

    try {
      const data = await studyApi.sendChatMessage(userText);
      const aiReply = data.reply || "AI 응답이 없습니다.";

      set((state) => ({
        messages: [...state.messages, { type: 'AI', content: aiReply }],
        isChatLoading: false
      }));
    } catch (error) {
      console.error(error);
      set((state) => ({
        messages: [...state.messages, { type: 'AI', content: "서버 연결에 실패했습니다." }],
        isChatLoading: false
      }));
    }
  },

  // 타이머 및 세션 자동 전환
  currentMode: "CLASS",
  timeLeft: SESSION_MODES.CLASS.defaultTime,
  isTimerRunning: false,

  setSessionMode: (modeKey) => {
    const config = SESSION_MODES[modeKey];
    set({ 
      currentMode: modeKey, 
      timeLeft: config.defaultTime,
      isTimerRunning: true 
    });
  },

  // 1초씩 감소 + 0초가 되면 다음 단계로 이동
  tick: () => {
    const { timeLeft, currentMode } = get();

    if (timeLeft > 0) {
      set({ timeLeft: timeLeft - 1 });
    } else {
      // 시간이 0이 되었을 때 다음 단계 로직
      get().handleSessionEnd(currentMode);
    }
  },

  // 세션 종료 시 자동 진행
  handleSessionEnd: (mode) => {
    if (mode === "CLASS") {
      // 수업 끝 -> 휴식 시작
      set((state) => ({
        messages: [...state.messages, { type: 'AI', content: "50분 수업이 끝났습니다! 잠시 휴식할까요?" }]
      }));
      get().setSessionMode("BREAK");
    } 
    else if (mode === "BREAK") {
      // 휴식 끝 -> 테스트 시작
      set((state) => ({
        messages: [...state.messages, { type: 'AI', content: "휴식 끝! 이제 배운 내용을 테스트해보겠습니다." }]
      }));
      get().setSessionMode("TEST");
    }
    else if (mode === "TEST") {
      // 테스트 끝 -> 피드백
      set((state) => ({
        messages: [...state.messages, { type: 'AI', content: "테스트 종료! 결과를 분석해 드릴게요." }]
      }));
      get().setSessionMode("FEEDBACK");
    }
  }
}));

export default useStudyStore;