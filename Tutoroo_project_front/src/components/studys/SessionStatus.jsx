/** @jsxImportSource @emotion/react */
import { useEffect } from "react";
import useStudyStore, { SESSION_MODES } from "../../stores/useStudyStore";
import * as s from "./styles";

function SessionStatus() {
  const { currentMode, timeLeft, tick, isTimerRunning } = useStudyStore();

  useEffect(() => {
    let interval = null;
    if (isTimerRunning && timeLeft > 0) {
      interval = setInterval(tick, 1000);
    }
    return () => clearInterval(interval);
  }, [isTimerRunning, timeLeft, tick]);

  const formatTime = (seconds) => {
    const m = Math.floor(seconds / 60).toString().padStart(2, "0");
    const s = seconds % 60;
    return `${m}:${s.toString().padStart(2, "0")}`;
  };

  const label = SESSION_MODES[currentMode]?.label || "학습";

  return (
    <div css={s.statusWidget}>
      <div css={s.statusLabel}>{label} 세션</div>
      <div css={s.timerText}>{formatTime(timeLeft)}</div>
    </div>
  );
}

export default SessionStatus;