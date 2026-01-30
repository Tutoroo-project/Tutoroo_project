/** @jsxImportSource @emotion/react */
import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import useStudyStore from "../../stores/useStudyStore";
import * as s from "./styles";
import tigerImg from "../../assets/images/mascots/logo_tiger.png";
import turtleImg from "../../assets/images/mascots/logo_turtle.png";
import rabbitImg from "../../assets/images/mascots/logo_rabbit.png";
import kangarooImg from "../../assets/images/mascots/logo_icon.png";
import dragonImg from "../../assets/images/mascots/logo_dragon.png";

const TUTORS = [
  { id: "TIGER", name: "호랑이 선생님", image: tigerImg, desc: <>엄격하고 카리스마 있는 스파르타 스타일!<br/> 딴짓은 용납 못해요.</> },
  { id: "TURTLE", name: "거북이 선생님", image: turtleImg, desc: <>천천히, 하지만 확실하게!<br/> 이해할 때까지 친절하게 반복해줘요.</> },
  { id: "RABBIT", name: "토끼 선생님", image: rabbitImg, desc: <>빠르고 효율적인 핵심 요약!<br/> 급한 시험 대비에 딱이에요.</> },
  { id: "KANGAROO", name: "캥거루 선생님", image: kangarooImg, desc: <>주머니에서 꿀팁이 쏟아져요!<br/> 실전 예제 위주의 수업.</> },
  { id: "DRAGON", name: "용 선생님", image: dragonImg, desc: <>깊은 깨달음을 주는 현자 스타일.<br/> 하오체를 사용해요.</> },
];

const TutorSelectionPage = () => {
  const navigate = useNavigate();
  
  // store에서 messages와 기타 상태 가져오기
  const { 
      studyDay, loadUserStatus, startClassSession, isLoading, planId,
      todayTopic, isStudyCompletedToday, messages 
  } = useStudyStore();
  
  const [activeTutorId, setActiveTutorId] = useState("TIGER");
  const [isCustomMode, setIsCustomMode] = useState(false);
  const [customInput, setCustomInput] = useState("");

  // [New] 진행 중인 학습(메시지)이 있으면 튜터 선택 건너뛰기
  useEffect(() => {
    // 메시지가 있다는 것은 이미 세션이 시작되었다는 의미이므로 바로 이동
    if (messages && messages.length > 0) {
        navigate("/study", { replace: true }); // 뒤로가기 방지를 위해 replace 사용
    }
  }, [messages, navigate]);

  useEffect(() => {
    // planId가 있다면 상태 로드
    // (단, 위 useEffect에 의해 messages가 있으면 리다이렉트가 먼저 발생함)
    if (planId) {
        loadUserStatus(planId);
    } else {
        loadUserStatus();
    }
  }, [loadUserStatus, planId]);

  const activeTutor = TUTORS.find((t) => t.id === activeTutorId);
  const isDayOne = studyDay === 1;

  const handleTutorClick = (id) => {
    setActiveTutorId(id);
    if (isCustomMode) setIsCustomMode(false);
  };

  const handleToggleCustom = () => {
    if (isDayOne) {
      alert(" 커스텀 선생님은 학습 2일차부터 선택할 수 있습니다!\n1일차는 기본 선생님과 함께 기초를 다져보세요.");
      return;
    }
    setIsCustomMode((prev) => !prev);
  };

  const handleStart = () => {
    // 오늘 학습 완료 여부 체크
    if (isStudyCompletedToday) {
        alert("오늘 학습을 이미 완료하셨습니다. 내일 다시 도전해주세요!");
        return;
    }
    if (isLoading) return;

    const tutorInfo = {
        id: activeTutorId,
        isCustom: isCustomMode,
        customRequirement: isCustomMode ? customInput : null
    };

    startClassSession(tutorInfo, navigate);
  };

  // 버튼 렌더링 헬퍼 함수
  const renderStartButton = () => {
    if (isStudyCompletedToday) {
        return (
            <button css={s.startBtn} disabled style={{ backgroundColor: '#999', cursor: 'default' }}>
                🎉 오늘 학습 완료! (내일 00시 오픈)
            </button>
        );
    }
    return (
        <button css={s.startBtn} onClick={handleStart} disabled={isLoading}>
            {isLoading ? "로딩 중..." : "수업 시작하기"}
        </button>
    );
  };

  return (
    <div css={s.container}>
      <h2 css={s.title}>
        {todayTopic ? `Day ${studyDay}. ${todayTopic}` : `오늘 함께할 선생님을 선택해주세요 (${studyDay}일차)`}
      </h2>

      <div css={s.contentWrap}>
        <div css={s.listPanel}>
          {TUTORS.map((tutor) => (
            <div
              key={tutor.id}
              css={s.tutorItem(activeTutorId === tutor.id)}
              onClick={() => handleTutorClick(tutor.id)}
            >
              <img src={tutor.image} alt={tutor.name} className="profile" />
              <div className="name">{tutor.name}</div>
              <div className="arrow">›</div>
            </div>
          ))}

          <div 
            css={[s.customBtn(isCustomMode), isDayOne && s.disabledBtn]} 
            onClick={handleToggleCustom}
          >
            <div className="name">
              {isDayOne ? " 커스텀 설정 (2일차부터 가능)" : " 커스텀 설정으로 변경"}
            </div>
          </div>
        </div>

        <div css={s.detailPanel}>
          {isCustomMode ? (
            <div css={s.infoBox}>
              <h3>커스텀 선생님 설정</h3>
              <p css={s.guideText}>
                선택한 <strong>{activeTutor.name}</strong>의 성격에<br/>
                원하는 특징을 추가하여 수업을 진행합니다.
              </p>
              <textarea
                css={s.customInput}
                placeholder="예: 사투리를 써줘, 칭찬을 많이 해줘 등"
                value={customInput}
                onChange={(e) => setCustomInput(e.target.value)}
              />
              {renderStartButton()}
            </div>
          ) : (
            <div css={s.infoBox}>
              <img src={activeTutor.image} alt={activeTutor.name} css={s.detailProfileImg} />
              
              <p css={s.guideText}>
                {isStudyCompletedToday ? (
                    <strong>오늘의 목표를 달성했습니다!<br/>푹 쉬고 내일 만나요.</strong>
                ) : (
                    <>
                    선택한 <strong>{activeTutor.name}</strong>과 함께<br/>
                    즐거운 학습을 시작해보세요!
                    </>
                )}
              </p>
              
              <div css={s.descBox}>
                <strong>[ {activeTutor.name} ]</strong>
                <p>{activeTutor.desc}</p>
              </div>
              {renderStartButton()}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default TutorSelectionPage;