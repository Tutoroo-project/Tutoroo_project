/** @jsxImportSource @emotion/react */
import { useState, useEffect, useRef } from "react";
import Header from "../../components/layouts/Header";
import SessionStatus from "../../components/studys/SessionStatus"; 
import useStudyStore from "../../stores/useStudyStore";
import * as s from "./styles";

function StudyPage() {
  const { messages, sendMessage, isChatLoading } = useStudyStore();
  const [inputText, setInputText] = useState("");
  const scrollRef = useRef(null);

  // 메시지 올 때마다 스크롤 맨 아래로 이동
  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages, isChatLoading]);

  const handleSend = () => {
    if (!inputText.trim()) return;
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
        {/* 채팅 로그 영역 */}
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
                  {/* AI면 프로필 아이콘 표시 */}
                  {!isUser && <div css={s.aiProfileIcon} />} 
                  
                  <div css={s.bubble(isUser)}>
                    {msg.content}
                  </div>
                </div>
              );
            })
          )}
          
          {/* AI가 생각 중일 때 표시 */}
          {isChatLoading && (
            <div css={s.messageRow(false)}>
              <div css={s.aiProfileIcon} />
              <div css={s.bubble(false)}>...</div>
            </div>
          )}
        </main>

        {/* 하단 입력 영역 */}
        <footer css={s.bottomArea}>
            <div css={s.bottomInner}>
                {/* [왼쪽 하단] 타이머 및 세션 상태 */}
                <SessionStatus />

                {/* 채팅 입력창 */}
                <div css={s.inputWrapper}>
                    <input 
                      type="text" 
                      placeholder="AI 튜터에게 질문해보세요." 
                      css={s.inputBox}
                      value={inputText}
                      onChange={(e) => setInputText(e.target.value)}
                      onKeyDown={handleKeyDown}
                      disabled={isChatLoading}
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