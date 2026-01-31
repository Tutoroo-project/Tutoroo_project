/** @jsxImportSource @emotion/react */
import { useEffect } from "react";
import useStudyStore, { SESSION_MODES } from "../../stores/useStudyStore";
import * as s from "./styles";

function SessionStatus() {
  const { currentMode, timeLeft, tick, isTimerRunning, isInfinitePractice } = useStudyStore();

  useEffect(() => {
    let interval = null;
    if (!isInfinitePractice) {
      if (isTimerRunning && timeLeft > 0) {
        interval = setInterval(tick, 1000);
      } else if (isTimerRunning && timeLeft === 0) {
        tick(); // 시간이 0이 되는 순간 다음 단계 트리거
      }
    }
    return () => clearInterval(interval);
  }, [isInfinitePractice, isTimerRunning, timeLeft, tick]);

  const formatTime = (seconds) => {
    if (seconds < 0) return "00:00";
    const min = Math.floor(seconds / 60).toString().padStart(2, "0"); 
    const sec = seconds % 60; 
    return `${min}:${sec.toString().padStart(2, "0")}`;
  };

  // 현재 모드에 맞는 라벨 가져오기 (없으면 기본값)
  const label = SESSION_MODES[currentMode]?.label || "학습 중";
  // 복습 모드 등 시간이 0으로 설정된 경우 타이머 숨김 처리
  const showTime = SESSION_MODES[currentMode]?.defaultTime > 0 || timeLeft > 0;

  return (
    <div css={s.statusWidget}>
      <div css={s.statusLabel}>{label}</div>
      <div css={s.timerText}>
         {isInfinitePractice ? "∞" : showTime ? formatTime(timeLeft) : "-"}
      </div>
    </div>
  );
}

export default SessionStatus;