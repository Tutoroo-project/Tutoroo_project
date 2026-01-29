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

// [Helper] 날짜 비교 함수 (YYYY-MM-DD)
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

  // [New] 추가된 상태
  todayTopic: "",               // 오늘의 로드맵 주제
  isStudyCompletedToday: false, // 오늘 학습 완료 여부 (Daily Lock)
  
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

  // 1. 플랜 기본 정보 설정
  setPlanInfo: (planId, goal) => {
    set({ planId, studyGoal: goal });
  },

  // 2. 사용자 학습 상태 로드
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
        // [수정] 백엔드 응답에서 마지막 학습일 확인하여 오늘 완료 여부 설정
        const isFinished = isSameDate(data.lastStudyDate);

        set({ 
            planId: data.planId, 
            studyDay: data.currentDay || 1, 
            studyGoal: data.goal,
            todayTopic: data.todayTopic || "오늘의 학습", // 로드맵 주제 매핑
            selectedTutorId: data.personaName ? data.personaName.toLowerCase() : "kangaroo",
            isStudyCompletedToday: isFinished // 완료 여부 상태 저장
        });
      }
    } catch (error) {
      console.error("사용자 상태 로드 실패:", error);
    } finally {
      set({ isLoading: false });
    }
  },

  // 3. 스피커 토글 (TTS On/Off)
  toggleSpeaker: () => {
    set((state) => ({ isSpeakerOn: !state.isSpeakerOn }));
  },

  // 4. 초기 세션 데이터 로드
  initializeStudySession: async () => {
     if (get().messages.length > 0 && get().currentMode === "CLASS") return;
     
     if (!get().planId) {
         await get().loadUserStatus();
     }
  },

  // 5. 수업 시작
  startClassSession: async (tutorInfo, navigate) => {
    // [보호] 이미 완료했으면 시작 불가
    if (get().isStudyCompletedToday) {
        alert("오늘의 학습은 이미 완료되었습니다. 내일 만나요!");
        return;
    }

    set({ isLoading: true, messages: [] });
    const { planId, studyDay, isSpeakerOn } = get();

    if (!planId) {
        alert("학습 정보를 찾을 수 없습니다. 다시 시도해주세요.");
        set({ isLoading: false });
        return;
    }

    try {
      // 수업 시작 API 호출
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

  // 6. 모드 설정 및 AI 멘트/이미지 요청
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

  // 7. 타이머 틱
  tick: () => {
    const { timeLeft, nextSessionStep } = get();
    if (timeLeft > 0) {
      set({ timeLeft: timeLeft - 1 });
    } else {
      nextSessionStep();
    }
  },

  // 8. 다음 단계로 자동 전환 (수정: 학습 완료 처리 추가)
  nextSessionStep: async () => {
    const { currentStepIndex, sessionSchedule, planId } = get();
    const nextIndex = currentStepIndex + 1;

    if (nextIndex < SESSION_SEQUENCE.length) {
      const nextMode = SESSION_SEQUENCE[nextIndex];
      set({ currentStepIndex: nextIndex });
      get().setupMode(nextMode, sessionSchedule, true);

      // [New] 다음 모드가 REVIEW(복습)라면 오늘 학습 마무리 단계임
      if (nextMode === "REVIEW") {
          try {
              // 백엔드에 완료 처리 요청 (AI 피드백 생성이 트리거 역할)
              await studyApi.generateAiFeedback(planId);
              // 프론트 상태 즉시 잠금
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

  // 9. 사용자 메시지 전송
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