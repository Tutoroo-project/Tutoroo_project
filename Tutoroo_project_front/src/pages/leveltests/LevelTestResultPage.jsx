/** @jsxImportSource @emotion/react */
import Header from "../../components/layouts/Header";
import { useNavigate } from "react-router-dom";
import * as s from "./styles";
import useLevelTestStore from "../../stores/useLevelTestStore";

function LevelTestResultPage() {
  const navigate = useNavigate();
  const { subject, level, summary, roadmap } = useLevelTestStore();

  const chapters = Array.isArray(roadmap) ? roadmap : [];

  return (
    <>
      <Header />

      <main css={s.page}>
        <section css={s.resultCard}>
          <div css={s.tutorMessage}>
            <strong>AI Tutor가 분석한 내용입니다.</strong>
          </div>

          <h2 css={s.title}>레벨 테스트 결과</h2>

          <div css={s.summaryGrid}>
            <div css={s.summaryItem}>
              <span>과목</span>
              <strong>{subject ?? "-"}</strong>
            </div>

            <div css={s.summaryItem}>
              <span>추천 레벨</span>
              <strong>Lv.{level ?? "-"}</strong>
            </div>
          </div>

          <p css={s.description}>
            {summary || "현재 수준을 기준으로 맞춤 학습 로드맵을 추천드릴게요."}
          </p>
        </section>

        <section css={s.roadmapSection}>
          <h3 css={s.sectionTitle}>AI 추천 학습 로드맵</h3>

          {chapters.length === 0 ? (
            <p css={s.roadmapHint}>
              로드맵 데이터가 없습니다. 다시 레벨 테스트를 진행해주세요.
            </p>
          ) : (
            <ul css={s.roadmapList}>
              {chapters.map((c, idx) => (
                <li key={idx} css={s.roadmapItem}>
                  <div css={s.roadmapItemTitle}>
                    {c?.week ? `Week ${c.week}` : `Step ${idx + 1}`}
                    {c?.title ? ` · ${c.title}` : ""}
                  </div>

                  {Array.isArray(c?.topics) && c.topics.length > 0 && (
                    <ul css={s.topicList}>
                      {c.topics.map((t, tIdx) => (
                        <li key={tIdx} css={s.topicItem}>
                          {t}
                        </li>
                      ))}
                    </ul>
                  )}

                  {c?.description && (
                    <p css={s.roadmapItemDesc}>{c.description}</p>
                  )}
                </li>
              ))}
            </ul>
          )}

          <p css={s.roadmapHint}>
            AI가 분석한 결과를 기반으로 생성된 맞춤 학습 로드맵입니다.
          </p>
        </section>

        <div css={s.naviArea}>
          <button css={s.primaryBtn} onClick={() => navigate("/")}>
            이 로드맵으로 학습 시작하기
          </button>
        </div>
      </main>
    </>
  );
}

export default LevelTestResultPage;
