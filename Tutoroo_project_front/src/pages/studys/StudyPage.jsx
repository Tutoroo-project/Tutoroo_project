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
import { HiMiniSpeakerWave, HiMiniSpeakerXMark } from "react-icons/hi2";
import { FaCircle } from "react-icons/fa";
import { PiMicrophoneStageFill } from "react-icons/pi";
import { MdImage, MdClose } from "react-icons/md";

const TUTOR_IMAGES = {
  tiger: tigerImg,
  turtle: turtleImg,
  rabbit: rabbitImg,
  kangaroo: kangarooImg,
  eastern_dragon: dragonImg,
  dragon: dragonImg 
};

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "";

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
    initializeStudySession,
    currentTestQuestion,
    userTestAnswer,
    submitTest,
    studentRating,
    studentFeedbackText,
    nextSessionStep
  } = useStudyStore();

  const [inputText, setInputText] = useState("");
  const [isRecording, setIsRecording] = useState(false); 
  const [chatImageFile, setChatImageFile] = useState(null);
  const [chatImagePreview, setChatImagePreview] = useState(null);
  const [testImageFile, setTestImageFile] = useState(null);
  const [localRating, setLocalRating] = useState(0);
  const [localFeedback, setLocalFeedback] = useState("");
  
  const scrollRef = useRef(null);
  const audioRef = useRef(new Audio());
  const mediaRecorderRef = useRef(null); 
  const audioChunksRef = useRef([]);
  const chatImageInputRef = useRef(null);
  const testImageInputRef = useRef(null);

  const currentTutorImage = TUTOR_IMAGES[selectedTutorId] || kangarooImg;

  useEffect(() => {
    initializeStudySession();
    
    return () => {
        if (audioRef.current) {
            audioRef.current.pause();
            audioRef.current.currentTime = 0;
        }
        if (mediaRecorderRef.current && mediaRecorderRef.current.state === "recording") {
            mediaRecorderRef.current.stop();
        }
    };
  }, []); 

  useEffect(() => {
    if (scrollRef.current) scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
  }, [messages, isChatLoading, isRecording]);

  useEffect(() => {
    if (messages.length > 0 && isSpeakerOn) {
      const lastMsg = messages[messages.length - 1];
      
      if (lastMsg.type === 'AI' && lastMsg.audioUrl) {
        audioRef.current.pause();
        
        const fullUrl = lastMsg.audioUrl.startsWith("http") 
          ? lastMsg.audioUrl 
          : `${API_BASE_URL}${lastMsg.audioUrl}`;

        audioRef.current.src = fullUrl;
        audioRef.current.play().catch(e => {
            console.log("Audio play blocked:", e);
        });
      }
    } else {
        audioRef.current.pause(); 
    }
  }, [messages, isSpeakerOn]);

  useEffect(() => {
    if (chatImageFile) {
      const reader = new FileReader();
      reader.onloadend = () => {
        setChatImagePreview(reader.result);
      };
      reader.readAsDataURL(chatImageFile);
    } else {
      setChatImagePreview(null);
    }
  }, [chatImageFile]);

  const handleSend = () => {
    if ((!inputText.trim() && !chatImageFile) || isChatLoading) return;
    sendMessage(inputText, chatImageFile);
    setInputText("");
    setChatImageFile(null);
    setChatImagePreview(null);
  };

  const handleKeyDown = (e) => {
    if (e.key === "Enter" && !e.nativeEvent.isComposing) handleSend();
  };

  const getImageSource = (url) => {
    if (!url) return null;
    if (url.startsWith("http")) return url;
    if (url.includes('/tutors/')) {
        const filename = url.split('/').pop().split('.')[0].toLowerCase();
        return TUTOR_IMAGES[filename] || kangarooImg;
    }
    if (url.includes('break_time') || url.includes('quiz_bg')) {
        return currentTutorImage; 
    }
    const cleanBase = API_BASE_URL.replace(/\/$/, ""); 
    const cleanUrl = url.startsWith("/") ? url : `/${url}`; 
    return `${cleanBase}${cleanUrl}`;
  };

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
        window.URL.revokeObjectURL(url); 
    } catch (e) {
        console.error(e);
        alert("다운로드에 실패했습니다.");
    }
  };

  const handleTestSubmit = () => {
    if (!inputText.trim() && !testImageFile) {
        alert("답안을 입력하거나 이미지를 첨부해주세요.");
        return;
    }
    
    submitTest(inputText, testImageFile);
    setInputText("");
    setTestImageFile(null);
  };

  const handleFeedbackSubmit = () => {
    if (localRating === 0) {
        alert("별점을 선택해주세요!");
        return;
    }
    
    useStudyStore.setState({ 
        studentRating: localRating, 
        studentFeedbackText: localFeedback 
    });
    
    submitStudentFeedback();
  };

  const renderStars = () => {
    return (
      <div css={s.starContainer}>
        {[1, 2, 3, 4, 5].map((star) => (
          <span
            key={star}
            css={s.star(star <= localRating)}
            onClick={() => setLocalRating(star)}
          >
            ★
          </span>
        ))}
      </div>
    );
  };

  // ✅ 쉬는시간 여부 확인
  const isBreakTime = currentMode === 'BREAK';

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
              const imgSrc = getImageSource(msg.imageUrl);

              return (
                <div key={index} css={s.messageRow(isUser)}>
                  {!isUser && (
                    <div css={s.aiProfileIcon}>
                      <img src={currentTutorImage} alt="tutor" />
                    </div>
                  )} 
                  <div css={s.bubble(isUser)}>
                    {imgSrc && (
                        <img 
                            src={imgSrc} 
                            alt="session-visual" 
                            onError={(e) => e.target.style.display = 'none'} 
                        />
                    )}
                    {msg.hasImage && <span css={s.imageAttachedBadge}>📷 이미지 첨부됨</span>}
                    {msg.content}
                    
                    {msg.testData && msg.testData.options && (
                      <div css={s.testOptions}>
                        {msg.testData.options.map((option, idx) => (
                          <button
                            key={idx}
                            css={s.optionButton}
                            onClick={() => setInputText(option)}
                          >
                            {idx + 1}. {option}
                          </button>
                        ))}
                      </div>
                    )}
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

          {/* ✅ 쉬는시간 다음 세션 버튼 */}
          {isBreakTime && (
            <div css={s.breakButtonContainer}>
              <button 
                css={s.skipBreakButton}
                onClick={() => nextSessionStep()}
              >
                💨 쉬는시간 건너뛰기
              </button>
            </div>
          )}
        </main>
        
        <footer css={s.bottomArea}>
            <div css={s.bottomInner}>
                <SessionStatus />
                
                {currentMode === 'TEST' ? (
                  <>
                    <div css={s.controlToolbar}>
                        <button css={s.iconBtn(isSpeakerOn)} onClick={toggleSpeaker}>
                            {isSpeakerOn ? <HiMiniSpeakerWave /> : <HiMiniSpeakerXMark />}
                        </button>
                        <button 
                            css={s.textBtn} 
                            onClick={() => testImageInputRef.current?.click()}
                        >
                            📎 이미지
                        </button>
                        <input
                            type="file"
                            ref={testImageInputRef}
                            hidden
                            accept="image/*"
                            onChange={(e) => {
                                const file = e.target.files?.[0];
                                if (file) setTestImageFile(file);
                            }}
                        />
                        {testImageFile && (
                            <span css={s.fileInfo}>{testImageFile.name}</span>
                        )}
                    </div>
                    <div css={s.inputWrapper}>
                        <input 
                          type="text" 
                          placeholder="답안을 입력하세요"
                          css={s.inputBox}
                          value={inputText}
                          onChange={(e) => setInputText(e.target.value)}
                          disabled={isChatLoading}
                        />
                    </div>
                    <button 
                        css={s.sendBtn} 
                        onClick={handleTestSubmit} 
                        disabled={isChatLoading}
                    >
                        제출
                    </button>
                  </>
                ) : currentMode === 'STUDENT_FEEDBACK' ? (
                  <div css={s.feedbackContainer}>
                    <div css={s.feedbackSection}>
                        <p css={s.feedbackLabel}>오늘 수업은 어떠셨나요?</p>
                        {renderStars()}
                        <textarea
                            css={s.feedbackTextarea}
                            placeholder="선생님께 하고 싶은 말을 자유롭게 남겨주세요 (선택)"
                            value={localFeedback}
                            onChange={(e) => setLocalFeedback(e.target.value)}
                            rows={4}
                        />
                        <button 
                            css={s.submitFeedbackBtn} 
                            onClick={handleFeedbackSubmit}
                            disabled={isChatLoading}
                        >
                            평가 제출
                        </button>
                    </div>
                  </div>
                ) : (
                  <>
                    <div css={s.controlToolbar}>
                        <button css={s.iconBtn(isSpeakerOn)} onClick={toggleSpeaker}>
                            {isSpeakerOn ? <HiMiniSpeakerWave /> : <HiMiniSpeakerXMark />}
                        </button>
                        <button 
                            css={s.iconBtn(isRecording)} 
                            onMouseDown={startRecording} onMouseUp={stopRecording}
                            onTouchStart={startRecording} onTouchEnd={stopRecording}
                        >
                            {isRecording ? <FaCircle /> : <PiMicrophoneStageFill />}
                        </button>
                        <button 
                            css={s.iconBtn(!!chatImageFile)} 
                            onClick={() => chatImageInputRef.current?.click()}
                            title="이미지 첨부"
                        >
                            <MdImage />
                        </button>
                        <input
                            type="file"
                            ref={chatImageInputRef}
                            hidden
                            accept="image/*"
                            onChange={(e) => {
                                const file = e.target.files?.[0];
                                if (file) setChatImageFile(file);
                            }}
                        />
                        {currentMode === 'REVIEW' && (
                            <button css={s.textBtn} onClick={handleDownloadPdf} disabled={isChatLoading}>
                                📄 자료 다운
                            </button>
                        )}
                    </div>
                    
                    {chatImagePreview && (
                      <div css={s.imagePreviewContainer}>
                        <img src={chatImagePreview} alt="preview" css={s.imagePreview} />
                        <button 
                          css={s.removeImageBtn}
                          onClick={() => {
                            setChatImageFile(null);
                            setChatImagePreview(null);
                          }}
                        >
                          <MdClose />
                        </button>
                      </div>
                    )}
                    
                    <div css={s.inputWrapper}>
                        <input 
                          type="text" 
                          placeholder={isRecording ? "듣고 있습니다..." : chatImageFile ? "이미지에 대해 질문하세요" : "질문해보세요."}
                          css={s.inputBox}
                          value={inputText}
                          onChange={(e) => setInputText(e.target.value)}
                          onKeyDown={handleKeyDown}
                          disabled={isChatLoading || isRecording}
                          autoFocus
                        />
                    </div>
                    <button css={s.sendBtn} onClick={handleSend} disabled={isChatLoading || isRecording}>
                        전송
                    </button>
                  </>
                )}
            </div>
        </footer>
      </div>
    </>
  );
}

export default StudyPage;