/** @jsxImportSource @emotion/react */
import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import Header from "../../components/layouts/Header";
import useStudyStore from "../../stores/useStudyStore";
import * as s from "./styles";

import tigerImg from "../../assets/images/mascots/logo_tiger.png";
import turtleImg from "../../assets/images/mascots/logo_turtle.png";
import rabbitImg from "../../assets/images/mascots/logo_rabbit.png";
import kangarooImg from "../../assets/images/mascots/logo_icon.png";
import dragonImg from "../../assets/images/mascots/logo_dragon.png";

const TUTORS = [
  { id: "tiger", name: "호랑이 선생님", image: tigerImg, desc: "엄격하고 카리스마 있는 스파르타 스타일! 딴짓은 용납 못해요." },
  { id: "turtle", name: "거북이 선생님", image: turtleImg, desc: "천천히, 하지만 확실하게! 이해할 때까지 친절하게 반복해줘요." },
  { id: "rabbit", name: "토끼 선생님", image: rabbitImg, desc: "빠르고 효율적인 핵심 요약! 급한 시험 대비에 딱이에요." },
  { id: "kangaroo", name: "캥거루 선생님", image: kangarooImg, desc: "주머니에서 꿀팁이 쏟아져요! 실전 예제 위주의 수업." },
  { id: "dragon", name: "드래곤 선생님", image: dragonImg, desc: "방대한 지식의 수호자. 깊이 있는 원리 이해를 도와줘요." },
];

function TutorSelectionPage() {
  const navigate = useNavigate();
  const { studyDay, loadUserStatus, selectedTutorId, setTutorId, startStudyPlan } = useStudyStore();
  
  const [isCustomMode, setIsCustomMode] = useState(false);
  const [customInput, setCustomInput] = useState("");

  useEffect(() => {
    loadUserStatus();
    setTutorId("kangaroo");
    setIsCustomMode(false);
    
  }, [loadUserStatus, setTutorId]); // 의존성 배열에 함수 추가

  const activeTutor = TUTORS.find((t) => t.id === selectedTutorId) || TUTORS[3];

  const handleStart = () => {
    startStudyPlan(isCustomMode ? customInput : null, navigate);
  };

  return (
    <>
      <Header />
      <div css={s.container}>
        <h1 css={s.title}>AI 튜터 선택</h1>

        <div css={s.contentWrap}>
          {/* [좌측] 튜터 리스트 */}
          <div css={s.listPanel}>
            {TUTORS.map((tutor) => (
              <button
                key={tutor.id}
                css={s.tutorItem(selectedTutorId === tutor.id && !isCustomMode)}
                onClick={() => {
                  setTutorId(tutor.id);
                  setIsCustomMode(false);
                }}
              >
                <img src={tutor.image} alt={tutor.name} className="icon" />
                <span className="name">{tutor.name}</span>
                <span className="arrow">›</span>
              </button>
            ))}

            {studyDay >= 2 && (
              <button 
                css={s.customBtn(isCustomMode)}
                onClick={() => setIsCustomMode(true)}
              >
                커스텀 선생님 생성 +
              </button>
            )}
          </div>

          {/* [우측] 상세 설명 패널 */}
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
                <button css={s.startBtn} onClick={handleStart}>
                  이 설정으로 시작하기
                </button>
              </div>
            ) : (
              <div css={s.infoBox}>
                <img src={activeTutor.image} alt={activeTutor.name} css={s.detailProfileImg} />
                
                <p css={s.guideText}>
                  선택한 <strong>{activeTutor.name}</strong>과 함께<br/>
                  즐거운 학습을 시작해보세요!
                </p>
                <div css={s.descBox}>
                  <strong>[ {activeTutor.name} ]</strong>
                  <p>{activeTutor.desc}</p>
                </div>
                <button css={s.startBtn} onClick={handleStart}>
                  수업 시작하기
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </>
  );
}

export default TutorSelectionPage;