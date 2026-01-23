/** @jsxImportSource @emotion/react */
import { useState, useEffect, useRef } from "react";
import Header from "../../components/layouts/Header";
import SessionStatus from "../../components/studys/SessionStatus"; 
import useStudyStore from "../../stores/useStudyStore"; // SESSION_MODES는 store 내부에서 처리
import { studyApi } from "../../apis/studys/studysApi"; 
import * as s from "./styles";

import tigerImg from "../../assets/images/mascots/logo_tiger.png";
import turtleImg from "../../assets/images/mascots/logo_turtle.png";
import rabbitImg from "../../assets/images/mascots/logo_rabbit.png";
import kangarooImg from "../../assets/images/mascots/logo_icon.png";
import dragonImg from "../../assets/images/mascots/logo_dragon.png";

const TUTOR_IMAGES = {
  tiger: tigerImg,
  turtle: turtleImg,
  rabbit: rabbitImg,
  kangaroo: kangarooImg,
  eastern_dragon: dragonImg,
  dragon: dragonImg 
};

function StudyPage() {
  const { 
    messages, 
    sendMessage, 
    isChatLoading, 
    selectedTutorId,
    isSpeakerOn,
    toggleSpeaker,
    currentMode,
    planId,
    studyDay,
    setSessionMode // 테스트용으로 모드 변경이 필요할 수 있어 가져옴
  } = useStudyStore();

  const [inputText, setInputText] = useState("");
  const [isRecording, setIsRecording] = useState(false); 
  const scrollRef = useRef(null);
  const audioRef = useRef(new Audio());
  const mediaRecorderRef = useRef(null); 
  const audioChunksRef = useRef([]);

  const currentTutorImage = TUTOR_IMAGES[selectedTutorId] || tigerImg;

  // --- 1. 스크롤 자동 이동 ---
  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages, isChatLoading, isRecording]);

  // --- 2. TTS 자동 재생 ---
  useEffect(() => {
    if (messages.length > 0 && isSpeakerOn) {
      const lastMsg = messages[messages.length - 1];
      if (lastMsg.type === 'AI' && lastMsg.audioUrl) {
        audioRef.current.pause();
        audioRef.current.src = lastMsg.audioUrl;
        audioRef.current.play().catch(e => console.log("Audio play blocked:", e));
      }
    } else {
        audioRef.current.pause(); 
    }
  }, [messages, isSpeakerOn]);

  // --- 3. 메시지 전송 ---
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

  // --- 4. STT (음성 인식) ---
  const startRecording = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      mediaRecorderRef.current = new MediaRecorder(stream);
      audioChunksRef.current = [];

      mediaRecorderRef.current.ondataavailable = (event) => {
        if (event.data.size > 0) {
          audioChunksRef.current.push(event.data);
        }
      };

      mediaRecorderRef.current.onstop = async () => {
        const audioBlob = new Blob(audioChunksRef.current, { type: "audio/mp3" });
        setIsRecording(false);
        
        try {
          const text = await studyApi.uploadAudio(audioBlob);
          if (text) {
              setInputText(text); 
          }
        } catch (e) {
            console.error("STT Error", e);
            alert("음성 인식에 실패했습니다.");
        }
        
        stream.getTracks().forEach(track => track.stop());
      };

      mediaRecorderRef.current.start();
      setIsRecording(true);

    } catch (e) {
      console.error("Mic Access Error", e);
      alert("마이크 접근 권한이 필요합니다.");
    }
  };

  const stopRecording = () => {
    if (mediaRecorderRef.current && isRecording) {
      mediaRecorderRef.current.stop();
    }
  };

  // --- 5. PDF 다운로드 ---
  const handleDownloadPdf = async () => {
    try {
        const blob = await studyApi.downloadReviewPdf(planId, studyDay);
        const url = window.URL.createObjectURL(new Blob([blob]));
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', `Study_Review_Day${studyDay}.pdf`);
        document.body.appendChild(link);
        link.click();
        link.remove();
    } catch (e) {
        alert("복습 자료를 다운로드할 수 없습니다.");
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
          
          {(isChatLoading || isRecording) && (
            <div css={s.messageRow(false)}>
              <div css={s.aiProfileIcon}>
                <img src={currentTutorImage} alt="tutor" />
              </div>
              <div css={s.bubble(false)}>
                {isRecording ? (
                    <span css={s.recordingPulse}>🎤 듣고 있어요...</span>
                ) : (
                    <span className="dot-flashing">...</span>
                )}
              </div>
            </div>
          )}
        </main>

        <footer css={s.bottomArea}>
            <div css={s.bottomInner}>
                <SessionStatus />

                <div css={s.controlToolbar}>
                    {/* 1. 스피커 토글 */}
                    <button 
                        css={s.iconBtn(isSpeakerOn)} 
                        onClick={toggleSpeaker}
                        title={isSpeakerOn ? "TTS 끄기" : "TTS 켜기"}
                    >
                        {isSpeakerOn ? "🔊" : "🔇"}
                    </button>

                    {/* 2. 마이크 (STT) */}
                    <button 
                        css={s.iconBtn(isRecording)} 
                        onMouseDown={startRecording}
                        onMouseUp={stopRecording}
                        onTouchStart={startRecording} 
                        onTouchEnd={stopRecording}
                        title="누르고 말하기"
                    >
                        {isRecording ? "🔴" : "🎤"}
                    </button>

                    {/* 3. 복습 자료 다운로드 (조건부 렌더링: REVIEW 모드일 때만 표시) */}
                    {currentMode === 'REVIEW' && (
                        <button 
                            css={s.textBtn} 
                            onClick={handleDownloadPdf}
                            disabled={isChatLoading} 
                        >
                            📄 자료 다운
                        </button>
                    )}
                    
                    {/* [개발용 임시 버튼] 테스트를 위해 강제로 REVIEW 모드로 전환하려면 주석 해제하세요 */}
                    {/* <button onClick={() => setSessionMode('REVIEW')} style={{fontSize: 10}}>복습모드(Dev)</button> */}
                </div>

                <div css={s.inputWrapper}>
                    <input 
                      type="text" 
                      placeholder={isRecording ? "말씀하시는 내용을 듣고 있습니다..." : "AI 튜터에게 질문해보세요."}
                      css={s.inputBox}
                      value={inputText}
                      onChange={(e) => setInputText(e.target.value)}
                      onKeyDown={handleKeyDown}
                      disabled={isChatLoading || isRecording}
                      autoFocus
                    />
                </div>
                <button 
                  css={s.sendBtn} 
                  onClick={handleSend}
                  disabled={isChatLoading || isRecording}
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