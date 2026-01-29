import { create } from "zustand";
import { studyApi } from "../apis/studys/studysApi";

// 순차적 진행을 위한 세션 순서 정의
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

// 모드별 라벨 및 기본 시간 설정
export const SESSION_MODES = {
  CLASS: { label: "수업", defaultTime: 5 * 60 },
  BREAK: { label: "쉬는 시간", defaultTime: 1 * 60 },
  TEST: { label: "테스트", defaultTime: 15 * 60 },
  GRADING: { label: "채점 중", defaultTime: 10 },
  EXPLANATION: { label: "해설 강의", defaultTime: 10 * 60 },
  AI_FEEDBACK: { label: "AI 피드백", defaultTime: 5 * 60 },
  STUDENT_FEEDBACK: { label: "수업 평가", defaultTime: 3 * 60 },
  REVIEW: { label: "복습 자료", defaultTime: 0 },
};

// [Helper] 날짜 비교 함수
const isSameDate = (dateString) => {
  if (!dateString) return false;
  const today = new Date();
  const target = new Date(dateString);
  return (
    today.getFullYear() === target.getFullYear() &&
    today.getMonth() === target.getMonth() &&
    today.getDate() === target.getDate()
  );
};

const useStudyStore = create((set, get) => ({
  studyDay: 1,      
  planId: null,
  studyGoal: "",     
  selectedTutorId: "kangaroo", 
  
  todayTopic: "", 
  isStudyCompletedToday: false, 
  
  messages: [],
  isLoading: false,     
  isChatLoading: false, 

  // 세션 상태
  currentMode: "CLASS",  
  timeLeft: 0,           
  isTimerRunning: false, 
  sessionSchedule: {},   
  currentStepIndex: 0,   

  isSpeakerOn: false,     

  // --- [Actions] 동작 함수들 ---

  // [수정 1] 플랜 설정 시, 같은 플랜이면 리셋하지 않음
  setPlanInfo: (planId, goal) => {
    const currentPlanId = get().planId;

    // 기존과 다른 새로운 플랜을 선택했을 때만 상태 초기화
    if (currentPlanId !== planId) {
        set({ 
            planId, 
            studyGoal: goal,
            messages: [],           // 메시지 초기화
            currentMode: "CLASS",   // 모드 초기화
            currentStepIndex: 0,
            isTimerRunning: false,
            timeLeft: SESSION_MODES["CLASS"].defaultTime // 시간도 초기화
        });
    } else {
        // 같은 플랜이면 목표 텍스트만 업데이트하고 나머지는 유지 (이어하기)
        set({ studyGoal: goal });
    }
  },

  loadUserStatus: async (specificPlanId = null) => {
    set({ isLoading: true });
    try {
      const targetPlanId = specificPlanId || get().planId;
      if (!targetPlanId) {
          set({ isLoading: false });
          return;
      }

      const data = await studyApi.getStudyStatus(targetPlanId);
      
      if (data) {
        const isFinished = isSameDate(data.lastStudyDate);

        set({ 
            planId: data.planId, 
            studyDay: data.currentDay || 1, 
            studyGoal: data.goal,
            todayTopic: data.todayTopic || "오늘의 학습",
            selectedTutorId: data.personaName ? data.personaName.toLowerCase() : "kangaroo",
            isStudyCompletedToday: isFinished 
        });
      }
    } catch (error) {
      console.error("사용자 상태 로드 실패:", error);
    } finally {
      set({ isLoading: false });
    }
  },

  toggleSpeaker: () => {
    set((state) => ({ isSpeakerOn: !state.isSpeakerOn }));
  },

  // [수정 2] 페이지 진입 시 초기화 로직 (이어하기 지원)
  initializeStudySession: async () => {
     const state = get();
     
     // 이미 메시지가 존재한다면(학습 중이었다면) 초기화하지 않고 리턴
     if (state.messages.length > 0) {
         return; 
     }
     
     // 플랜 ID가 없거나 데이터가 비어있을 때만 서버에서 상태 로드
     if (!state.planId) {
         await state.loadUserStatus();
     }
  },

  startClassSession: async (tutorInfo, navigate) => {
    if (get().isStudyCompletedToday) {
        alert("오늘의 학습은 이미 완료되었습니다. 내일 만나요!");
        return;
    }

    // [참고] 여기서는 명시적으로 '시작' 버튼을 눌렀으므로 초기화해도 되지만,
    // TutorSelectionPage에서 넘어올 때 호출되므로 messages를 비워주는 게 맞음
    set({ isLoading: true, messages: [] });
    
    const { planId, studyDay, isSpeakerOn } = get();

    if (!planId) {
        alert("학습 정보를 찾을 수 없습니다. 다시 시도해주세요.");
        set({ isLoading: false });
        return;
    }

    try {
      const res = await studyApi.startClass({
        planId,
        dayCount: studyDay,
        personaName: tutorInfo.id.toUpperCase(),
        dailyMood: "NORMAL",
        customOption: tutorInfo.isCustom ? tutorInfo.customRequirement : null,
        needsTts: isSpeakerOn 
      });

      set({ 
        selectedTutorId: tutorInfo.id.toLowerCase(),
        messages: [{ 
            type: 'AI', 
            content: res.aiMessage, 
            audioUrl: res.audioUrl,
            imageUrl: res.imageUrl 
        }],
        currentMode: "CLASS",
        currentStepIndex: 0,
        sessionSchedule: res.schedule || {} 
      });

      get().setupMode("CLASS", res.schedule || {}, false);
      
      navigate("/study");

    } catch (error) {
      console.error("수업 시작 실패:", error);
      alert("수업을 시작하는 데 문제가 발생했습니다.");
    } finally {
      set({ isLoading: false });
    }
  },

  setupMode: async (mode, scheduleMap, shouldFetchMessage = true) => {
    const config = SESSION_MODES[mode] || SESSION_MODES.CLASS;
    const duration = scheduleMap[mode] !== undefined ? scheduleMap[mode] : config.defaultTime;

    set({ 
      currentMode: mode, 
      timeLeft: duration,
      isTimerRunning: duration > 0,
      isChatLoading: shouldFetchMessage 
    });

    if (shouldFetchMessage) {
        try {
            const { planId, selectedTutorId, studyDay, isSpeakerOn } = get();
            const res = await studyApi.startSessionMode({
                planId,
                sessionMode: mode,
                personaName: selectedTutorId.toUpperCase(),
                dayCount: studyDay,
                needsTts: isSpeakerOn 
            });

            set((state) => ({
                messages: [...state.messages, { 
                    type: 'AI', 
                    content: res.aiMessage, 
                    audioUrl: res.audioUrl,
                    imageUrl: res.imageUrl 
                }],
                isChatLoading: false
            }));

        } catch (error) {
            console.error(`세션(${mode}) 멘트 로드 실패:`, error);
            set({ isChatLoading: false });
        }
    }
  },

  tick: () => {
    const { timeLeft, nextSessionStep } = get();
    if (timeLeft > 0) {
      set({ timeLeft: timeLeft - 1 });
    } else {
      nextSessionStep();
    }
  },

  nextSessionStep: async () => {
    const { currentStepIndex, sessionSchedule, planId } = get();
    const nextIndex = currentStepIndex + 1;

    if (nextIndex < SESSION_SEQUENCE.length) {
      const nextMode = SESSION_SEQUENCE[nextIndex];
      set({ currentStepIndex: nextIndex });
      get().setupMode(nextMode, sessionSchedule, true);

      if (nextMode === "REVIEW") {
          try {
              await studyApi.generateAiFeedback(planId);
              set({ isStudyCompletedToday: true }); 
          } catch (e) {
              console.error("학습 마무리 처리 실패:", e);
          }
      }
    } else {
      set({ isTimerRunning: false });
      set((state) => ({
        messages: [...state.messages, { type: 'AI', content: "오늘의 모든 학습이 종료되었습니다! 복습 자료를 확인해보세요." }]
      }));
    }
  },

  sendMessage: async (text) => {
      set((state) => ({ messages: [...state.messages, { type: 'USER', content: text }], isChatLoading: true }));
      try {
          const { planId, isSpeakerOn } = get(); 
          const res = await studyApi.sendChatMessage({ 
              planId, 
              message: text, 
              needsTts: isSpeakerOn 
          });
          set((state) => ({ messages: [...state.messages, { type: 'AI', content: res.aiResponse, audioUrl: res.audioUrl }], isChatLoading: false }));
      } catch (e) {
          console.error(e);
          set((state) => ({ messages: [...state.messages, { type: 'AI', content: "오류가 발생했습니다." }], isChatLoading: false }));
      }
  },
}));

export default useStudyStore;