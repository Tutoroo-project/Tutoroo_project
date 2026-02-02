import { create } from "zustand";
import { studyApi } from "../apis/studys/studysApi";

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
  customOption: null, // ✅ 추가
  
  todayTopic: "", 
  isStudyCompletedToday: false, 
  
  messages: [],
  isLoading: false,     
  isChatLoading: false, 

  currentMode: "CLASS",  
  timeLeft: 0,           
  isTimerRunning: false, 
  sessionSchedule: {},   
  currentStepIndex: 0,   

  isSpeakerOn: false,
  
  currentTestQuestion: null,
  userTestAnswer: "",
  testResult: null,

  isInfinitePractice: false,
  setInfinitePractice: (flag) => set({ isInfinitePractice: !!flag }),

  studentRating: 0,
  studentFeedbackText: "",

  setPlanInfo: (planId, goal) => {
    const currentPlanId = get().planId;

    if (currentPlanId !== planId) {
        set({ 
            planId, 
            studyGoal: goal,
            messages: [],
            currentMode: "CLASS",
            currentStepIndex: 0,
            isTimerRunning: false,
            timeLeft: SESSION_MODES["CLASS"].defaultTime,
            isStudyCompletedToday: false,
            currentTestQuestion: null,
            userTestAnswer: "",
            testResult: null,
            studentRating: 0,
            studentFeedbackText: "",
            isInfinitePractice: false,
            customOption: null,
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

  initializeStudySession: async () => {
     const state = get();
     
     if (state.messages.length > 0) {
         return; 
     }
     
     if (!state.planId) {
         await state.loadUserStatus();
     }
  },

  startClassSession: async (tutorInfo, navigate, options = {}) => {
    const isInfinite = !!options?.isInfinite;
    const navigateTo = options?.navigateTo || "/study";
    const dayCount = options?.dayCount;

    if (!isInfinite && get().isStudyCompletedToday) {
        alert("오늘의 학습은 이미 완료되었습니다. 내일 만나요!");
        return;
    }

    set({ isLoading: true, messages: [], isInfinitePractice: isInfinite });
    
    const { planId, studyDay, isSpeakerOn } = get();

    if (!planId) {
        alert("학습 정보를 찾을 수 없습니다. 다시 시도해주세요.");
        set({ isLoading: false });
        return;
    }

    const effectiveDayCount = dayCount !== undefined ? dayCount : studyDay;

    try {
      const res = await studyApi.startClass({
        planId,
        dayCount: effectiveDayCount,
        personaName: tutorInfo.id.toUpperCase(),
        dailyMood: "NORMAL",
        customOption: tutorInfo.isCustom ? tutorInfo.customRequirement : null,
        needsTts: isSpeakerOn 
      });

      // ✅ customOption 저장
      set({ 
        studyDay: effectiveDayCount,
        selectedTutorId: tutorInfo.id.toLowerCase(),
        customOption: tutorInfo.isCustom ? tutorInfo.customRequirement : null,
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
      
      navigate(navigateTo);

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
    const infinite = get().isInfinitePractice;

    set({ 
      currentMode: mode, 
      timeLeft: duration,
      isTimerRunning: !infinite && duration > 0,
      isChatLoading: shouldFetchMessage 
    });

    if (mode === "TEST") {
        await get().generateTestQuestion();
        return;
    }

    if (mode === "STUDENT_FEEDBACK") {
        set({ studentRating: 0, studentFeedbackText: "" });
    }

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
    if (get().isInfinitePractice) return;
    
    const { timeLeft, nextSessionStep } = get();
    if (timeLeft > 0) {
      set({ timeLeft: timeLeft - 1 });
    } else {
      nextSessionStep();
    }
  },

  generateTestQuestion: async () => {
    set({ isChatLoading: true });
    try {
        const { planId, studyDay } = get();
        const question = await studyApi.generateDailyTest(planId, studyDay);
        
        set({ 
            currentTestQuestion: question,
            userTestAnswer: "",
            isChatLoading: false
        });

        set((state) => ({
            messages: [...state.messages, {
                type: 'AI',
                content: question.question,
                testData: question
            }]
        }));

    } catch (error) {
        console.error("테스트 문제 생성 실패:", error);
        set({ isChatLoading: false });
    }
  },

  submitTest: async (answer, imageFile = null) => {
    set({ isChatLoading: true });
    try {
        const { planId } = get();
        const result = await studyApi.submitDailyTest({
            planId,
            textAnswer: answer,
            imageFile
        });

        set({ 
            testResult: result,
            isChatLoading: false
        });

        set((state) => ({
            messages: [...state.messages, {
                type: 'USER',
                content: answer
            }, {
                type: 'AI',
                content: `점수: ${result.score}점\n\n${result.feedback}`,
                audioUrl: result.audioUrl
            }]
        }));

        setTimeout(() => {
            get().nextSessionStep();
        }, 3000);

    } catch (error) {
        console.error("테스트 제출 실패:", error);
        set({ isChatLoading: false });
    }
  },

  submitStudentFeedback: async () => {
    const { planId, studyDay, studentRating, studentFeedbackText } = get();
    
    if (studentRating === 0) {
        alert("별점을 선택해주세요!");
        return;
    }

    set({ isChatLoading: true });
    
    try {
        await studyApi.submitStudentFeedback({
            planId,
            dayCount: studyDay,
            feedback: studentFeedbackText,
            rating: studentRating
        });

        set((state) => ({
            messages: [...state.messages, {
                type: 'AI',
                content: "소중한 의견 감사합니다! 더 나은 수업을 위해 노력하겠습니다."
            }],
            isChatLoading: false
        }));

        setTimeout(() => {
            get().nextSessionStep();
        }, 2000);

    } catch (error) {
        console.error("피드백 제출 실패:", error);
        set({ isChatLoading: false });
    }
  },

  nextSessionStep: async () => {
    const { currentStepIndex, sessionSchedule, planId, todayTopic, testResult } = get();
    const nextIndex = currentStepIndex + 1;

    if (nextIndex < SESSION_SEQUENCE.length) {
      const nextMode = SESSION_SEQUENCE[nextIndex];
      set({ currentStepIndex: nextIndex });
      
      get().setupMode(nextMode, sessionSchedule, true);

      if (nextMode === "REVIEW") {
        if (get().isInfinitePractice) {
          set((state) => ({
            messages: [
              ...state.messages,
              {
                type: "AI",
                content:
                  "무한 실습 모드에서는 시간 제한 없이 학습할 수 있어요. 필요하면 다음 단계로 계속 진행하거나 질문을 이어가세요!",
              },
            ],
          }));
          return;
        }

        try {
          set({ isChatLoading: true });

          const logData = {
            planId: planId,
            score: testResult?.score || 0,
            contentSummary: todayTopic || "오늘의 학습",
            isCompleted: true
          };

          await studyApi.saveStudyLog(logData);
          console.log("✅ 학습 로그 저장 완료:", logData);

          const feedbackText = await studyApi.generateAiFeedback(planId);
          
          set({ isStudyCompletedToday: true });

          set((state) => ({
              messages: [
                  ...state.messages,
                  { 
                      type: 'AI', 
                      content: feedbackText || "오늘의 학습 분석 결과를 불러오지 못했습니다.",
                  },
                  {
                      type: 'AI',
                      content: "오늘 학습하느라 정말 고생 많았어요! 아래 버튼을 눌러 복습 자료를 다운로드 받으세요."
                  }
              ],
              isChatLoading: false
          }));

        } catch (e) {
          console.error("학습 마무리 실패:", e);
          set((state) => ({
              messages: [
                  ...state.messages, 
                  { type: 'AI', content: "학습은 완료되었지만 기록 저장에 실패했습니다. 잠시 후 다시 시도해주세요." }
              ],
              isChatLoading: false,
              isStudyCompletedToday: true
          }));
        }
      }

    } else {
      set({ isTimerRunning: false });
      set((state) => ({
        messages: [...state.messages, { type: 'AI', content: "모든 과정이 종료되었습니다. 안녕히 가세요!" }]
      }));
    }
  },

  sendMessage: async (text, imageFile = null) => {
      const userMessage = {
          type: 'USER',
          content: text,
          hasImage: !!imageFile
      };

      set((state) => ({ 
          messages: [...state.messages, userMessage], 
          isChatLoading: true 
      }));
      
      try {
          const { planId, isSpeakerOn } = get(); 
          const res = await studyApi.sendChatMessage({ 
              planId, 
              message: text, 
              needsTts: isSpeakerOn,
              imageFile 
          });
          
          set((state) => ({ 
              messages: [...state.messages, { 
                  type: 'AI', 
                  content: res.aiResponse, 
                  audioUrl: res.audioUrl 
              }], 
              isChatLoading: false 
          }));
          
      } catch (e) {
          console.error("메시지 전송 실패:", e);
          
          const errorMessage = e.response?.status === 500 
              ? "서버에 일시적인 문제가 발생했습니다. 잠시 후 다시 시도해주세요."
              : "메시지 전송 중 오류가 발생했습니다.";
          
          set((state) => ({ 
              messages: [...state.messages, { 
                  type: 'AI', 
                  content: errorMessage 
              }], 
              isChatLoading: false 
          }));
      }
  },
}));

export default useStudyStore;