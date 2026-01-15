/** @jsxImportSource @emotion/react */
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Header from "../../components/layouts/Header";
import * as s from "./styles";
import { useRef } from "react";

// 레벨 테스트 질문 목록 (임시) AI/API 연동되면 수정
const QUESTIONS = [
  "학습할 과목을 입력해주세요. (예: Java, Python)",
  "이 과목을 얼마나 공부해보셨나요?",
  "간단한 문제를 풀어볼게요.\nJava에서 변수 선언 방법은?",
];

// 채팅 형식 LevelTestPage
function LevelTestPage() {
  //Navigate 호출
  const navigate = useNavigate();

  // 파일 / 이미지 업로드용 ref
  const imageInputRef = useRef(null);
  const fileInputRef = useRef(null);

  const [showMenu, setShowMenu] = useState(false); // + 메뉴 열림 상태
  const [isCompleted, setIsCompleted] = useState(false); // 테스트 완료 여부

  // 채팅 메시지 목록 (AI 와 유저)
  const [messages, setMessages] = useState([
    { role: "ai", content: "수준 파악을 시작해볼게요 🙂" },
  ]);
  const [step, setStep] = useState(0); // 현재 질문 단계 (수정해야할수도 있음)
  const [input, setInput] = useState(""); // 입력창 값

  // AI 질문 출력
  useEffect(() => {
    if (step < QUESTIONS.length) {
      setMessages((prev) => [
        ...prev,
        { role: "ai", content: QUESTIONS[step] },
      ]);
    }
  }, [step]);

  // 이미지 업로드 핸들러 (사용자가 이미지를 업로드해야하는 경우가 있을때를 위해)
  const handleImageUpload = (e) => {
    const file = e.target.files[0];
    if (!file) return;

    console.log("이미지 업로드:", file);
  };

  // 파일 업로드 핸들러 (사용자가 파일 업로드해야하는 경우가 있을때를 위해)
  const handleFileUpload = (e) => {
    const file = e.target.files[0];
    if (!file) return;

    console.log("파일 업로드:", file);
  };

  // 사용자 입력 전송
  const handleSubmit = () => {
    if (!input.trim()) return;

    // 사용자 메시지 추가
    setMessages((prev) => [...prev, { role: "user", content: input }]);
    setInput("");
    setShowMenu(false);

    // 마지막 질문일 경우
    if (step === QUESTIONS.length - 1) {
      setMessages((prev) => [
        ...prev,
        {
          role: "ai",
          content:
            "레벨 테스트가 완료되었습니다 🎉\n결과를 확인하고 AI가 만들어준 로드맵을 확인해보세요!",
        },
      ]);
      setIsCompleted(true);
      return;
    }

    // 다음 질문으로 이동
    setStep((prev) => prev + 1);
  };

  return (
    <>
      {/* 공통으로 사용하는 헤더 */}
      <Header />

      <div css={s.pageContainer}>
        {/* 채팅 영역 */}
        <main css={s.chatArea}>
          {messages.map((msg, idx) => (
            <div key={idx} css={msg.role === "ai" ? s.aiBubble : s.userBubble}>
              {msg.content}
            </div>
          ))}
        </main>

        {/* 하단 영역 OR 결과 영역 */}
        <footer css={s.bottomArea}>
          {isCompleted ? (
            // 레벨 테스트 완료 후
            <div css={s.resultFooter}>
              <button
                css={s.resultBtn}
                onClick={() => navigate("/level-test/result")}
              >
                결과 확인하기
              </button>
            </div>
          ) : (
            // 테스트 진행 중
            <div css={s.bottomInner}>
              <div css={s.inputWrapper}>
                {/* + 버튼 (첨부 메뉴 토글 스위치)*/}
                <button
                  css={s.plusBtn}
                  onClick={() => setShowMenu((prev) => !prev)}
                >
                  ＋
                </button>

                {/* 입력창 */}
                <input
                  css={s.inputBox}
                  value={input}
                  placeholder="답변을 입력하세요."
                  onChange={(e) => setInput(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter" && !e.shiftKey) {
                      e.preventDefault();
                      handleSubmit();
                    }
                  }}
                />

                {/* + 메뉴 */}
                {showMenu && (
                  <div css={s.plusMenu}>
                    <button
                      css={s.menuItem}
                      onClick={() => imageInputRef.current.click()}
                    >
                      + Upload Picture
                    </button>

                    <button
                      css={s.menuItem}
                      onClick={() => fileInputRef.current.click()}
                    >
                      + Upload File
                    </button>

                    {/* hidden IMAGE inputs */}
                    <input
                      type="file"
                      accept="image/*"
                      ref={imageInputRef}
                      hidden
                      onChange={handleImageUpload}
                    />

                    {/* hidden FILE inputs */}
                    <input
                      type="file"
                      ref={fileInputRef}
                      hidden
                      onChange={handleFileUpload}
                    />
                  </div>
                )}
              </div>

              {/* 전송 버튼 */}
              <button css={s.sendBtn} onClick={handleSubmit}>
                전송
              </button>
            </div>
          )}
        </footer>
      </div>
    </>
  );
}

export default LevelTestPage;
