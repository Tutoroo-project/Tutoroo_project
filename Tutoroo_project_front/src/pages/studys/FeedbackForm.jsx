/** @jsxImportSource @emotion/react */
import { useState } from "react";
import * as s from "./styles";
import useStudyStore from "../../stores/useStudyStore";

const FeedbackForm = ({ isChatLoading }) => {
  const [localRating, setLocalRating] = useState(0);
  const [localFeedback, setLocalFeedback] = useState("");

  const handleSubmit = () => {
    if (localRating === 0) return alert("별점을 선택해주세요!");
    useStudyStore.setState({ studentRating: localRating, studentFeedbackText: localFeedback });
    // store의 submit 함수 호출 로직 추가 가능
  };

  return (
    <div css={s.feedbackContainer}>
      <div css={s.feedbackSection}>
        <p css={s.feedbackLabel}>오늘 수업은 어떠셨나요?</p>
        <div css={s.starContainer}>
          {[1, 2, 3, 4, 5].map((star) => (
            <span key={star} css={s.star(star <= localRating)} onClick={() => setLocalRating(star)}>★</span>
          ))}
        </div>
        <textarea
          css={s.feedbackTextarea}
          placeholder="선생님께 하고 싶은 말을 남겨주세요"
          value={localFeedback}
          onChange={(e) => setLocalFeedback(e.target.value)}
          rows={4}
        />
        <button css={s.submitFeedbackBtn} onClick={handleSubmit} disabled={isChatLoading}>
          평가 제출
        </button>
      </div>
    </div>
  );
};

export default FeedbackForm;