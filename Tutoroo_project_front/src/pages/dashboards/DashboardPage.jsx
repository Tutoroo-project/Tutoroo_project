/** @jsxImportSource @emotion/react */
import { useMutation } from "@tanstack/react-query";
import { useEffect, useState, useMemo, useRef } from "react";
import { useNavigate } from "react-router-dom";
import Swal from "sweetalert2";
import { studyApi } from "../../apis/studys/studysApi";
import { userApi } from "../../apis/users/usersApi";
import { rankingApi } from "../../apis/ranking/rankingApi";
import Header from "../../components/layouts/Header";
import ModalRoot from "../../components/modals/ModalRoot";
import StudyChart from "../../components/charts/StudyChart";
import useAuthStore from "../../stores/useAuthStore";
import useModalStore from "../../stores/modalStore";
import useStudyStore from "../../stores/useStudyStore";
import { FaTrash } from "react-icons/fa";

import * as s from "./styles";


// 요일 이름
const DAY_NAMES = ["일", "월", "화", "수", "목", "금", "토"];

function toYmd(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function getWeekDates(startDateStr, offset = 0) {
  if (!startDateStr) {
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

  // 학습 시작일 기준으로 주 계산
  const startDate = new Date(startDateStr);
  startDate.setHours(0, 0, 0, 0);

  // offset주 만큼 이동
  const weekStart = new Date(startDate);
  weekStart.setDate(startDate.getDate() + offset * 7);

  return Array.from({ length: 7 }, (_, i) => {
    const date = new Date(weekStart);
    date.setDate(weekStart.getDate() + i);

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

function DashboardPage() {
  const navigate = useNavigate();

  // 전역 상태 (Zustand)
  const user = useAuthStore((state) => state.user);
  const openLogin = useModalStore((state) => state.openLogin);
  const openStudyPlan = useModalStore((state) => state.openStudyPlan);

  // 학습 정보 설정 액션 및 상태
  const setPlanInfo = useStudyStore((state) => state.setPlanInfo);
  const currentPlanId = useStudyStore((state) => state.planId);
  const currentMessages = useStudyStore((state) => state.messages);

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

  const [isStudyMenuOpen, setIsStudyMenuOpen] = useState(false);
  const studyMenuRef = useRef(null);

  useEffect(() => {
    const handleOutside = (e) => {
      if (!studyMenuRef.current) return;
      if (!studyMenuRef.current.contains(e.target)) setIsStudyMenuOpen(false);
    };
    const handleKey = (e) => {
      if (e.key === "Escape") setIsStudyMenuOpen(false);
    };
    document.addEventListener("mousedown", handleOutside);
    document.addEventListener("keydown", handleKey);
    return () => {
      document.removeEventListener("mousedown", handleOutside);
      document.removeEventListener("keydown", handleKey);
    };
  }, []);

  const [planDetail, setPlanDetail] = useState(null);

  const dates = useMemo(() => {
    const startDate = planDetail?.startDate;
    return getWeekDates(startDate, weekOffset);
  }, [planDetail?.startDate, weekOffset]);

  // 차트 관련 상태
  const [chartData, setChartData] = useState([]);
  const [weeklyRate, setWeeklyRate] = useState(0);

  const [myDash, setMyDash] = useState(null);

  const progressRate = Number.isFinite(weeklyRate)
    ? Math.min(100, Math.max(0, weeklyRate))
    : 0;

  const aiReport = dashboardData?.aiAnalysisReport;

  const planIdForFeedback = selectedStudyId ? Number(selectedStudyId) : null;

  const { mutate: generateFeedback, isPending } = useMutation({
    mutationFn: () => studyApi.generateAiFeedback(planIdForFeedback),
    onSuccess: (feedbackText) => {
      setDashboardData((prev) =>
        prev ? { ...prev, aiAnalysisReport: feedbackText } : prev,
      );
    },
  });

  const handleDeleteStudy = async () => {
    if (!selectedStudyId) {
      Swal.fire("알림", "삭제할 학습을 선택해주세요.", "warning");
      return;
    }

    // 1. 비밀번호 입력 받기
    const { value: password } = await Swal.fire({
      title: '학습 삭제',
      html: '정말 삭제하시겠습니까?<br/>본인 확인을 위해 <b>비밀번호</b>를 입력해주세요.',
      input: 'password',
      inputPlaceholder: '비밀번호 입력',
      showCancelButton: true,
      confirmButtonText: '삭제',
      cancelButtonText: '취소',
      confirmButtonColor: '#ff4d4f',
      preConfirm: async (password) => {
        if (!password) {
          Swal.showValidationMessage('비밀번호를 입력해주세요.');
        }
        return password;
      }
    });

    if (password) {
      try {
        // 2. 비밀번호 검증 API 호출
        await userApi.verifyPassword(password);

        // 3. 검증 성공 시 삭제 API 호출
        await studyApi.deleteStudyPlan(selectedStudyId);

        await Swal.fire("삭제 완료", "학습 플랜이 정상적으로 삭제되었습니다.", "success");

        // 4. 리스트 갱신 및 선택값 초기화
        const newList = await studyApi.getStudyList();
        setStudyList(newList);

        if (newList.length > 0) {
          // 남은 학습 중 첫 번째 선택
          setSelectedStudyId(String(newList[0].id));
        } else {
          // 남은 학습이 없으면 초기화
          setSelectedStudyId("");
          setDashboardData(null);
          setChartData([]);
          setPlanDetail(null);
        }

      } catch (error) {
        console.error(error);
        const msg = error.response?.data?.message || "비밀번호가 일치하지 않거나 삭제 중 오류가 발생했습니다.";
        Swal.fire("삭제 실패", msg, "error");
      }
    }
  };

  useEffect(() => {
    const todayIso = toYmd(new Date());
    const idx = dates.findIndex((d) => d.iso === todayIso);
    setSelectedIndex(idx >= 0 ? idx : 0);
  }, [dates]);

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

    // 주차를 정렬하여 순서대로 처리
    const sortedWeeks = Object.keys(detailedCurriculum).sort((a, b) => {
      const weekNoA = parseInt(a.match(/\d+/)?.[0] || "0");
      const weekNoB = parseInt(b.match(/\d+/)?.[0] || "0");
      return weekNoA - weekNoB;
    });

    let cumulativeDayNo = 0;

    sortedWeeks.forEach((week) => {
      const days = detailedCurriculum[week];
      if (!Array.isArray(days)) return;

      days.forEach((d) => {
        const dayNo = getDayNo(d.day);
        if (!dayNo) return;

        cumulativeDayNo++;
        list.push({ ...d, dayNo: cumulativeDayNo, week });
      });
    });

    return list;
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

  useEffect(() => {
    if (!selectedStudyId) return;
    setDashboardData((prev) =>
      prev ? { ...prev, aiAnalysisReport: "" } : prev,
    );
    setPlanDetail(null);
    setCurriculumByDate({});
    setDoneByIso({});
    setChartData([]);
    setWeeklyRate(0);
    setWeekOffset(0);
  }, [selectedStudyId]);

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
  }, [user, weekOffset, selectedStudyId, dates]);

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

  // 오늘 날짜 확인 및 완료 여부 계산
  const todayIso = toYmd(new Date());
  const isTodayDone = !!doneByIso[todayIso]?.isDone;

  // 학습 시작 핸들러
  const startRegularClass = () => {
    if (isTodayDone) {
      alert("오늘의 학습을 이미 완료했습니다! 내일 또 만나요.");
      return;
    }

    if (!selectedStudyId) {
      alert("학습을 선택해주세요");
      return;
    }

    const targetId = Number(selectedStudyId);

    // 이어하기 체크: 현재 플랜과 같고 메시지가 있으면 바로 이동
    if (targetId === currentPlanId && currentMessages.length > 0) {
      navigate("/study");
      return;
    }

    const selectedStudy = studyList.find(
      (s) => String(s.id) === String(selectedStudyId),
    );
    const studyName = selectedStudy ? selectedStudy.name : "학습";

    setPlanInfo(targetId, studyName);
    navigate(`/tutor`);
  };

  const goInfinitePractice = () => {
    if (!selectedStudyId) {
      alert("학습을 선택해주세요");
      return;
    }
    const targetId = Number(selectedStudyId);
    const selectedStudy = studyList.find(
      (s) => String(s.id) === String(selectedStudyId),
    );
    const studyName = selectedStudy ? selectedStudy.name : "학습";

    setPlanInfo(targetId, studyName);
    navigate("/practice/infinite");
  }

  const toggleStudyMenu = () => {
    if (!user) {
      openLogin();
      return;
    }
    if (!selectedStudyId) {
      alert("학습을 선택해주세요");
      return;
    }
    setIsStudyMenuOpen((prev) => !prev);
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

              <button
                css={s.deleteBtn}
                onClick={handleDeleteStudy}
                disabled={!selectedStudyId}
                title="현재 선택된 학습 삭제"
              >
                <FaTrash />
              </button>

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
              <div css={s.studyMenuWrap} ref={studyMenuRef}>
                <button
                  css={[s.studyBtn, isTodayDone && s.completedBtn]}
                  onClick={toggleStudyMenu}
                  aria-haspopup="menu"
                  aria-expanded={isStudyMenuOpen}
                  type="button"
                >
                  {isTodayDone ? "오늘 학습 완료" : "학습하러 가기"}
                  <span css={[s.caret, isStudyMenuOpen && s.caretOpen]}>▼</span>
                </button>

                {isStudyMenuOpen && (
                  <div css={s.studyMenu} role="menu">
                    <button
                      type="button"
                      css={s.studyMenuItem}
                      onClick={() => {
                        setIsStudyMenuOpen(false);
                        startRegularClass();
                      }}
                      disabled={isTodayDone}   //  오늘 완료면 정규수업만 막고
                    >
                      정규 수업
                    </button>

                    <button
                      type="button"
                      css={s.studyMenuItem}
                      onClick={() => {
                        setIsStudyMenuOpen(false);
                        goInfinitePractice();   // 실습은 계속 가능
                      }}
                    >
                      무한 반복 실습
                    </button>
                  </div>
                )}
              </div>
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
              disabled={weekOffset <= 0}
            >
              ‹
            </button>

            <div css={s.calendarRow}>
              {dates.map((date, i) => {
                const isToday = date.iso === toYmd(new Date());
                const done = doneByIso[date.iso];

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
                  {(aiReport || "")
                    .split("\n")
                    .filter(Boolean)
                    .map((line, idx) => (
                      <p key={idx} css={s.feedbackLine}>
                        {line}
                      </p>
                    ))}
                </div>

                <button
                  css={s.feedbackBtn}
                  disabled={!planIdForFeedback || isPending}
                  onClick={() => generateFeedback()}
                >
                  {isPending
                    ? "생성 중..."
                    : aiReport
                      ? "피드백 업데이트"
                      : "AI 피드백 생성"}
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