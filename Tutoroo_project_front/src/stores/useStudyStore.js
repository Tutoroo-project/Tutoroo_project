import { create } from "zustand";
import { studyApi } from "../apis/studys/studysApi";

export const SESSION_MODES = {
  CLASS: { label: "수업", defaultTime: 50 * 60, hasTimer: true },
  BREAK: { label: "쉬는 시간", defaultTime: 10 * 60, hasTimer: true },   
  TEST: { label: "테스트", defaultTime: 0, hasTimer: false }, 
  GRADING: { label: "채점 중", defaultTime: 0, hasTimer: false },
  FEEDBACK: { label: "피드백", defaultTime: 0, hasTimer: false },
  REVIEW: { label: "복습", defaultTime: 0, hasTimer: false },
};

const useStudyStore = create((set, get) => ({
  // --- [State] 상태 변수 ---
  studyDay: 1,      
  planId: null,     
  isLoading: false,
  selectedTutorId: "tiger",
  
  // 채팅 및 오디오 상태
  messages: [],
  isChatLoading: false,
  isSpeakerOn: false, 

  // 타이머 상태
  currentMode: "CLASS",
  timeLeft: SESSION_MODES.CLASS.defaultTime,
  isTimerRunning: false,

  // --- [Actions] 액션 함수 ---

  // 1. [TutorSelectionPage용] 내 학습 상태(Day, PlanId) 불러오기
  loadUserStatus: async () => {
    set({ isLoading: true });
    try {
      const data = await studyApi.getStudyStatus();
      if (!data) {
          // 데이터가 없으면 기본값 유지
          set({ isLoading: false });
          return;
      }

      // API 응답(currentDay)을 스토어 변수(studyDay)에 매핑
      set({ 
        studyDay: data.currentDay || 1, 
        planId: data.planId,
        selectedTutorId: data.personaName ? data.personaName.toLowerCase() : "tiger"
      }); 
    } catch (error) {
      console.error("로드 실패:", error);
      // 에러 시에도 기본값으로 페이지가 뜨긴 해야 함
      set({ studyDay: 1 });
    } finally {
      set({ isLoading: false });
    }
  },

  // 2. [TutorSelectionPage용] 튜터 선택 후 수업 시작하기
  startClassSession: async (tutorInfo, navigate) => {
    set({ isLoading: true });
    const { planId, studyDay } = get();

    if (!planId) {
        alert("학습 플랜 정보를 찾을 수 없습니다. 다시 시도해주세요.");
        await get().loadUserStatus(); // 재시도
        set({ isLoading: false });
        return;
    }

    try {
      // 튜터 선택 화면에서 넘겨받은 ID(예: "TIGER")로 수업 시작 요청
      const res = await studyApi.startClass({
        planId: planId, 
        dayCount: studyDay,
        personaName: tutorInfo.id.toUpperCase(), 
        dailyMood: "HAPPY" 
      });

      // 응답받은 데이터로 채팅방 초기화
      set({ 
        selectedTutorId: tutorInfo.id,
        messages: [{
          type: 'AI',
          content: res.aiMessage,
          audioUrl: res.audioUrl
        }],
        currentMode: "CLASS",
        timeLeft: SESSION_MODES.CLASS.defaultTime,
        isTimerRunning: true,
        isSpeakerOn: false // 수업 진입 시 TTS 꺼둠
      });

      navigate("/study"); // 페이지 이동

    } catch (error) {
      console.error("수업 시작 실패:", error);
      alert("수업을 시작할 수 없습니다. 잠시 후 다시 시도해주세요.");
    } finally {
      set({ isLoading: false });
    }
  },

  // 3. [StudyPage용] 페이지 진입 시 자동 수업 재개 (이미 선택된 튜터로)
  initializeStudySession: async () => {
    set({ isLoading: true, isChatLoading: true });
    try {
        const statusData = await studyApi.getStudyStatus();
        
        if (!statusData) {
            set({ messages: [{ type: 'AI', content: "진행 중인 학습 플랜이 없습니다." }] });
            return;
        }

        const myTutorId = statusData.personaName ? statusData.personaName.toLowerCase() : "tiger";
        const myPlanId = statusData.planId;
        const myDay = statusData.currentDay || 1;

        set({ 
            planId: myPlanId,
            studyDay: myDay, 
            selectedTutorId: myTutorId 
        });

        // 이미 메시지가 있다면 API 호출 생략 (중복 방지)
        if (get().messages.length === 0) {
            const classRes = await studyApi.startClass({
                planId: myPlanId, 
                dayCount: myDay,
                personaName: statusData.personaName || "TIGER", 
                dailyMood: "HAPPY" 
            });

            set({
                messages: [{
                    type: 'AI',
                    content: classRes.aiMessage,
                    audioUrl: classRes.audioUrl
                }],
                currentMode: "CLASS",
                timeLeft: SESSION_MODES.CLASS.defaultTime,
                isTimerRunning: true,
                isSpeakerOn: false 
            });
        }

    } catch (error) {
        console.error("수업 초기화 실패:", error);
    } finally {
        set({ isLoading: false, isChatLoading: false });
    }
  },

  // 4. 채팅 메시지 전송
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
      set((state) => ({
        messages: [...state.messages, { type: 'AI', content: "오류가 발생했습니다." }],
        isChatLoading: false
      }));
    }
  },

  // 기타 유틸리티 함수들
  setSessionMode: (modeKey, customTime = null) => {
    const config = SESSION_MODES[modeKey];
    set({ 
      currentMode: modeKey, 
      timeLeft: customTime !== null ? customTime : config.defaultTime,
      isTimerRunning: config.hasTimer 
    });
  },

  updateTimeLeft: (newTime) => set({ timeLeft: newTime }),
  toggleSpeaker: () => set((state) => ({ isSpeakerOn: !state.isSpeakerOn })),

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