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

const useStudyStore = create((set, get) => ({
  studyDay: 1,      
  planId: null,
  studyGoal: "",     
  selectedTutorId: "kangaroo", 
  
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
  
  // [New] 오늘 학습 완료 여부 상태 추가
  isStudyCompletedToday: false,

  // --- [Actions] 동작 함수들 ---

  setPlanInfo: (planId, goal) => {
    // 플랜 ID가 바뀔 때만 초기화 (이어하기 지원)
    if (get().planId !== planId) {
        set({ 
            planId, 
            studyGoal: goal,
            messages: [],
            currentMode: "CLASS",
            currentStepIndex: 0,
            isTimerRunning: false,
            isStudyCompletedToday: false // 초기화
        });
    } else {
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
        set({ 
            planId: data.planId, 
            studyDay: data.currentDay || 1, 
            studyGoal: data.goal,
            selectedTutorId: data.personaName ? data.personaName.toLowerCase() : "kangaroo",
            // 만약 API에서 isCompleted 같은 필드를 준다면 여기서도 세팅 가능
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

  initializeStudySession: async () => {
     if (get().messages.length > 0) return; // 이어하기
     if (!get().planId) await get().loadUserStatus();
  },

  startClassSession: async (tutorInfo, navigate) => {
    // 이미 완료된 상태라면 차단 로직을 추가할 수도 있음
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
        sessionSchedule: res.schedule || {},
        isStudyCompletedToday: false 
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

  // [핵심 수정] 다음 단계 전환 및 학습 완료 처리
  nextSessionStep: async () => {
    const { currentStepIndex, sessionSchedule, planId } = get();
    const nextIndex = currentStepIndex + 1;

    if (nextIndex < SESSION_SEQUENCE.length) {
      const nextMode = SESSION_SEQUENCE[nextIndex];
      set({ currentStepIndex: nextIndex });
      get().setupMode(nextMode, sessionSchedule, true);

      // [New] 마지막 단계인 'REVIEW' (복습) 모드로 진입하면 서버에 완료 요청
      if (nextMode === "REVIEW") {
          try {
              // 백엔드 API 명세에 있는 AI 피드백 생성 API 호출
              // 이 API가 실행되면 DB의 study_logs 테이블에 피드백이 저장되고 완료 처리될 것으로 예상됨
              await studyApi.generateAiFeedback(planId);
              
              set({ isStudyCompletedToday: true }); // 프론트 상태도 완료로 변경
              console.log("오늘 학습 완료 및 피드백 생성 요청 성공");
          } catch (e) {
              console.error("학습 마무리(AI 피드백 생성) 처리 실패:", e);
              // 에러가 나더라도 UI는 멈추지 않도록 처리
          }
      }

    } else {
      // 모든 시퀀스가 끝났을 때
      set({ isTimerRunning: false });
      set((state) => ({
        messages: [...state.messages, { type: 'AI', content: "오늘의 모든 학습이 종료되었습니다! 복습 자료를 확인하고 푹 쉬세요." }]
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