/** @jsxImportSource @emotion/react */
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { studyApi } from "../../apis/studys/studysApi";
import { userApi } from "../../apis/users/usersApi";
import Header from "../../components/layouts/Header";
import ModalRoot from "../../components/modals/ModalRoot";

import useAuthStore from "../../stores/useAuthStore";
import useModalStore from "../../stores/modalStore";
import useStudyStore from "../../stores/useStudyStore";

import * as s from "./styles";

// 요일 이름
const DAY_NAMES = ["일", "월", "화", "수", "목", "금", "토"];

// 기준 날짜를 기준으로 한 주 7일 날짜 정보 생성
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

/** * 로그인 후 사용자 메인 대시보드 페이지
 */
function DashboardPage() {
  const navigate = useNavigate();

  // 전역 상태 (Zustand)
  const user = useAuthStore((state) => state.user);
  const openLogin = useModalStore((state) => state.openLogin);
  const openStudyPlan = useModalStore((state) => state.openStudyPlan);
  
  // [New] 학습 정보 설정을 위한 액션 가져오기
  const setPlanInfo = useStudyStore((state) => state.setPlanInfo);

  // 대시보드 데이터 상태 관리
  const [dashboardData, setDashboardData] = useState(null);

  // 학습 목록 및 선택 상태
  const [studyList, setStudyList] = useState([]); 
  const [selectedStudyId, setSelectedStudyId] = useState(""); 

  const userName = dashboardData?.name || user?.name || "OOO";

  // 캘린더 관련 상태
  const [weekOffset, setWeekOffset] = useState(0); 
  const [selectedIndex, setSelectedIndex] = useState(0); 
  const [showDetail, setShowDetail] = useState(false);

  const dates = getWeekDates(weekOffset);

  // 로그인 체크
  useEffect(() => {
    const hasToken = !!localStorage.getItem("accessToken");
    if (!user && !hasToken) openLogin();
  }, [user, openLogin]);

  // 학습 목록이 로드되면 첫 번째 항목 자동 선택
  useEffect(() => {
    if (studyList.length > 0 && !selectedStudyId) {
      setSelectedStudyId(studyList[0].id);
    }
  }, [studyList, selectedStudyId]);

  // 데이터 조회 (대시보드 + 학습 목록)
  useEffect(() => {
    if (!user) return;

    const fetchData = async () => {
      try {
        // 1. 대시보드 정보
        const dashboard = await userApi.getDashboard();
        setDashboardData(dashboard);

        // 2. 학습 목록
        const list = await studyApi.getStudyList();
        setStudyList(Array.isArray(list) ? list : []);

      } catch (e) {
        console.error("데이터 조회 실패 :", e);
      }
    };
    fetchData();
  }, [user]);

  // 학습 시작 핸들러
  const handleStartStudy = () => {
    if (!selectedStudyId) {
      alert("학습을 선택해주세요");
      return;
    }

    const selectedStudy = studyList.find(s => String(s.id) === String(selectedStudyId));
    const studyName = selectedStudy ? selectedStudy.name : "학습";

    setPlanInfo(Number(selectedStudyId), studyName);

    navigate(`/tutor`);
  };

  return (
    <>
      <Header />
      <div css={s.pageBg}>
        <main css={s.container}>
          {/* 인삿말 영역 */}
          <section css={s.greeting}>
            <div css={s.greetingText}>
              <div css={s.titleRow}>
                <h2>반가워요 {userName}님!</h2>
                <button 
                  css={s.petBtn} 
                  onClick={() => navigate("/pet")} 
                >
                  🐶 마이 펫
                </button>
              </div>
              <p>오늘의 목표를 달성하고 포인트를 획득해보세요</p>
            </div>

            {/* 학습 액션 버튼 영역 */}
            <div css={s.actionWrap}>
              {/* 학습 선택 드롭다운 */}
              <select
                css={s.select}
                value={selectedStudyId}
                onChange={(e) => setSelectedStudyId(e.target.value)}
                disabled={studyList.length === 0}
              >
                <option value="">
                  {studyList.length === 0 ? "학습이 없습니다" : "학습 선택"}
                </option>
                {studyList.map((study) => (
                  <option key={study.id} value={study.id}>
                    {study.name}
                  </option>
                ))}
              </select>

              {/* 학습 추가 버튼 */}
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

              {/* 학습 시작 버튼 */}
              <button
                css={s.studyBtn}
                onClick={handleStartStudy}
              >
                학습하러 가기
              </button>
            </div>
          </section>

          {/* 상단 요약 카드 영역 */}
          <section css={s.cards}>
            <div css={s.card}>
              <span>현재학습 목표</span>
              <strong>{dashboardData?.currentGoal || "설정된 목표가 없습니다."}</strong>
            </div>

            <div css={s.card}>
              <span>학습 진도율</span>
              <div css={s.progressRow}>
                <div css={s.progressBar} />
                <span css={s.progressText}>0%</span>
              </div>
            </div>

            <div
              css={s.card}
              onClick={() => navigate("/ranking")}
              style={{ cursor: "pointer" }}
            >
              <span>누적 포인트 / 랭킹</span>
              <strong css={s.pointText}>{dashboardData?.totalPoints || 0} P</strong>
              <p css={s.rankText}>현재 전체 {dashboardData?.rank || "-"}위</p>
            </div>
          </section>

          {/* 캘린더 영역 */}
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

          {/* 상세 정보 토글 */}
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
                  현재 개념은 잘 이해하고 있지만, 응용 문제에서 실수가 반복되고
                  있어요.
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