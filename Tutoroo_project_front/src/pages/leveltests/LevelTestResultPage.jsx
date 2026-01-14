/** @jsxImportSource @emotion/react */
import Header from "../../components/layouts/Header";
import { useNavigate } from "react-router-dom";
import * as s from "./styles";
import useLevelTestStore from "../../stores/useLevelTestStore";

//임시이미지
const FALLBACK_ROADMAP_IMAGE =
  "https://via.placeholder.com/800x1200?text=AI+Roadmap+Image";

function LevelTestResultPage() {
  const navigate = useNavigate();

  // 레벨 테스트 결과 (Zustand)
  const {
    subject,
    level,
    summary,
    roadmap,
    roadmapImageUrl, 
  } = useLevelTestStore();

  // 일단은 주석으로 쓸게요 나중에 api 붙으면 해제해서 레벨테스트 결과 안받고 오면 navigate로 /level-test로 강제이동
  // useEffect(() => {
  //   if (!subject || !level) {
  //     navigate("/level-test");
  //   }
  // }, [subject, level, navigate]);

  // if (!subject || !level) {
  //   return null;
  // }

  //  실제로 사용할 이미지 URL 결정
  const imageUrl = roadmapImageUrl || FALLBACK_ROADMAP_IMAGE;

  return (
    <>
      <Header />

      <main css={s.page}>
        {/* ===== 결과 요약 ===== */}
        <section css={s.resultCard}>
          <div css={s.tutorMessage}>
            <strong>AI Tutor가 분석한 내용입니다.</strong>
          </div>

          <h2 css={s.title}>레벨 테스트 결과</h2>

          <div css={s.summaryGrid}>
            <div css={s.summaryItem}>
              <span>과목</span>
              <strong>{subject ?? "Java"}</strong>
            </div>

            <div css={s.summaryItem}>
              <span>추천 레벨</span>
              <strong>Lv.{level ?? 3}</strong>
            </div>
          </div>

          <p css={s.description}>
            {summary ||
              "현재 수준을 기준으로 맞춤 학습 로드맵을 추천드릴게요."}
          </p>
        </section>

        {/* ===== 로드맵 이미지 ===== */}
        <section css={s.roadmapSection}>
          <h3 css={s.sectionTitle}>AI 추천 학습 로드맵</h3>

          <div css={s.roadmapImageWrapper}>
            <img
              src={imageUrl}
              alt="AI 학습 로드맵"
              css={s.roadmapImage}
            />
          </div>

          <p css={s.roadmapHint}>
            AI가 분석한 결과를 기반으로 생성된 맞춤 학습 로드맵입니다.
          </p>
        </section>

        {/* ===== CTA ===== */}
        <div css={s.naviArea}>
          <button
            css={s.primaryBtn}
            onClick={() => navigate("/dashboard")}
          >
            이 로드맵으로 학습 시작하기
          </button>
        </div>
      </main>
    </>
  );
}

export default LevelTestResultPage;
