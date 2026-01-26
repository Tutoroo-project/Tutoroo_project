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

const TUTOR_IMAGES = {
  tiger: tigerImg,
  turtle: turtleImg,
  rabbit: rabbitImg,
  kangaroo: kangarooImg,
  eastern_dragon: dragonImg,
  dragon: dragonImg 
};

// [New] API 기본 URL 설정 (환경변수 없으면 로컬호스트 기본값)
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

  // 기본 이미지를 캥거루로 변경 (선택된 ID가 없을 경우 대비)
  const currentTutorImage = TUTOR_IMAGES[selectedTutorId] || kangarooImg;

  useEffect(() => {
    // startClassSession을 통해 이미 메시지가 로드된 상태라면 중복 호출 방지 로직이 Store 내부에 있음
    initializeStudySession();
  }, []); 

  // 스크롤 자동 이동
  useEffect(() => {
    if (scrollRef.current) scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
  }, [messages, isChatLoading, isRecording]);

  // [수정] TTS 자동 재생 로직 (경로 문제 해결)
  useEffect(() => {
    if (messages.length > 0 && isSpeakerOn) {
      const lastMsg = messages[messages.length - 1];
      
      // AI 메시지이고 오디오 URL이 있는 경우
      if (lastMsg.type === 'AI' && lastMsg.audioUrl) {
        audioRef.current.pause();
        
        // [핵심] URL이 http로 시작하지 않으면(상대경로면) 백엔드 주소를 붙여줌
        const fullUrl = lastMsg.audioUrl.startsWith("http") 
          ? lastMsg.audioUrl 
          : `${API_BASE_URL}${lastMsg.audioUrl}`;

        audioRef.current.src = fullUrl;
        
        // 브라우저 정책상 사용자 인터랙션 없이는 자동 재생이 막힐 수 있음 (예외처리)
        audioRef.current.play().catch(e => {
            console.log("Audio play blocked (user interaction needed):", e);
        });
      }
    } else {
        // 스피커가 꺼져있으면 재생 중단
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

  // [수정] STT 녹음 시작 (webm 포맷 적용)
  const startRecording = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      
      // 브라우저 호환성을 위해 webm 선호 (없으면 빈 문자열로 기본값 사용)
      const mimeType = MediaRecorder.isTypeSupported("audio/webm") ? "audio/webm" : "";
      mediaRecorderRef.current = new MediaRecorder(stream, { mimeType });
      
      audioChunksRef.current = [];
      
      mediaRecorderRef.current.ondataavailable = (event) => {
        if (event.data.size > 0) audioChunksRef.current.push(event.data);
      };
      
      mediaRecorderRef.current.onstop = async () => {
        // [핵심] mp3 -> webm으로 변경 (OpenAI Whisper 호환성 및 브라우저 지원 최적화)
        const audioBlob = new Blob(audioChunksRef.current, { type: "audio/webm" });
        setIsRecording(false);
        try {
          const text = await studyApi.uploadAudio(audioBlob);
          if (text) setInputText(text); 
        } catch (e) {
            console.error("STT Error", e);
            alert("음성 인식에 실패했습니다.");
        }
        // 녹음 종료 후 스트림 트랙 정지 (마이크 아이콘 꺼짐 처리)
        stream.getTracks().forEach(track => track.stop());
      };
      
      mediaRecorderRef.current.start();
      setIsRecording(true);
    } catch (e) {
      console.error("Mic access error:", e);
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
              <p>내 학습 정보를 불러오는 중입니다...</p>
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
                {isRecording ? <span css={s.recordingPulse}>🎤 듣고 있어요...</span> : <span className="dot-flashing">...</span>}
              </div>
            </div>
          )}
        </main>
        <footer css={s.bottomArea}>
            <div css={s.bottomInner}>
                <SessionStatus />
                <div css={s.controlToolbar}>
                    <button css={s.iconBtn(isSpeakerOn)} onClick={toggleSpeaker} title={isSpeakerOn ? "TTS 끄기" : "TTS 켜기"}>
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
                      placeholder={isRecording ? "말씀하시는 내용을 듣고 있습니다..." : "AI 튜터에게 질문해보세요."}
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