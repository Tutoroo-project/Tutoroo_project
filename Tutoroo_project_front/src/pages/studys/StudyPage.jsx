/** @jsxImportSource @emotion/react */
import { useState, useEffect, useRef } from "react";
import Header from "../../components/layouts/Header";
import SessionStatus from "../../components/studys/SessionStatus"; 
import useStudyStore from "../../stores/useStudyStore";
import * as s from "./styles";

// [추가] 튜터 이미지 Import
import tigerImg from "../../assets/images/mascots/logo_tiger.png";
import turtleImg from "../../assets/images/mascots/logo_turtle.png";
import rabbitImg from "../../assets/images/mascots/logo_rabbit.png";
import kangarooImg from "../../assets/images/mascots/logo_icon.png";
import dragonImg from "../../assets/images/mascots/logo_dragon.png";

// [추가] ID와 이미지 매핑
const TUTOR_IMAGES = {
  tiger: tigerImg,
  turtle: turtleImg,
  rabbit: rabbitImg,
  kangaroo: kangarooImg,
  eastern_dragon: dragonImg,
  dragon: dragonImg // 예외 처리
};

function StudyPage() {
  // [수정] selectedTutorId 가져오기
  const { messages, sendMessage, isChatLoading, selectedTutorId } = useStudyStore();
  const [inputText, setInputText] = useState("");
  const scrollRef = useRef(null);
  const audioRef = useRef(new Audio());

  // 현재 튜터 이미지 결정
  const currentTutorImage = TUTOR_IMAGES[selectedTutorId] || tigerImg;

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages, isChatLoading]);

  // 오디오 자동 재생
  useEffect(() => {
    if (messages.length > 0) {
      const lastMsg = messages[messages.length - 1];
      if (lastMsg.type === 'AI' && lastMsg.audioUrl) {
        audioRef.current.pause();
        audioRef.current.src = lastMsg.audioUrl;
        audioRef.current.play().catch(e => console.log("Audio play blocked:", e));
      }
    }
  }, [messages]);

  const handleSend = () => {
    if (!inputText.trim() || isChatLoading) return;
    sendMessage(inputText);
    setInputText("");
  };

  const handleKeyDown = (e) => {
    if (e.key === "Enter" && !e.nativeEvent.isComposing) {
      handleSend();
    }
  };

  return (
    <>
      <Header />
      <div css={s.pageContainer}>
        <main css={s.chatArea} ref={scrollRef}>
          {messages.length === 0 ? (
            <div css={s.placeholder}>
              <p>수업 준비 중입니다...</p>
            </div>
          ) : (
            messages.map((msg, index) => {
              const isUser = msg.type === "USER";
              return (
                <div key={index} css={s.messageRow(isUser)}>
                  {/* [수정] AI 메시지일 때 튜터 이미지 표시 */}
                  {!isUser && (
                    <div css={s.aiProfileIcon}>
                      <img src={currentTutorImage} alt="tutor" />
                    </div>
                  )} 
                  
                  <div css={s.bubble(isUser)}>
                    {msg.content}
                  </div>
                </div>
              );
            })
          )}
          
          {isChatLoading && (
            <div css={s.messageRow(false)}>
              <div css={s.aiProfileIcon}>
                <img src={currentTutorImage} alt="tutor" />
              </div>
              <div css={s.bubble(false)}>
                <span className="dot-flashing">...</span>
              </div>
            </div>
          )}
        </main>

        <footer css={s.bottomArea}>
            <div css={s.bottomInner}>
                <SessionStatus />
                <div css={s.inputWrapper}>
                    <input 
                      type="text" 
                      placeholder="AI 튜터에게 질문해보세요." 
                      css={s.inputBox}
                      value={inputText}
                      onChange={(e) => setInputText(e.target.value)}
                      onKeyDown={handleKeyDown}
                      disabled={isChatLoading}
                      autoFocus
                    />
                </div>
                <button 
                  css={s.sendBtn} 
                  onClick={handleSend}
                  disabled={isChatLoading}
                >
                  전송
                </button>
            </div>
        </footer>
      </div>
    </>
  );
}

export default StudyPage;