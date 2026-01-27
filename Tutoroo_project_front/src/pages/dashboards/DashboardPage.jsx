/** @jsxImportSource @emotion/react */
import { useMutation } from "@tanstack/react-query";
import { useEffect, useState, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import { studyApi } from "../../apis/studys/studysApi";
import { userApi } from "../../apis/users/usersApi";
import { rankingApi } from "../../apis/ranking/rankingApi";
import Header from "../../components/layouts/Header";
import ModalRoot from "../../components/modals/ModalRoot";
import StudyChart from "../../components/charts/StudyChart";
import useAuthStore from "../../stores/useAuthStore";
import useModalStore from "../../stores/modalStore";
import useStudyStore from "../../stores/useStudyStore";

import * as s from "./styles";

// 요일 이름
const DAY_NAMES = ["일", "월", "화", "수", "목", "금", "토"];

function toYmd(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

// 기준 날짜를 기준으로 한 주 7일 날짜 정보 생성
function getWeekDates(offset = 0) {
  const today = new Date();
  const day = today.getDay();
  const start = new Date(today);
  start.setHours(0, 0, 0, 0);
  start.setDate(today.getDate() - day + offset * 7);

  return Array.from({ length: 7 }, (_, i) => {
    const date = new Date(start);
    date.setDate(start.getDate() + i);

    return {
      date,
      year: date.getFullYear(),
      month: date.getMonth() + 1,
      day: date.getDate(),
      label: `${date.getDate()}일 (${DAY_NAMES[date.getDay()]})`,
      iso: toYmd(date),
      dateObj: date,
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

  const dates = useMemo(() => getWeekDates(weekOffset), [weekOffset]);

  // 차트 관련 상태
  const [chartData, setChartData] = useState([]);
  const [weeklyRate, setWeeklyRate] = useState(0);

  const [myDash, setMyDash] = useState(null);

  const progressRate = Number.isFinite(weeklyRate)
    ? Math.min(100, Math.max(0, weeklyRate))
    : 0;

  const aiReport = dashboardData?.aiAnalysisReport;   // 백엔드 대시보드 DTO 필드
  const aiSuggestion = dashboardData?.aiSuggestion;
  const currentPlanId = dashboardData?.studyList?.[0]?.id;

  const { mutate: generateFeedback, isPending } = useMutation({
    mutationFn: () => studyApi.generateAiFeedback(currentPlanId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    },
  });

  useEffect(() => {
    const todayIso = toYmd(new Date());
    const idx = dates.findIndex((d) => d.iso === todayIso);
    setSelectedIndex(idx >= 0 ? idx : 0);
  }, [weekOffset]);

  const [planDetail, setPlanDetail] = useState(null);
  const [curriculumByDate, setCurriculumByDate] = useState({});
  const [doneByIso, setDoneByIso] = useState({});

  function parseYmdToDate(ymd) {
    if (!ymd) return null;
    const [y, m, d] = ymd.split("-").map(Number);
    return new Date(y, m - 1, d);
  }

  function getDayNo(dayStr) {
    const m = String(dayStr ?? "").match(/(\d+)/);
    return m ? Number(m[1]) : null;
  }

  function flattenCurriculum(detailedCurriculum) {
    const list = [];
    if (!detailedCurriculum) return list;
    Object.entries(detailedCurriculum).forEach(([week, days]) => {
      (days ?? []).forEach((d) => {
        const dayNo = getDayNo(d.day);
        if (!dayNo) return;
        list.push({ ...d, dayNo, week });
      });
    });

    return list.sort((a, b) => a.dayNo - b.dayNo);
  }

  const MS_WEEK = 7 * 24 * 60 * 60 * 1000;

  function getWeekStartSunday(d) {
    const x = new Date(d);
    x.setHours(0, 0, 0, 0);
    x.setDate(x.getDate() - x.getDay());
    return x;
  }

  function calcWeekNo(startYmd, weekStartDate) {
    if (!startYmd || !weekStartDate) return null;

    const start = parseYmdToDate(startYmd);
    if (!start) return null;

    const baseSunday = getWeekStartSunday(start);
    const currentSunday = getWeekStartSunday(weekStartDate);

    const diffWeeks = Math.floor((currentSunday - baseSunday) / MS_WEEK);
    return Math.max(1, diffWeeks + 1);
  }

  const weekNo = useMemo(() => {
    const startYmd = planDetail?.startDate;
    const weekStart = dates?.[0]?.dateObj;
    return calcWeekNo(startYmd, weekStart);
  }, [planDetail?.startDate, dates]);

  //  선택된 학습(planId) 바뀔 때마다 로드맵 불러오기
  useEffect(() => {
    if (!user || !selectedStudyId) return;

    const fetchPlan = async () => {
      try {
        const detail = await studyApi.getPlanDetail(selectedStudyId);
        setPlanDetail(detail);
      } catch (e) {
        console.error("플랜 상세 조회 실패:", e);
        setPlanDetail(null);
      }
    };

    fetchPlan();
  }, [user, selectedStudyId]);

  useEffect(() => {
    const detailed = planDetail?.roadmap?.detailedCurriculum;
    const startYmd = planDetail?.startDate;

    if (!detailed || !startYmd) {
      setCurriculumByDate({});
      return;
    }

    const start = parseYmdToDate(startYmd);
    const flat = flattenCurriculum(detailed);

    const map = {};
    flat.forEach((item) => {
      const d = new Date(start);
      d.setDate(start.getDate() + (item.dayNo - 1));
      map[toYmd(d)] = item;
    });

    setCurriculumByDate(map);
  }, [planDetail]);

  // 로그인 체크
  useEffect(() => {
    const hasToken = !!localStorage.getItem("accessToken");
    if (!user && !hasToken) openLogin();
  }, [user, openLogin]);

  // 학습 목록이 로드되면 첫 번째 항목 자동 선택
  useEffect(() => {
    if (studyList.length > 0 && !selectedStudyId) {
      setSelectedStudyId(String(studyList[0].id));
    }
  }, [studyList, selectedStudyId]);

  // 데이터 조회 (대시보드 + 학습 목록)
  useEffect(() => {
    if (!user) return;
    if (!selectedStudyId) return;

    let alive = true;

    const fetchChart = async () => {
      try {
        const ymSet = new Set(dates.map((d) => `${d.year}-${d.month}`));
        const ymList = Array.from(ymSet).map((k) => {
          const [year, month] = k.split("-").map(Number);
          return { year, month };
        });

        const results = await Promise.all(
          ymList.map(({ year, month }) =>
            studyApi.getMonthlyCalendar({
              year,
              month,
              planId: Number(selectedStudyId),
            }),
          ),
        );

        const mapByIso = {};
        results.forEach((response) => {
          const year = response.year;
          const month = String(response.month).padStart(2, "0");
          (response.logs || []).forEach((log) => {
            const day = String(log.day).padStart(2, "0");
            const iso = `${year}-${month}-${day}`;
            mapByIso[iso] = {
              isDone: !!log.isDone,
              score: log.score ?? log.maxScore ?? 0,
              topic: log.topic ?? "",
            };
          });
        });

        const nextChart = dates.map((d) => {
          const log = mapByIso[d.iso];
          return {
            label: `${d.month}/${d.day}`,
            score: log?.score ?? 0,
            completed: !!log?.isDone,
          };
        });

        const doneCount = nextChart.filter((x) => x.completed).length;
        const rate = nextChart.length
          ? Math.round((doneCount / nextChart.length) * 100)
          : 0;
        if (!alive) return;
        setDoneByIso(mapByIso);
        setChartData(nextChart);
        setWeeklyRate(rate);
      } catch (e) {
        console.error("차트/캘린더 데이터 조회 실패:", e);
        if (!alive) return;
        setChartData([]);
        setWeeklyRate(0);
      }
    };
    fetchChart();
    return () => {
      alive = false;
    };
  }, [user, weekOffset, selectedStudyId]);

  useEffect(() => {
    if (!user) return;

    const fetchData = async () => {
      const [dashRes, listRes, myRes] = await Promise.allSettled([
        userApi.getDashboard(),
        studyApi.getStudyList(),
        rankingApi.getRankings(),
      ]);

      if (dashRes.status === "fulfilled") setDashboardData(dashRes.value);
      if (listRes.status === "fulfilled") {
        const list = listRes.value;
        setStudyList(Array.isArray(list) ? list : []);
      }
      if (myRes.status === "fulfilled") setMyDash(myRes.value?.myRank ?? null);
    };
    fetchData();
  }, [user]);

  // 학습 시작 핸들러
  const handleStartStudy = () => {
    if (!selectedStudyId) {
      alert("학습을 선택해주세요");
      return;
    }

    const selectedStudy = studyList.find(
      (s) => String(s.id) === String(selectedStudyId),
    );
    const studyName = selectedStudy ? selectedStudy.name : "학습";

    setPlanInfo(Number(selectedStudyId), studyName);

    navigate(`/tutor`);
  };

  const point = myDash?.totalPoint ?? dashboardData?.currentPoint ?? 0;
  const rankNo = myDash?.rank ?? dashboardData?.rank ?? "-";

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
                <button css={s.petBtn} onClick={() => navigate("/pet")}>
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
                  <option key={study.id} value={String(study.id)}>
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
              <button css={s.studyBtn} onClick={handleStartStudy}>
                학습하러 가기
              </button>
            </div>
          </section>

          {/* 상단 요약 카드 영역 */}
          <section css={s.cards}>
            <div css={s.card}>
              <span>현재학습 목표</span>
              <strong>
                {dashboardData?.currentGoal || "설정된 목표가 없습니다."}
              </strong>
            </div>

            <div css={s.card}>
              <span>
                {weekNo ? `${weekNo}주차 학습 진도율` : "학습 진도율"}
              </span>
              <div css={s.progressRow}>
                <div css={s.progressBar}>
                  <div css={s.progressFill(progressRate)} />
                </div>
                <span css={s.progressText}>{progressRate}%</span>
              </div>
            </div>

            <div
              css={s.card}
              onClick={() => navigate("/ranking")}
              style={{ cursor: "pointer" }}
            >
              <span>누적 포인트 / 랭킹</span>
              <strong css={s.pointText}>{point} P</strong>
              <p css={s.rankText}>현재 전체 {rankNo}위</p>
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
              {dates.map((date, i) => {
                const isToday = date.iso === toYmd(new Date());
                const done = doneByIso[date.iso]; // { isDone, maxScore, topic }

                return (
                  <div
                    key={date.iso ?? i}
                    css={s.calendarCard(i === selectedIndex, isToday)}
                    onClick={() => setSelectedIndex(i)}
                  >
                    <div css={s.calendarHeader}>
                      <span css={s.headerLabel}>{date.label}</span>

                      <div css={s.headerBadges}>
                        {isToday && <span css={s.todayBadge}>오늘</span>}
                        {done?.isDone && <span css={s.doneBadge}>✔</span>}
                      </div>
                    </div>

                    <div css={s.calendarBody}>
                      {!curriculumByDate?.[date.iso] ? (
                        <div css={s.emptyCurriculum}>일정 없음</div>
                      ) : (
                        <>
                          <div css={s.dayBadge}>
                            Day {curriculumByDate[date.iso].dayNo}
                          </div>
                          <div css={s.topicText}>
                            {curriculumByDate[date.iso].topic}
                          </div>
                          <div css={s.metaText}>
                            {curriculumByDate[date.iso].method}
                          </div>
                          <div css={s.metaText}>
                            {curriculumByDate[date.iso].material}
                          </div>
                        </>
                      )}
                    </div>
                  </div>
                );
              })}
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
                <div css={s.chartPlaceholder}>
                  <StudyChart data={chartData} />
                </div>
                <div css={s.progressFooter}>{weeklyRate}%</div>
              </div>

              <div css={s.feedbackCard}>
                <h3 css={s.detailTitle}>AI 강의 피드백</h3>

                <div css={s.feedbackText}>
                  {(aiReport || "").split("\n").filter(Boolean).map((line, idx) => (
                    <p key={idx} css={s.feedbackLine}>
                      {line}
                    </p>
                  ))}
                </div>

                <button
                  css={s.feedbackBtn}
                  disabled={!currentPlanId || isPending}
                  onClick={() => generateFeedback()}
                >
                  {isPending ? "생성 중..." : aiReport ? "피드백 업데이트" : "AI 피드백 생성"}
                </button>
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
