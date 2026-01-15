/** @jsxImportSource @emotion/react */
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Header from "../../components/layouts/Header";
import * as s from "./styles";
import { useRef } from "react";

const QUESTIONS = [
  "학습할 과목을 입력해주세요. (예: Java, Python)",
  "이 과목을 얼마나 공부해보셨나요?",
  "간단한 문제를 풀어볼게요.\nJava에서 변수 선언 방법은?",
];

function LevelTestPage() {
  const navigate = useNavigate();

  const imageInputRef = useRef(null);
  const fileInputRef = useRef(null);

  const [showMenu, setShowMenu] = useState(false);
  const [isCompleted, setIsCompleted] = useState(false);
  const [messages, setMessages] = useState([
    { role: "ai", content: "수준 파악을 시작해볼게요 🙂" },
  ]);
  const [step, setStep] = useState(0);
  const [input, setInput] = useState("");

  // AI 질문 출력
  useEffect(() => {
    if (step < QUESTIONS.length) {
      setMessages((prev) => [
        ...prev,
        { role: "ai", content: QUESTIONS[step] },
      ]);
    }
  }, [step]);

  const handleImageUpload = (e) => {
  const file = e.target.files[0];
  if (!file) return;

  console.log("이미지 업로드:", file);
};

const handleFileUpload = (e) => {
  const file = e.target.files[0];
  if (!file) return;

  console.log("파일 업로드:", file);
};


  const handleSubmit = () => {
    if (!input.trim()) return;

    setMessages((prev) => [...prev, { role: "user", content: input }]);
    setInput("");
    setShowMenu(false);

    // 마지막 질문
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

    setStep((prev) => prev + 1);
  };

  return (
    <>
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

        {/* 하단 영역 */}
        <footer css={s.bottomArea}>
          {isCompleted ? (
            // ===== 레벨 테스트 완료 후 =====
            <div css={s.resultFooter}>
              <button
                css={s.resultBtn}
                onClick={() => navigate("/level-test/result")}
              >
                결과 확인하기
              </button>
            </div>
          ) : (
            // ===== 테스트 진행 중 =====
            <div css={s.bottomInner}>
              <div css={s.inputWrapper}>
                {/* + 버튼 */}
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

                    {/* hidden inputs */}
                    <input
                      type="file"
                      accept="image/*"
                      ref={imageInputRef}
                      hidden
                      onChange={handleImageUpload}
                    />

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
