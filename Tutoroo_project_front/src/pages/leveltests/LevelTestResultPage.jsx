/** @jsxImportSource @emotion/react */
import Header from "../../components/layouts/Header";
import { useNavigate } from "react-router-dom";
import * as s from "./styles";
import useLevelTestStore from "../../stores/useLevelTestStore";

// 로드맵 이미지가 없어서 임시로 넣은 이미지
const FALLBACK_ROADMAP_IMAGE =
  "https://via.placeholder.com/800x1200?text=AI+Roadmap+Image";

// 레벨 테스트 결과 (Zustand) levelTestStore에 저장된 TEST 결과 사용
// AI가 분석한 요약, 추천레벨, 로드맵 이미지 표시
function LevelTestResultPage() {
  const navigate = useNavigate();

  // 레벨 테스트 완료 후 저장된 결과 데이터
  const { subject, level, summary, roadmap, roadmapImageUrl } =
    useLevelTestStore();

  // 일단은 주석으로 쓸게요 나중에 API 붙으면 해제해서
  // 레벨테스트 결과 안받고 url 주소창에 적고 오면 Navigate가
  // /level-test로 강제이동 시킬거임

  // useEffect(() => {
  //   if (!subject || !level) {
  //     navigate("/level-test");
  //   }
  // }, [subject, level, navigate]);

  // if (!subject || !level) {
  //   return null;
  // }

  //  실제로 사용할 로드맵 이미지 사용하고 없으면 위에 임시로 이미지 URL 넣어둔거 사용
  const imageUrl = roadmapImageUrl || FALLBACK_ROADMAP_IMAGE;

  return (
    <>
      {/* 공통으로 사용하는 헤더 컴포넌트 */}
      <Header />

      <main css={s.page}>
        {/* 결과 요약 카드 */}
        <section css={s.resultCard}>
          {/* AI 튜터 안내 메시지 */}
          <div css={s.tutorMessage}>
            <strong>AI Tutor가 분석한 내용입니다.</strong>
          </div>

          <h2 css={s.title}>레벨 테스트 결과</h2>
          {/* 과목 / 추천 레벨 요약 랜더링 */}
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

          {/* AI 요약 설명 */}
          <p css={s.description}>
            {summary || "현재 수준을 기준으로 맞춤 학습 로드맵을 추천드릴게요."}
          </p>
        </section>

        {/* 로드맵 이미지 */}
        <section css={s.roadmapSection}>
          <h3 css={s.sectionTitle}>AI 추천 학습 로드맵</h3>

          <div css={s.roadmapImageWrapper}>
            <img src={imageUrl} alt="AI 학습 로드맵" css={s.roadmapImage} />
          </div>

          <p css={s.roadmapHint}>
            AI가 분석한 결과를 기반으로 생성된 맞춤 학습 로드맵입니다.
          </p>
        </section>

        {/* 대시보드로 돌아가기 버튼 */}
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
