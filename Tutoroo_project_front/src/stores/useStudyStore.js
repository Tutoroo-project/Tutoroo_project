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
  // --- [State] 상태 변수들 ---
  studyDay: 1,      
  planId: null,
  studyGoal: "",     
  selectedTutorId: "kangaroo", // 기본값 (소문자 유지)
  
  messages: [],
  isLoading: false,     // 전체 페이지 로딩
  isChatLoading: false, // 채팅/AI 응답 로딩

  // 세션 상태
  currentMode: "CLASS",  
  timeLeft: 0,           
  isTimerRunning: false, 
  sessionSchedule: {},   
  currentStepIndex: 0,   

  // 설정 상태
  isSpeakerOn: true,     

  // --- [Actions] 동작 함수들 ---

  // 1. [복구됨] 플랜 기본 정보 설정
  setPlanInfo: (planId, goal) => {
    set({ planId, studyGoal: goal });
  },

  // 2. [복구됨] 사용자 학습 상태 로드 (대시보드/튜터선택 페이지용)
  loadUserStatus: async (specificPlanId = null) => {
    set({ isLoading: true });
    try {
      const targetPlanId = specificPlanId || get().planId;
      
      // planId가 없으면 로드 중단
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
            // 튜터 ID 소문자로 변환하여 프론트 이미지 매핑 오류 방지
            selectedTutorId: data.personaName ? data.personaName.toLowerCase() : "kangaroo"
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

  // 4. 초기 세션 데이터 로드 (StudyPage 진입 시 안전장치)
  initializeStudySession: async () => {
     // 이미 메시지가 있고 수업 중이라면 중복 호출 방지
     if (get().messages.length > 0 && get().currentMode === "CLASS") return;
     
     // 튜터 선택 페이지를 거치지 않고 바로 들어왔을 경우 상태 로드 시도
     if (!get().planId) {
         await get().loadUserStatus();
     }
  },

  // 5. 수업 시작 (TutorSelectionPage에서 호출)
  startClassSession: async (tutorInfo, navigate) => {
    set({ isLoading: true, messages: [] });
    const { planId, studyDay, isSpeakerOn } = get();

    if (!planId) {
        alert("학습 정보를 찾을 수 없습니다. 다시 시도해주세요.");
        set({ isLoading: false });
        return;
    }

    try {
      // 수업 시작 API 호출 (오프닝 멘트 + 스케줄 + 이미지 수신)
      const res = await studyApi.startClass({
        planId,
        dayCount: studyDay,
        personaName: tutorInfo.id.toUpperCase(), // API는 대문자 요구
        dailyMood: "NORMAL",
        customOption: tutorInfo.isCustom ? tutorInfo.customRequirement : null,
        needsTts: isSpeakerOn
      });

      // 스토어 상태 업데이트
      set({ 
        selectedTutorId: tutorInfo.id.toLowerCase(), // 프론트는 소문자 사용
        messages: [{ 
            type: 'AI', 
            content: res.aiMessage, 
            audioUrl: res.audioUrl,
            imageUrl: res.imageUrl // [New] 이미지 URL 저장
        }],
        currentMode: "CLASS",
        currentStepIndex: 0,
        sessionSchedule: res.schedule || {} 
      });

      // 모드 설정 (API 중복 호출 방지를 위해 fetchMessage=false)
      get().setupMode("CLASS", res.schedule || {}, false);
      
      // 페이지 이동
      navigate("/study");

    } catch (error) {
      console.error("수업 시작 실패:", error);
      alert("수업을 시작하는 데 문제가 발생했습니다.");
    } finally {
      set({ isLoading: false });
    }
  },

  // 6. 모드 설정 및 AI 멘트/이미지 요청 (핵심 로직)
  setupMode: async (mode, scheduleMap, shouldFetchMessage = true) => {
    const config = SESSION_MODES[mode] || SESSION_MODES.CLASS;
    // 스케줄에 설정된 시간이 있으면 사용, 없으면 기본값
    const duration = scheduleMap[mode] !== undefined ? scheduleMap[mode] : config.defaultTime;

    set({ 
      currentMode: mode, 
      timeLeft: duration,
      isTimerRunning: duration > 0,
      isChatLoading: shouldFetchMessage // 메시지를 가져와야 하면 로딩 시작
    });

    // 멘트 생성이 필요한 경우 (단계 전환 시)
    if (shouldFetchMessage) {
        try {
            const { planId, selectedTutorId, studyDay, isSpeakerOn } = get();

            // 세션 시작 API 호출
            const res = await studyApi.startSessionMode({
                planId,
                sessionMode: mode,
                personaName: selectedTutorId.toUpperCase(),
                dayCount: studyDay,
                needsTts: isSpeakerOn
            });

            // AI 메시지 추가 (이미지 포함)
            set((state) => ({
                messages: [...state.messages, { 
                    type: 'AI', 
                    content: res.aiMessage, 
                    audioUrl: res.audioUrl,
                    imageUrl: res.imageUrl // [New] 세션별 이미지 URL 저장
                }],
                isChatLoading: false
            }));

        } catch (error) {
            console.error(`세션(${mode}) 멘트 로드 실패:`, error);
            set({ isChatLoading: false });
        }
    }
  },

  // 7. 타이머 틱 (1초 감소)
  tick: () => {
    const { timeLeft, nextSessionStep } = get();
    if (timeLeft > 0) {
      set({ timeLeft: timeLeft - 1 });
    } else {
      // 시간이 다 되면 다음 단계로
      nextSessionStep();
    }
  },

  // 8. 다음 단계로 자동 전환
  nextSessionStep: () => {
    const { currentStepIndex, sessionSchedule } = get();
    const nextIndex = currentStepIndex + 1;

    if (nextIndex < SESSION_SEQUENCE.length) {
      const nextMode = SESSION_SEQUENCE[nextIndex];
      set({ currentStepIndex: nextIndex });
      
      // 다음 모드 설정 (이때 AI 멘트를 요청하도록 true 전달)
      get().setupMode(nextMode, sessionSchedule, true);

    } else {
      // 모든 시퀀스 종료
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