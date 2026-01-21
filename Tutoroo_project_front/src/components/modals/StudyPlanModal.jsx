/** @jsxImportSource @emotion/react */
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import Swal from "sweetalert2";

import * as s from "./styles";
import useModalStore from "../../stores/modalStore";
import useLevelTestStore from "../../stores/useLevelTestStore";
import logo from "../../assets/images/mascots/logo.png";

// 학습 추가(목표 입력) 모달
// - 과목(goal), 하루 공부 가능 시간(availableTime), 기간/마감(deadline)을 입력받아
// - LevelTestPage에서 /api/assessment/consult 시작할 수 있도록 store에 저장
function StudyPlanModal() {
  const navigate = useNavigate();

  const closeStudyPlan = useModalStore((state) => state.closeStudyPlan);
  const setStudyInfo = useLevelTestStore((state) => state.setStudyInfo);
  const reset = useLevelTestStore((state) => state.reset);

  const [goal, setGoal] = useState("");
  const [availableTime, setAvailableTime] = useState("");
  const [deadline, setDeadline] = useState("");

  const onSubmit = (e) => {
    e.preventDefault();

    if (!goal.trim() || !availableTime.trim() || !deadline.trim()) {
      Swal.fire({
        icon: "warning",
        title: "입력 오류",
        text: "과목, 하루 공부 가능 시간, 기간 모두 입력해주세요.",
        confirmButtonColor: "#FF8A3D",
      });
      return;
    }

    // 이전 결과/정보 초기화 후 새 studyInfo 세팅
    reset();
    setStudyInfo({
      goal: goal.trim(),
      availableTime: availableTime.trim(),
      deadline: deadline.trim(),
    });

    closeStudyPlan();
    navigate("/level-test");
  };

  return (
    <div css={s.overlay}>
      <div css={s.modal} onMouseDown={(e) => e.stopPropagation()}>
        <div css={s.header}>
          <div />
          <div css={s.logoWrap}>
            <img src={logo} alt="Tutoroo" css={s.logoImg} />
          </div>
          <button type="button" css={s.exitBtn} onClick={closeStudyPlan}>
            나가기 →
          </button>
        </div>

        <p css={s.description}>
          먼저 간단한 정보를 입력해주면, AI가 이어서 질문하며 수준을 파악해요.
        </p>

        <form css={s.form} onSubmit={onSubmit}>
          <label css={s.formLabel}>
            <span css={s.required}>*</span> 어떤 과목을 배우고 싶나요?
          </label>
          <input
            css={s.input}
            value={goal}
            onChange={(e) => setGoal(e.target.value)}
            placeholder="예) Java, Python, 토익"
          />

          <label css={s.formLabel}>
            <span css={s.required}>*</span> 하루에 얼마나 공부할 수 있나요?
          </label>
          <input
            css={s.input}
            value={availableTime}
            onChange={(e) => setAvailableTime(e.target.value)}
            placeholder="예) 1시간, 2시간, 30분"
          />

          <label css={s.formLabel}>
            <span css={s.required}>*</span> 기간은 얼마나 계획하고 있나요?
          </label>
          <input
            css={s.input}
            value={deadline}
            onChange={(e) => setDeadline(e.target.value)}
            placeholder="예) 2주, 1개월, 3개월"
          />

          <button css={s.submitBtn} type="submit">
            다음 (AI 질문 시작)
          </button>
        </form>
      </div>
    </div>
  );
}

export default StudyPlanModal;
