/** @jsxImportSource @emotion/react */
import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import Header from "../../components/layouts/Header";
import useStudyStore from "../../stores/useStudyStore";
import { practiceApi } from "../../apis/practices/practiceApi";
import * as s from "./styles";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "";

const QUESTION_TYPE_LABEL = {
  MULTIPLE_CHOICE: "객관식",
  SHORT_ANSWER: "단답형",
  LONG_ANSWER: "서술형",
  CODE_FILL_IN: "코드 빈칸",
  CODE_IMPLEMENTATION: "코드 구현",
  DRAWING_SUBMISSION: "그림 제출",
  AUDIO_RECORDING: "음성 녹음",
  VIDEO_SUBMISSION: "영상 제출",
};

function normalizeMediaUrl(url) {
  if (!url) return null;
  if (url.startsWith("http")) return url;
  const cleanBase = API_BASE_URL.replace(/\/$/, "");
  const cleanUrl = url.startsWith("/") ? url : `/${url}`;
  return `${cleanBase}${cleanUrl}`;
}

function InfiniteStudyPage() {
  const navigate = useNavigate();

  const planId = useStudyStore((st) => st.planId);
  const studyGoal = useStudyStore((st) => st.studyGoal);
  const setInfinitePractice = useStudyStore((st) => st.setInfinitePractice);

  const [questionCount, setQuestionCount] = useState(5);
  const [difficulty, setDifficulty] = useState("NORMAL");
  const [isWeaknessMode, setIsWeaknessMode] = useState(false);

  const [test, setTest] = useState(null);
  const [answers, setAnswers] = useState({});
  const [grading, setGrading] = useState(null);
  const [weakness, setWeakness] = useState(null);
  const [loading, setLoading] = useState(false);

  // 무한 모드 플래그 (타이머/세션 영향 방지)
  useEffect(() => {
    setInfinitePractice(true);
    return () => setInfinitePractice(false);
  }, [setInfinitePractice]);

  // planId 없으면 대시보드로
  useEffect(() => {
    if (!planId) {
      alert("학습을 선택해주세요.");
      navigate("/");
      return;
    }
    // 최초 진입 시 자동 생성
    handleGenerate(true);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [planId]);

  const questionList = test?.questions ?? [];

  const questionIndexById = useMemo(() => {
    const map = new Map();
    questionList.forEach((q, idx) => map.set(q.questionId, idx));
    return map;
  }, [questionList]);

  const handleGenerate = async (silent = false) => {
    if (!planId) return;
    setLoading(true);
    try {
      const data = await practiceApi.generateTest({
        planId,
        questionCount: Number(questionCount),
        difficulty,
        isWeaknessMode,
      });
      setTest(data);
      setAnswers({});
      setGrading(null);
      setWeakness(null);

      if (!silent) window.scrollTo({ top: 0, behavior: "smooth" });
    } catch (e) {
      console.error(e);
      alert("문제 생성에 실패했습니다. 잠시 후 다시 시도해주세요.");
    } finally {
      setLoading(false);
    }
  };

  const setTextAnswer = (questionId, value) => {
    setAnswers((prev) => ({
      ...prev,
      [questionId]: { ...prev[questionId], answerText: value },
    }));
  };

  const setChoiceAnswer = (questionId, selectedIndex, optionText) => {
    setAnswers((prev) => ({
      ...prev,
      [questionId]: {
        ...prev[questionId],
        selectedIndex,
        // 백엔드가 answerText를 참고할 수도 있어 같이 채움
        answerText: optionText ?? "",
      },
    }));
  };

  const handleSubmit = async () => {
    if (!planId) return;
    if (!questionList.length) {
      alert("먼저 문제를 생성해주세요.");
      return;
    }

    const payloadAnswers = questionList.map((q) => {
      const a = answers[q.questionId] || {};
      return {
        questionId: q.questionId,
        answerText: (a.answerText ?? "").toString(),
        selectedIndex: a.selectedIndex ?? null,
      };
    });

    const hasAny = payloadAnswers.some(
      (a) =>
        (a.answerText && a.answerText.trim().length > 0) ||
        a.selectedIndex != null
    );
    if (!hasAny) {
      alert("하나 이상 답안을 작성/선택해주세요.");
      return;
    }

    setLoading(true);
    try {
      const res = await practiceApi.submitTest({
        planId,
        answers: payloadAnswers,
      });
      setGrading(res);

      setTimeout(() => {
        const el = document.getElementById("practice-result");
        if (el) el.scrollIntoView({ behavior: "smooth", block: "start" });
      }, 0);
    } catch (e) {
      console.error(e);
      alert("제출/채점에 실패했습니다. 잠시 후 다시 시도해주세요.");
    } finally {
      setLoading(false);
    }
  };

  const handleLoadWeakness = async () => {
    if (!planId) return;
    setLoading(true);
    try {
      const res = await practiceApi.getWeaknessAnalysis(planId);
      setWeakness(res);

      setTimeout(() => {
        const el = document.getElementById("weakness-result");
        if (el) el.scrollIntoView({ behavior: "smooth", block: "start" });
      }, 0);
    } catch (e) {
      console.error(e);
      alert("약점 분석을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <Header />
      <div css={s.pageContainer}>
        <main css={s.chatArea}>
          <section css={s.headerPanel}>
            <div>
              <h2 css={s.pageTitle}>무한 반복 실습</h2>
              <p css={s.pageSubTitle}>
                {studyGoal
                  ? `선택된 학습: ${studyGoal}`
                  : "선택된 학습으로 문제를 생성해 연습하세요."}
              </p>
            </div>
            <div css={s.badgeRow}>
              <span css={s.badge}>planId: {planId ?? "-"}</span>
              {test?.testSessionId != null && (
                <span css={s.badge}>session: {test.testSessionId}</span>
              )}
            </div>
          </section>

          {loading && (
            <div css={s.placeholder}>
              <p>처리 중입니다...</p>
            </div>
          )}

          {!loading && questionList.length === 0 && (
            <div css={s.placeholder}>
              <p>아래에서 설정을 고르고 “문제 생성”을 눌러 시작하세요.</p>
            </div>
          )}

          {questionList.map((q, idx) => {
            const qid = q.questionId;
            const type = q.type;
            const typeLabel = QUESTION_TYPE_LABEL[type] || type;
            const mediaUrl = normalizeMediaUrl(q.referenceMediaUrl);
            const a = answers[qid] || {};

            return (
              <section key={qid ?? idx} css={s.questionCard}>
                <div css={s.questionHeader}>
                  <div css={s.questionTitleRow}>
                    <span css={s.qNo}>Q{idx + 1}</span>
                    <span css={s.typeTag}>{typeLabel}</span>
                    {q.topic && <span css={s.topicTag}>{q.topic}</span>}
                  </div>
                </div>

                <div css={s.questionText}>{q.questionText}</div>

                {mediaUrl && (
                  <img
                    css={s.referenceImage}
                    src={mediaUrl}
                    alt="reference"
                    onError={(e) => {
                      e.currentTarget.style.display = "none";
                    }}
                  />
                )}

                {type === "MULTIPLE_CHOICE" &&
                Array.isArray(q.options) &&
                q.options.length > 0 ? (
                  <div css={s.optionsWrapper}>
                    {q.options.map((opt, optIdx) => {
                      const checked = a.selectedIndex === optIdx;
                      return (
                        <label key={optIdx} css={s.optionItem(checked)}>
                          <input
                            type="radio"
                            name={`q-${qid}`}
                            checked={checked}
                            onChange={() => setChoiceAnswer(qid, optIdx, opt)}
                          />
                          <span>{opt}</span>
                        </label>
                      );
                    })}
                  </div>
                ) : (
                  <textarea
                    css={s.answerTextarea}
                    placeholder="답안을 입력하세요"
                    value={a.answerText ?? ""}
                    onChange={(e) => setTextAnswer(qid, e.target.value)}
                  />
                )}
              </section>
            );
          })}

          {grading && (
            <section id="practice-result" css={s.resultCard}>
              <div css={s.resultHeader}>
                <h3>채점 결과</h3>
                <span css={s.scorePill}>
                  총점 {grading.totalScore ?? 0}점
                </span>
              </div>

              {grading.summaryReview && (
                <p css={s.resultSummary}>{grading.summaryReview}</p>
              )}

              {Array.isArray(grading.results) && grading.results.length > 0 && (
                <div css={s.resultList}>
                  {grading.results.map((r, i) => {
                    const qIdx = questionIndexById.get(r.questionId);
                    const label =
                      qIdx != null ? `Q${qIdx + 1}` : `Q${i + 1}`;

                    return (
                      <div key={`${r.questionId}-${i}`} css={s.resultItem}>
                        <div css={s.resultItemHeader}>
                          <span css={s.resultQNo}>{label}</span>
                          <span
                            css={r.isCorrect ? s.correctPill : s.wrongPill}
                          >
                            {r.isCorrect ? "정답" : "오답"}
                          </span>
                          {r.weaknessTag && (
                            <span css={s.weakTag}>{r.weaknessTag}</span>
                          )}
                        </div>

                        {r.userAnswer && (
                          <div css={s.resultRow}>
                            <b>내 답:</b> <span>{r.userAnswer}</span>
                          </div>
                        )}

                        {r.explanation && (
                          <div css={s.resultRow}>
                            <b>해설:</b> <span>{r.explanation}</span>
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              )}
            </section>
          )}

          {weakness && (
            <section id="weakness-result" css={s.resultCard}>
              <div css={s.resultHeader}>
                <h3>오답 클리닉</h3>
              </div>

              {Array.isArray(weakness.weakPoints) &&
              weakness.weakPoints.length > 0 ? (
                <div css={s.weakList}>
                  {weakness.weakPoints.map((w, idx) => (
                    <div key={idx} css={s.weakItem}>
                      <div css={s.weakTopic}>{w.topic}</div>
                      <div css={s.weakMeta}>
                        오답 {w.wrongCount}회 · 오류율{" "}
                        {Math.round((w.errorRate ?? 0) * 100)}%
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <p css={s.resultSummary}>아직 분석할 데이터가 없어요.</p>
              )}

              {Array.isArray(weakness.recommendedQuestions) &&
                weakness.recommendedQuestions.length > 0 && (
                  <div css={s.recoBox}>
                    <h4 css={s.recoTitle}>추천 문제</h4>
                    {weakness.recommendedQuestions.map((q, idx) => (
                      <div key={q.questionId ?? idx} css={s.recoItem}>
                        <b>• {q.topic || "추천"}</b> {q.questionText}
                      </div>
                    ))}
                  </div>
                )}
            </section>
          )}
        </main>

        <footer css={s.bottomArea}>
          <div css={s.bottomInner}>
            <div css={s.controlGroup}>
              <label css={s.controlLabel}>
                문제 수
                <select
                  css={s.selectBox}
                  value={questionCount}
                  onChange={(e) => setQuestionCount(Number(e.target.value))}
                  disabled={loading}
                >
                  <option value={5}>5</option>
                  <option value={10}>10</option>
                </select>
              </label>

              <label css={s.controlLabel}>
                난이도
                <select
                  css={s.selectBox}
                  value={difficulty}
                  onChange={(e) => setDifficulty(e.target.value)}
                  disabled={loading}
                >
                  <option value="EASY">EASY</option>
                  <option value="NORMAL">NORMAL</option>
                  <option value="HARD">HARD</option>
                </select>
              </label>

              <label css={s.checkboxLabel}>
                <input
                  type="checkbox"
                  checked={isWeaknessMode}
                  onChange={(e) => setIsWeaknessMode(e.target.checked)}
                  disabled={loading}
                />
                약점 모드
              </label>
            </div>

            <div css={s.actionGroup}>
              <button
                css={s.actionBtnPrimary}
                onClick={() => handleGenerate(false)}
                disabled={loading}
              >
                문제 생성
              </button>
              <button
                css={s.actionBtn}
                onClick={handleSubmit}
                disabled={loading || questionList.length === 0}
              >
                제출/채점
              </button>
              <button
                css={s.actionBtn}
                onClick={handleLoadWeakness}
                disabled={loading}
              >
                오답 클리닉
              </button>
            </div>
          </div>
        </footer>
      </div>
    </>
  );
}

export default InfiniteStudyPage;