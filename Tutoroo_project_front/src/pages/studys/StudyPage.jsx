/** @jsxImportSource @emotion/react */
import { useState, useEffect, useRef } from "react";
import Header from "../../components/layouts/Header";
import SessionStatus from "../../components/studys/SessionStatus"; 
import useStudyStore from "../../stores/useStudyStore";
import { studyApi } from "../../apis/studys/studysApi"; 
import * as s from "./styles";
import tigerImg from "../../assets/images/mascots/logo_tiger.png";
import turtleImg from "../../assets/images/mascots/logo_turtle.png";
import rabbitImg from "../../assets/images/mascots/logo_rabbit.png";
import kangarooImg from "../../assets/images/mascots/logo_icon.png";
import dragonImg from "../../assets/images/mascots/logo_dragon.png";

// 튜터 이미지 매핑 객체
const TUTOR_IMAGES = {
  tiger: tigerImg,
  turtle: turtleImg,
  rabbit: rabbitImg,
  kangaroo: kangarooImg,
  eastern_dragon: dragonImg,
  dragon: dragonImg 
};

// 백엔드 URL (환경변수 또는 기본값)
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

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
    initializeStudySession 
  } = useStudyStore();

  const [inputText, setInputText] = useState("");
  const [isRecording, setIsRecording] = useState(false); 
  const scrollRef = useRef(null);
  const audioRef = useRef(new Audio());
  const mediaRecorderRef = useRef(null); 
  const audioChunksRef = useRef([]);

  const currentTutorImage = TUTOR_IMAGES[selectedTutorId] || kangarooImg;

  useEffect(() => {
    initializeStudySession();
  }, []); 

  useEffect(() => {
    if (scrollRef.current) scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
  }, [messages, isChatLoading, isRecording]);

  useEffect(() => {
    if (messages.length > 0 && isSpeakerOn) {
      const lastMsg = messages[messages.length - 1];
      
      // AI 메시지이고 오디오 URL이 있는 경우 재생
      if (lastMsg.type === 'AI' && lastMsg.audioUrl) {
        audioRef.current.pause();
        
        const fullUrl = lastMsg.audioUrl.startsWith("http") 
          ? lastMsg.audioUrl 
          : `${API_BASE_URL}${lastMsg.audioUrl}`;

        audioRef.current.src = fullUrl;
        audioRef.current.play().catch(e => {
            console.log("Audio play blocked (user interaction needed):", e);
        });
      }
    } else {
        audioRef.current.pause(); 
    }
  }, [messages, isSpeakerOn]);

  const handleSend = () => {
    if (!inputText.trim() || isChatLoading) return;
    sendMessage(inputText);
    setInputText("");
  };

  const handleKeyDown = (e) => {
    if (e.key === "Enter" && !e.nativeEvent.isComposing) handleSend();
  };

  // [핵심 기능] 이미지 소스 결정 함수
  // 백엔드에서 온 URL 경로를 보고 프론트엔드 에셋으로 연결하거나 실제 URL로 변환
  const getImageSource = (url) => {
    if (!url) return null;

    // 1. 튜터 이미지인 경우 (/images/tutors/tiger.png) -> 로컬 에셋 사용
    if (url.includes('/tutors/')) {
        // 경로에서 파일명(tiger) 추출
        const filename = url.split('/').pop().split('.')[0].toLowerCase();
        // 매핑된 이미지가 있으면 반환, 없으면 캥거루 반환
        return TUTOR_IMAGES[filename] || kangarooImg;
    }

    // 2. 쉬는 시간, 퀴즈 등 기타 이미지는 에셋이 없으면 튜터 이미지로 대체 (깨짐 방지)
    // (물론 서버에 해당 파일들을 올렸다면 아래 로직이 정상 작동함)
    if (url.includes('break_time') || url.includes('quiz_bg')) {
        // 임시로 튜터 이미지 사용 (원하는 이미지가 있다면 assets에 추가 후 여기서 분기 처리)
        return currentTutorImage; 
    }

    // 3. 그 외의 경우 (사용자 업로드 등) -> 백엔드 URL 사용
    return url.startsWith("http") ? url : `${API_BASE_URL}${url}`;
  };

  // STT 녹음 시작
  const startRecording = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const mimeType = MediaRecorder.isTypeSupported("audio/webm") ? "audio/webm" : "";
      mediaRecorderRef.current = new MediaRecorder(stream, { mimeType });
      
      audioChunksRef.current = [];
      mediaRecorderRef.current.ondataavailable = (event) => {
        if (event.data.size > 0) audioChunksRef.current.push(event.data);
      };
      
      mediaRecorderRef.current.onstop = async () => {
        const audioBlob = new Blob(audioChunksRef.current, { type: "audio/webm" });
        setIsRecording(false);
        try {
          const text = await studyApi.uploadAudio(audioBlob);
          if (text) setInputText(text); 
        } catch (e) {
            alert("음성 인식에 실패했습니다.");
        }
        stream.getTracks().forEach(track => track.stop());
      };
      
      mediaRecorderRef.current.start();
      setIsRecording(true);
    } catch (e) {
      alert("마이크 접근 권한이 필요합니다.");
    }
  };

  const stopRecording = () => {
    if (mediaRecorderRef.current && isRecording) mediaRecorderRef.current.stop();
  };

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
        alert("다운로드에 실패했습니다.");
    }
  };

  return (
    <>
      <Header />
      <div css={s.pageContainer}>
        <main css={s.chatArea} ref={scrollRef}>
          {messages.length === 0 ? (
            <div css={s.placeholder}>
              <p>학습 정보를 불러오는 중입니다...</p>
            </div>
          ) : (
            messages.map((msg, index) => {
              const isUser = msg.type === "USER";
              // 이미지 소스 결정
              const imgSrc = getImageSource(msg.imageUrl);

              return (
                <div key={index} css={s.messageRow(isUser)}>
                  {!isUser && (
                    <div css={s.aiProfileIcon}>
                      <img src={currentTutorImage} alt="tutor" />
                    </div>
                  )} 
                  <div css={s.bubble(isUser)}>
                    {/* [이미지 렌더링] src가 유효할 때만 표시 */}
                    {imgSrc && (
                        <img 
                            src={imgSrc} 
                            alt="session-visual" 
                            style={{ 
                                maxWidth: '100%', 
                                borderRadius: '8px', 
                                marginBottom: '10px', 
                                display: 'block',
                                objectFit: 'contain'
                            }}
                            onError={(e) => e.target.style.display = 'none'} // 로딩 실패 시 숨김 처리
                        />
                    )}
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
                {isRecording ? <span css={s.recordingPulse}>🎤 듣고 있어요...</span> : <span className="dot-flashing">...</span>}
              </div>
            </div>
          )}
        </main>
        <footer css={s.bottomArea}>
            <div css={s.bottomInner}>
                <SessionStatus />
                <div css={s.controlToolbar}>
                    <button css={s.iconBtn(isSpeakerOn)} onClick={toggleSpeaker}>
                        {isSpeakerOn ? "🔊" : "🔇"}
                    </button>
                    <button 
                        css={s.iconBtn(isRecording)} 
                        onMouseDown={startRecording} onMouseUp={stopRecording}
                        onTouchStart={startRecording} onTouchEnd={stopRecording}
                    >
                        {isRecording ? "🔴" : "🎤"}
                    </button>
                    {currentMode === 'REVIEW' && (
                        <button css={s.textBtn} onClick={handleDownloadPdf} disabled={isChatLoading}>📄 자료 다운</button>
                    )}
                </div>
                <div css={s.inputWrapper}>
                    <input 
                      type="text" 
                      placeholder={isRecording ? "듣고 있습니다..." : "질문해보세요."}
                      css={s.inputBox}
                      value={inputText}
                      onChange={(e) => setInputText(e.target.value)}
                      onKeyDown={handleKeyDown}
                      disabled={isChatLoading || isRecording}
                      autoFocus
                    />
                </div>
                <button css={s.sendBtn} onClick={handleSend} disabled={isChatLoading || isRecording}>전송</button>
            </div>
        </footer>
      </div>
    </>
  );
}

export default StudyPage;