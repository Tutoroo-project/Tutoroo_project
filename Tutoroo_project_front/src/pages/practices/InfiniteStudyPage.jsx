/** @jsxImportSource @emotion/react */
import { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
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

const TUTOR_IMAGES = {
    tiger: tigerImg,
    turtle: turtleImg,
    rabbit: rabbitImg,
    kangaroo: kangarooImg,
    eastern_dragon: dragonImg,
    dragon: dragonImg
};

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "";

function InfiniteStudyPage() {


    const setInfinitePractice = useStudyStore((s) => s.setInfinitePractice);

    useEffect(() => {
        setInfinitePractice(true);
        return () => setInfinitePractice(false); // 다른 페이지에 영향 없게 원복
    }, [setInfinitePractice]);
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

    // [수정] 페이지 진입 및 이탈 시 처리
    useEffect(() => {
        // 1. 세션 초기화 (Store에 메시지가 있으면 무시됨 -> 이어하기)
        initializeStudySession();

        // 2. 페이지를 떠날 때(대시보드 이동 등) 오디오/녹음만 중지하고 상태는 유지
        return () => {
            if (audioRef.current) {
                audioRef.current.pause();
                audioRef.current.currentTime = 0;
            }
            if (mediaRecorderRef.current && mediaRecorderRef.current.state === "recording") {
                mediaRecorderRef.current.stop();
            }
        };
    }, [initializeStudySession]);

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

    const handleSend = () => {
        if (!inputText.trim() || isChatLoading) return;
        sendMessage(inputText);
        setInputText("");
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
                                {isSpeakerOn ? <HiMiniSpeakerWave /> : <HiMiniSpeakerXMark />}
                            </button>
                            <button
                                css={s.iconBtn(isRecording)}
                                onMouseDown={startRecording} onMouseUp={stopRecording}
                                onTouchStart={startRecording} onTouchEnd={stopRecording}
                            >
                                {isRecording ? <FaCircle /> : <PiMicrophoneStageFill />}
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

export default InfiniteStudyPage;