/** @jsxImportSource @emotion/react */
import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import Swal from "sweetalert2";

import Header from "../../components/layouts/Header";
import * as s from "./styles";

import useLevelTestStore from "../../stores/useLevelTestStore";
import {
  consultAssessment,
  generateRoadmap,
} from "../../apis/assessments/assessmentApi";

// 백엔드 프롬프트 포맷에 맞춰 history role을 "User" / "AI"로 맞추는 게 안전
const ROLE = {
  USER: "User",
  AI: "AI",
};

function mapLevelToUi(level) {
  // 백엔드: BEGINNER / INTERMEDIATE / ADVANCED
  // 결과페이지가 Lv.{level} 형식이라 일단 숫자로 매핑 (원하면 문자열로 바꿔도 됨)
  if (level === "BEGINNER") return 1;
  if (level === "INTERMEDIATE") return 2;
  if (level === "ADVANCED") return 3;
  return level ?? null;
}

function LevelTestPage() {
  const navigate = useNavigate();

  const fileInputRef = useRef(null);

  const studyInfo = useLevelTestStore((st) => st.studyInfo);
  const setResult = useLevelTestStore((st) => st.setResult);

  // UI용 메시지(기존 유지)
  const [messages, setMessages] = useState([
    { role: "ai", content: "수준 파악을 시작하겠습니다." },
  ]);
  const [input, setInput] = useState("");

  //  백엔드로 보내는 history (User/AI 누적)
  const [history, setHistory] = useState([]);

  const [isCompleted, setIsCompleted] = useState(false);
  const [isConsulting, setIsConsulting] = useState(false);
  const [isGenerating, setIsGenerating] = useState(false);

  // 처음 진입 시: studyInfo 없으면 되돌리기 + consult 1회 시작
  useEffect(() => {
    if (!studyInfo?.goal || !studyInfo?.availableTime || !studyInfo?.deadline) {
      Swal.fire({
        icon: "warning",
        title: "학습 정보가 없어요",
        text: "먼저 '학습 추가'에서 과목/시간/기간을 입력해주세요.",
        confirmButtonColor: "#FF8A3D",
      }).then(() => navigate("/"));
      return;
    }

    (async () => {
      setIsConsulting(true);
      try {
        const res = await consultAssessment({
          studyInfo,
          history: [],
          lastUserMessage: null, // 첫 질문 유도
        });

        setMessages((prev) => [
          ...prev,
          { role: "ai", content: res.aiMessage },
        ]);
        setHistory([{ role: ROLE.AI, content: res.aiMessage }]);
      } catch (e) {
        Swal.fire({
          icon: "error",
          title: "AI 질문 시작 실패",
          text: "잠시 후 다시 시도해주세요.",
          confirmButtonColor: "#FF8A3D",
        });
      } finally {
        setIsConsulting(false);
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleGenerate = async (finalHistory) => {
    const resolveRoadmapUrl = (url) => {
      if (!url) return null;
      // 이미 http(s)면 그대로
      if (/^https?:\/\//i.test(url)) return url;

      // 상대경로면 API base 붙이기
      const base = import.meta.env.VITE_API_BASE_URL ?? "";
      try {
        return new URL(url, base).toString();
      } catch {
        const slashBase = base.endsWith("/") ? base.slice(0, -1) : base;
        const slashUrl = url.startsWith("/") ? url : `/${url}`;
        return `${slashBase}${slashUrl}`;
      }
    };
    setIsGenerating(true);
    try {
      const result = await generateRoadmap({
        studyInfo,
        history: finalHistory,
      });

      //  결과 페이지가 읽는 store에 매핑
      setResult({
        subject: studyInfo.goal,
        level: mapLevelToUi(result.analyzedLevel),
        summary: result.analysisReport ?? result.overview?.summary ?? null,
        roadmap: result.overview?.chapters ?? [],
        roadmapImageUrl: resolveRoadmapUrl(result.roadmapImageUrl),
      });

      // 안내 메시지(선택)
      if (result.message) {
        setMessages((prev) => [
          ...prev,
          { role: "ai", content: result.message },
        ]);
      }

      setIsCompleted(true);
    } catch (e) {
      const status = e?.response?.status;
      const data = e?.response?.data; // { timestamp, status, code, message, errors }
      const code = data?.code;
      const msg = data?.message;

      // 목표 생성 제한 (BASIC 1개 제한)
      if ((status === 400 || status === 402) && code === "L003") {
        Swal.fire({
          icon: "warning",
          title: "목표 생성 제한",
          text: msg ?? "BASIC 등급은 학습 목표를 1개만 설정할 수 있어요.",
          confirmButtonText: "대시보드로 이동",
          confirmButtonColor: "#FF8A3D",
        }).then(() => navigate("/dashboard"));
        return;
      }

      // 그 외 에러
      Swal.fire({
        icon: "error",
        title: "로드맵 생성 실패",
        text: msg ?? "로그인이 필요하거나(401), 서버 오류일 수 있어요.",
        confirmButtonColor: "#FF8A3D",
      });
    } finally {
      setIsGenerating(false);
    }
  };

  // 사용자 입력 전송 -> consult 반복
  const handleSubmit = async () => {
    if (!input.trim()) return;
    if (isConsulting || isGenerating || isCompleted) return;

    const userMsg = input.trim();

    // UI에 사용자 메시지
    setMessages((prev) => [...prev, { role: "user", content: userMsg }]);
    setInput("");

    setIsConsulting(true);
    try {
      //  현재 history(이전 턴들) + lastUserMessage(이번 입력)로 consult
      const res = await consultAssessment({
        studyInfo,
        history,
        lastUserMessage: userMsg,
      });

      // UI에 AI 메시지
      setMessages((prev) => [...prev, { role: "ai", content: res.aiMessage }]);

      //  history 갱신: (이번 userMsg) + (이번 aiMessage) 추가
      const nextHistory = [
        ...history,
        { role: ROLE.USER, content: userMsg },
        { role: ROLE.AI, content: res.aiMessage },
      ];
      setHistory(nextHistory);

      const hasUserAnswer = nextHistory.some((m) => m.role === ROLE.USER);
      if (res.isFinished && hasUserAnswer) {
        await handleGenerate(nextHistory);
      }
    } catch (e) {
      Swal.fire({
        icon: "error",
        title: "질문 진행 실패",
        text: "네트워크/서버 상태를 확인해주세요.",
        confirmButtonColor: "#FF8A3D",
      });
    } finally {
      setIsConsulting(false);
    }
  };

  // 파일 업로드 (유지)
  const handleFileUpload = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    e.target.value = "";
  };

  return (
    <>
      <Header />

      <div css={s.pageContainer}>
        <main css={s.chatArea}>
          {messages.map((msg, idx) => (
            <div key={idx} css={msg.role === "ai" ? s.aiBubble : s.userBubble}>
              {msg.content}
            </div>
          ))}
        </main>

        <footer css={s.bottomArea}>
          {isCompleted ? (
            <div css={s.resultFooter}>
              <button
                css={s.resultBtn}
                onClick={() => navigate("/level-test/result")}
              >
                결과 확인하기
              </button>
            </div>
          ) : (
            <div css={s.bottomInner}>
              <div css={s.inputWrapper}>
                <button
                  css={s.plusBtn}
                  type="button"
                  onClick={() => fileInputRef.current?.click()}
                  disabled={isConsulting || isGenerating}
                >
                  ＋
                </button>

                <input
                  css={s.inputBox}
                  value={input}
                  placeholder={
                    isConsulting ? "AI가 응답 중..." : "답변을 입력하세요."
                  }
                  onChange={(e) => setInput(e.target.value)}
                  disabled={isConsulting || isGenerating}
                  onKeyDown={(e) => {
                    if (e.key === "Enter" && !e.shiftKey) {
                      e.preventDefault();
                      handleSubmit();
                    }
                  }}
                />

                <input
                  type="file"
                  ref={fileInputRef}
                  hidden
                  onChange={handleFileUpload}
                />
              </div>

              <button
                css={s.sendBtn}
                onClick={handleSubmit}
                disabled={isConsulting || isGenerating}
              >
                {isGenerating ? "생성중..." : "전송"}
              </button>
            </div>
          )}
        </footer>
      </div>
    </>
  );
}

export default LevelTestPage;
