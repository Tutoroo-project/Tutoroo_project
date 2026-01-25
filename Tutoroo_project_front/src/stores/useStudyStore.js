import { create } from "zustand";
import { studyApi } from "../apis/studys/studysApi";

//  순차적 진행을 위한 세션 순서 정의
const SESSION_SEQUENCE = [
  "CLASS",           
  "BREAK",            
  "TEST",             
  "GRADING",          
  "EXPLANATION",     
  "AI_FEEDBACK",     
  "STUDENT_FEEDBACK", 
  "REVIEW"            
];

//  모드별 라벨 및 기본 시간 (AI가 시간을 안 줬을 때 사용)
export const SESSION_MODES = {
  CLASS: { label: "수업", defaultTime: 50 * 60 },
  BREAK: { label: "쉬는 시간", defaultTime: 10 * 60 },
  TEST: { label: "테스트", defaultTime: 15 * 60 },
  GRADING: { label: "채점 중", defaultTime: 10 },
  EXPLANATION: { label: "해설 강의", defaultTime: 10 * 60 },
  AI_FEEDBACK: { label: "AI 피드백", defaultTime: 5 * 60 },
  STUDENT_FEEDBACK: { label: "수업 평가", defaultTime: 3 * 60 },
  REVIEW: { label: "복습 자료", defaultTime: 0 },
};

const useStudyStore = create((set, get) => ({
  studyDay: 1,      
  planId: null,     
  studyGoal: "",   
  isLoading: false,
  selectedTutorId: "tiger",
  
  messages: [],
  isChatLoading: false,
  isSpeakerOn: false, 

  // 세션 관련 상태
  currentMode: "CLASS",
  timeLeft: SESSION_MODES.CLASS.defaultTime,
  isTimerRunning: false,
  
  // 시퀀스 관리용
  currentStepIndex: 0,
  sessionSchedule: {},

  //대시보드에서 플랜 정보(이름 포함) 설정
  setPlanInfo: (planId, goal) => {
    set({ planId, studyGoal: goal });
  },

  loadUserStatus: async (specificPlanId = null) => {
    set({ isLoading: true });
    try {
      const targetPlanId = specificPlanId || get().planId;
      const data = await studyApi.getStudyStatus(targetPlanId);
      
      if (!data) {
          set({ isLoading: false });
          return;
      }
      set({ 
        studyDay: data.currentDay || 1, 
        planId: data.planId, 
        studyGoal: data.goal, 
        selectedTutorId: data.personaName ? data.personaName.toLowerCase() : "tiger" 
      }); 
    } catch (error) {
      console.error("로드 실패:", error);
    } finally {
      set({ isLoading: false });
    }
  },

  initializeStudySession: async () => {
    set({ isLoading: true, isChatLoading: true });
    try {
        const { planId, studyDay, selectedTutorId } = get();

        // 수업 시작 요청 (studyGoal은 이미 setPlanInfo로 설정됨)
        const classRes = await studyApi.startClass({
            planId: planId, 
            dayCount: studyDay,
            personaName: selectedTutorId.toUpperCase(), 
            dailyMood: "HAPPY" 
        });

        const aiSchedule = classRes.schedule || {};

        set({
            messages: [{
                type: 'AI',
                content: classRes.aiMessage,
                audioUrl: classRes.audioUrl
            }],
            currentStepIndex: 0,
            sessionSchedule: aiSchedule, 
            isSpeakerOn: false 
        });

        get().setupMode("CLASS", aiSchedule);

    } catch (error) {
        console.error("수업 초기화 실패:", error);
        set({ messages: [{ type: 'AI', content: "수업 연결에 실패했습니다." }] });
    } finally {
        set({ isLoading: false, isChatLoading: false });
    }
  },

  // 내부 함수: 모드 설정 및 시간 적용
  setupMode: (mode, scheduleMap) => {
    const config = SESSION_MODES[mode];
    const duration = scheduleMap[mode] !== undefined ? scheduleMap[mode] : config.defaultTime;

    set({ 
      currentMode: mode, 
      timeLeft: duration,
      isTimerRunning: duration > 0 
    });
    
    // 모드 변경 시스템 메시지 (선택사항)
    set((state) => ({
        messages: [...state.messages, { type: 'SYSTEM', content: `[${config.label}] 세션이 시작되었습니다.` }]
    }));
  },

  updateTimeLeft: (newTime) => set({ timeLeft: newTime }),
  toggleSpeaker: () => set((state) => ({ isSpeakerOn: !state.isSpeakerOn })),

  // 타이머 및 자동 단계 이동
  tick: () => {
    const { timeLeft, isTimerRunning } = get();
    if (!isTimerRunning) return;

    if (timeLeft > 0) {
      set({ timeLeft: timeLeft - 1 });
    } else {
      // 시간이 다 되면 다음 단계로
      get().nextSessionStep();
    }
  },

  nextSessionStep: () => {
    const { currentStepIndex, sessionSchedule } = get();
    const nextIndex = currentStepIndex + 1;

    if (nextIndex < SESSION_SEQUENCE.length) {
      const nextMode = SESSION_SEQUENCE[nextIndex];
      set({ currentStepIndex: nextIndex });
      
      // 다음 모드 시작
      get().setupMode(nextMode, sessionSchedule);

    } else {
      // 모든 시퀀스 종료
      set({ isTimerRunning: false });
      set((state) => ({
        messages: [...state.messages, { type: 'AI', content: "오늘의 모든 학습이 종료되었습니다! 복습 자료를 확인해보세요." }]
      }));
    }
  },
  
  // 수동 수업 시작 
  startClassSession: async (tutorInfo, navigate) => {
    set({ isLoading: true });
    const { planId, studyDay } = get();

    if (!planId) {
        alert("학습 정보를 찾을 수 없습니다.");
        await get().loadUserStatus();
        set({ isLoading: false });
        return;
    }

    try {
      const res = await studyApi.startClass({
        planId: planId, 
        dayCount: studyDay,
        personaName: tutorInfo.id.toUpperCase(), 
        dailyMood: "HAPPY",
        customOption: tutorInfo.isCustom ? tutorInfo.customRequirement : null 
      });
      
      const aiSchedule = res.schedule || {};

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
        sessionSchedule: res.schedule || {} 
      });
      get().setupMode("CLASS", res.schedule || {});
      
      navigate("/study");

    } catch (error) {
      console.error("수업 시작 실패:", error);
      alert("오류가 발생했습니다.");
    } finally {
      set({ isLoading: false });
    }
  },

  sendMessage: async (text) => {
      set((state) => ({ messages: [...state.messages, { type: 'USER', content: text }], isChatLoading: true }));
      try {
          const res = await studyApi.sendChatMessage(text);
          set((state) => ({ messages: [...state.messages, { type: 'AI', content: res.aiMessage, audioUrl: res.audioUrl }], isChatLoading: false }));
      } catch (e) {
          set((state) => ({ messages: [...state.messages, { type: 'AI', content: "오류가 발생했습니다." }], isChatLoading: false }));
      }
  },

  setSessionMode: (modeKey, customTime = null) => {
    const config = SESSION_MODES[modeKey];
    set({ 
      currentMode: modeKey, 
      timeLeft: customTime !== null ? customTime : config.defaultTime,
      isTimerRunning: config.defaultTime > 0 
    });
  }
}));

export default useStudyStore;