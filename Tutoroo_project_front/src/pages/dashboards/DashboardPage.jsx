/** @jsxImportSource @emotion/react */
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Header from "../../components/layouts/Header";
import ModalRoot from "../../components/modals/ModalRoot";

import useAuthStore from "../../stores/useAuthStore";
import useModalStore from "../../stores/modalStore";

import * as s from "./styles";

const DAY_NAMES = ["일", "월", "화", "수", "목", "금", "토"];

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

function DashboardPage() {
  const navigate = useNavigate(); 
  const user = useAuthStore((state) => state.user);
  const openLogin = useModalStore((state) => state.openLogin);

  const userName = user?.name || "OOO";

  const [weekOffset, setWeekOffset] = useState(0);
  const [selectedIndex, setSelectedIndex] = useState(0);
  const [showDetail, setShowDetail] = useState(false);

  const dates = getWeekDates(weekOffset);

  // ✅ 로그인 안 되어있으면 자동으로 로그인 모달
  useEffect(() => {
    if (!user) openLogin();
  }, [user, openLogin]);

  return (
    <>
      <Header />

      <div css={s.pageBg}>
        <main css={s.container}>
          <div css={s.newtutorbtn}>
            <div onClick={() => navigate("/tutor")} style={{ cursor: "pointer" }}>
              신규 선생님 등록 +
            </div>
          </div>

          <section css={s.greeting}>
            <div css={s.greetingText}>
              <h2>반가워요 {userName}님!</h2>
              <p>오늘의 목표를 달성하고 포인트를 획득해보세요</p>
            </div>

            <div css={s.actionWrap}>
              <button>학습선택 ▼</button>
              <button onClick={() => navigate("/study")}>학습하러 가기</button>
            </div>
          </section>

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

            <div css={s.card}>
              <span>누적 포인트 / 랭킹</span>
              <strong css={s.pointText}>0 P</strong>
              <p css={s.rankText}>현재 전체 -위</p>
            </div>
          </section>

          <section css={s.calendarArea}>
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

          <div css={s.more} onClick={() => setShowDetail((prev) => !prev)}>
            {showDetail ? "접기" : "더보기"}
          </div>

          {showDetail && (
            <section css={s.detailSection}>
              <div css={s.detailCard}>
                <h3 css={s.detailTitle}>수업 성취 진행률</h3>
                <div css={s.chartPlaceholder}>그래프 영역 (차트 예정)</div>
                <div css={s.progressFooter}>80%</div>
              </div>

              <div css={s.feedbackCard}>
                <h3 css={s.detailTitle}>AI 강의 피드백</h3>
                <p css={s.feedbackText}>
                  현재 개념은 잘 이해하고 있지만, 응용 문제에서 실수가 반복되고 있어요.
                  핵심 개념을 다시 정리한 뒤 난이도가 낮은 문제부터 차근차근 풀어보세요.
                </p>

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

      <ModalRoot />
    </>
  );
}

export default DashboardPage;