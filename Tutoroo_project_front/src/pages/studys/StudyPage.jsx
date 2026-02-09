/** @jsxImportSource @emotion/react */
import { useState, useEffect, useRef } from "react";
import Header from "../../components/layouts/Header";
import SessionStatus from "../../components/studys/SessionStatus"; 
import useStudyStore from "../../stores/useStudyStore";
import { studyApi } from "../../apis/studys/studysApi"; 
import * as s from "./styles";

// 분리된 컴포넌트들
import ChatMessage from "./ChatMessage";
import ChatInput from "./ChatInput";
import FeedbackForm from "./FeedbackForm";

// 이미지 소스 (기존과 동일)
import tigerImg from "../../assets/images/mascots/logo_tiger.png";
import turtleImg from "../../assets/images/mascots/logo_turtle.png";
import rabbitImg from "../../assets/images/mascots/logo_rabbit.png";
import kangarooImg from "../../assets/images/mascots/logo_icon.png";
import dragonImg from "../../assets/images/mascots/logo_dragon.png";

const TUTOR_IMAGES = { tiger: tigerImg, turtle: turtleImg, rabbit: rabbitImg, kangaroo: kangarooImg, eastern_dragon: dragonImg, dragon: dragonImg };
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "";

function StudyPage() {
  const store = useStudyStore();
  const [inputText, setInputText] = useState("");
  const [isRecording, setIsRecording] = useState(false); 
  const [chatImageFile, setChatImageFile] = useState(null);
  const [chatImagePreview, setChatImagePreview] = useState(null);
  
  const scrollRef = useRef(null);
  const audioRef = useRef(new Audio());
  const mediaRecorderRef = useRef(null);
  const audioChunksRef = useRef([]);

  const currentTutorImage = TUTOR_IMAGES[store.selectedTutorId] || kangarooImg;

  // 초기화 및 정리
  useEffect(() => {
    store.initializeStudySession();
    return () => {
      audioRef.current?.pause();
      if (mediaRecorderRef.current?.state === "recording") mediaRecorderRef.current.stop();
    };
  }, []);

  // 이미지 소스 처리 함수 (유지)
  const getImageSource = (url) => {
    if (!url) return null;
    if (url.startsWith("http")) return url;
    if (url.includes('/tutors/')) {
        const filename = url.split('/').pop().split('.')[0].toLowerCase();
        return TUTOR_IMAGES[filename] || kangarooImg;
    }
    const cleanBase = API_BASE_URL.replace(/\/$/, ""); 
    const cleanUrl = url.startsWith("/") ? url : `/${url}`; 
    return `${cleanBase}${cleanUrl}`;
  };

  // 녹음 관련 핸들러 (유지)
  const startRecording = async () => { /* 기존 코드와 동일 */ };
  const stopRecording = () => { if (mediaRecorderRef.current && isRecording) mediaRecorderRef.current.stop(); };

  const handleSend = () => {
    if ((!inputText.trim() && !chatImageFile) || store.isChatLoading) return;
    store.sendMessage(inputText, chatImageFile);
    setInputText(""); setChatImageFile(null); setChatImagePreview(null);
  };

  return (
    <>
      <Header />
      <div css={s.pageContainer}>
        <main css={s.chatArea} ref={scrollRef}>
          {store.messages.length === 0 ? (
            <div css={s.placeholder}><p>학습 정보를 불러오는 중입니다...</p></div>
          ) : (
            store.messages.map((msg, index) => (
              <ChatMessage 
                key={index} 
                msg={msg} 
                isUser={msg.type === "USER"} 
                currentTutorImage={currentTutorImage}
                getImageSource={getImageSource}
                onOptionClick={(opt) => setInputText(opt)}
              />
            ))
          )}
          {(store.isChatLoading || isRecording) && (
            <div css={s.messageRow(false)}>
              <div css={s.aiProfileIcon}><img src={currentTutorImage} alt="tutor" /></div>
              <div css={s.bubble(false)}>
                {isRecording ? <span css={s.recordingPulse}>🎤 듣고 있어요...</span> : <span className="dot-flashing">...</span>}
              </div>
            </div>
          )}
        </main>
        
        <footer css={s.bottomArea}>
          <div css={s.bottomInner}>
            <SessionStatus />
            {store.currentMode === 'STUDENT_FEEDBACK' ? (
              <FeedbackForm isChatLoading={store.isChatLoading} />
            ) : (
              <ChatInput 
                inputText={inputText} setInputText={setInputText}
                handleSend={handleSend} handleKeyDown={(e) => e.key === "Enter" && handleSend()}
                isSpeakerOn={store.isSpeakerOn} toggleSpeaker={store.toggleSpeaker}
                isRecording={isRecording} startRecording={startRecording} stopRecording={stopRecording}
                chatImageFile={chatImageFile} setChatImageFile={setChatImageFile}
                chatImagePreview={chatImagePreview} setChatImagePreview={setChatImagePreview}
                isChatLoading={store.isChatLoading} currentMode={store.currentMode}
              />
            )}
          </div>
        </footer>
      </div>
    </>
  );
}

export default StudyPage;