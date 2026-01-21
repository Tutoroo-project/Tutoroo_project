/** @jsxImportSource @emotion/react */
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Header from "../../components/layouts/Header";
import ModalRoot from "../../components/modals/ModalRoot";

import useAuthStore from "../../stores/useAuthStore";
import useModalStore from "../../stores/modalStore";

import * as s from "./styles";

// 요일 이름
const DAY_NAMES = ["일", "월", "화", "수", "목", "금", "토"];

// 기준 날짜를 기준으로 한 주 7일 날짜 정보 생성
// -offset : 주단위 이동 (0 = 이번 주, -1 = 이전 주, +1 = 다음 주)

function getWeekDates(offset = 0) {
  const today = new Date();
  const day = today.getDay();
  const start = new Date(today);
  start.setDate(today.getDate() - day + offset * 7);

  return Array.from({ length: 7 }, (_, i) => {
    const date = new Date(start);
    date.setDate(start.getDate() + i);

    return {
      label: `${date.getDate()}일 (${DAY_NAMES[date.getDay()]})`,
    };
  });
}

/** 로그인 후 사용자 메인 대시보드 페이지 ("/") 홈 화면임 그냥
 *
 * - 사용자 인사 / 학습 선택 / 학습 진입
 * - 요약 카드 (목표, 진도, 포인트/랭킹)
 * - 주간 캘린더
 * - 상세 정보 (AI 피드백, 진행률)
 */

function DashboardPage() {
  const navigate = useNavigate();

  // 전역 상태 (Zustand)
  const user = useAuthStore((state) => state.user);
  const openLogin = useModalStore((state) => state.openLogin);
  const openStudyPlan = useModalStore((state) => state.openStudyPlan);

  // 상태 저장
  const [studyList, setStudyList] = useState([]); // 학습 목록 (임시임)
  const [selectedStudyId, setSelectedStudyId] = useState(""); // 선택된 학습 ID

  const userName = user?.name || "OOO";
  // 캘린더 관련 상태
  const [weekOffset, setWeekOffset] = useState(0); // 주 이동 offset
  const [selectedIndex, setSelectedIndex] = useState(0); // 선택된 날짜 인덱스

  // 상세 영역 토글
  const [showDetail, setShowDetail] = useState(false);

  // 현재 주 날짜 목록
  const dates = getWeekDates(weekOffset);

  // 로그인 안 되어있으면 자동으로 로그인 모달 열어줌
  useEffect(() => {
    if (!user) openLogin();
  }, [user, openLogin]);

  // 학습목록이 하나라도 있으면 학습선택이 아니라 java가 뜨게됨
  useEffect(() => {
    // 학습이 하나라도 있고, 아직 선택된 값이 없을 때
    if (studyList.length > 0 && !selectedStudyId) {
      setSelectedStudyId(studyList[0].id);
      // 또는 studyList[0].id
    }
  }, [studyList, selectedStudyId]);

  // 학습 목록 불러오기 (API 연동 해주면 주석 해제해서 쓸거임)
  /*
  useEffect(() => {
    if (!user) return;
    fetchStudyList();
  }, [user]);
  */

  return (
    <>
      {/* 공통으로 사용하는 헤더 컴포넌트 */}
      <Header />
      <div css={s.pageBg}>
        <main css={s.container}>
          {/* 인삿말 영역 */}
          <section css={s.greeting}>
            <div css={s.greetingText}>
              <h2>반가워요 {userName}님!</h2>
              <p>오늘의 목표를 달성하고 포인트를 획득해보세요</p>
            </div>

            {/* 학습 액션 버튼 영역 */}
            <div css={s.actionWrap}>
              {/* 학습 선택 */}
              <select
                css={s.select}
                value={selectedStudyId}
                onChange={(e) => setSelectedStudyId(e.target.value)}
                disabled={studyList.length === 0}
              >
                <option value="">
                  {studyList.length === 0 ? "학습이 없습니다" : "학습 선택"}
                </option>
                {/* 서버에서 받아온 학습 목록 */}
                {studyList.map((study) => (
                  <option key={study.id} value={study.id}>
                    {study.name}
                  </option>
                ))}
              </select>
              {/* 학습 추가 버튼 (클릭하면 학습 계획 수립 모달 open, 비로그인시 로그인 modal open) */}
              <button
                css={s.studyBtn}
                onClick={() => {
                  if (!user) {
                    openLogin();
                    return;
                  }
                  openStudyPlan();
                }}
              >
                학습 추가 +
              </button>

              {/* 학습 시작 */}
              <button
                css={s.studyBtn}
                onClick={() => {
                  if (!selectedStudyId) {
                    alert("학습을 선택해주세요");
                    return;
                  }
                  navigate(`/tutor`);
                }}
              >
                학습하러 가기
              </button>
            </div>
          </section>
          {/* 상단 요약 카드 영역 */}
          <section css={s.cards}>
            <div css={s.card}>
              <span>현재학습 목표</span>
              <strong>설정된 목표가 없습니다.</strong>
            </div>

            <div css={s.card}>
              <span>학습 진도율</span>
              <div css={s.progressRow}>
                <div css={s.progressBar} />
                <span css={s.progressText}>0%</span>
              </div>
            </div>

            {/* 포인트 / 랭킹 카드 (클릭하면 "/ranking" 페이지로 이동) */}
            <div
              css={s.card}
              onClick={() => navigate("/ranking")}
              style={{ cursor: "pointer" }}
            >
              <span>누적 포인트 / 랭킹</span>
              <strong css={s.pointText}>0 P</strong>
              <p css={s.rankText}>현재 전체 -위</p>
            </div>
          </section>

          {/* 캘린더 영역 */}
          <section css={s.calendarArea}>
            {/* 이전 주 */}
            <button
              css={s.arrowBtn}
              onClick={() => {
                setWeekOffset((prev) => prev - 1);
                setSelectedIndex(0);
              }}
            >
              ‹
            </button>

            <div css={s.calendarRow}>
              {dates.map((date, i) => (
                <div
                  key={i}
                  css={s.calendarCard(i === selectedIndex)}
                  onClick={() => setSelectedIndex(i)}
                >
                  <div css={s.calendarHeader}>{date.label}</div>
                  <div css={s.calendarBody} />
                </div>
              ))}
            </div>

            {/* 다음 주 */}
            <button
              css={s.arrowBtn}
              onClick={() => {
                setWeekOffset((prev) => prev + 1);
                setSelectedIndex(0);
              }}
            >
              ›
            </button>
          </section>

          {/* 상세 정보 토글 */}
          <div css={s.more} onClick={() => setShowDetail((prev) => !prev)}>
            {showDetail ? "접기" : "더보기"}
          </div>

          {showDetail && (
            <section css={s.detailSection}>
              {/* 수업 성취 진행률 (그래프) */}
              <div css={s.detailCard}>
                <h3 css={s.detailTitle}>수업 성취 진행률</h3>
                <div css={s.chartPlaceholder}>그래프 영역 (차트 예정)</div>
                <div css={s.progressFooter}>80%</div>
              </div>

              <div css={s.feedbackCard}>
                <h3 css={s.detailTitle}>AI 강의 피드백</h3>
                <p css={s.feedbackText}>
                  현재 개념은 잘 이해하고 있지만, 응용 문제에서 실수가 반복되고
                  있어요. 핵심 개념을 다시 정리한 뒤 난이도가 낮은 문제부터
                  차근차근 풀어보세요.
                </p>

                {/* AI 튜터 피드백 */}
                <ul css={s.feedbackList}>
                  <li>✔️ 개념 이해도 우수</li>
                  <li>✔️ 학습 지속성 좋음</li>
                  <li>⚠️ 실수 패턴 반복</li>
                </ul>
              </div>
            </section>
          )}
        </main>
      </div>

      {/* 전역 모달 랜더링 */}
      <ModalRoot />
    </>
  );
}

export default DashboardPage;
