import { css } from "@emotion/react";

/* =========================
   Page Layout
========================= */

export const pageBg = css`
  background-color: #ffffff;
  min-height: calc(100vh - 72px);
`;

export const container = css`
  width: 1200px;
  margin: 40px auto 0;
  padding-bottom: 40px;
`;

/* =========================
   Greeting
========================= */

export const greeting = css`
  display: flex;
  justify-content: space-between;
  align-items: center;
`;

export const greetingText = css`
  h2 {
    font-size: 28px;
    font-weight: 700;
    margin: 0;
  }

  p {
    font-size: 14px;
    color: #666;
    margin-top: 4px;
  }
`;

export const titleRow = css`
  display: flex;
  align-items: center;
  gap: 12px;

  h2 {
    font-size: 28px;
    font-weight: 600;
    margin: 0;
  }
`;

export const petBtn = css`
  padding: 6px 14px;
  border-radius: 20px;
  border: 1px solid #ff8a3d;
  background-color: #fff3e6;
  color: #ff8a3d;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background-color: #ff8a3d;
    color: #fff;
    transform: translateY(-2px);
  }
`;

export const actionWrap = css`
  display: flex;
  gap: 12px;
`;

export const select = css`
  height: 40px;
  padding: 0 12px;
  border-radius: 8px;
  border: 1px solid #dddddd;
  font-size: 14px;
  background-color: #fff;

  &:focus {
    outline: none;
    border-color: #ff8a3d;
  }
`;

export const studyBtn = css`
  width: 160px;
  height: 40px;
  border-radius: 12px;
  border: none;
  background: #ff8a3d;
  color: #fff;
  font-weight: 600;
`;

/* =========================
   Summary Cards
========================= */

export const cards = css`
  margin-top: 32px;
  display: flex;
  gap: 20px;
`;

export const card = css`
  flex: 1;
  height: 120px;
  padding: 16px;
  border-radius: 12px;
  border: 1px solid #d9d9d9;
  background: #fff;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);

  span {
    font-size: 13px;
    color: #666;
  }

  strong {
    display: block;
    margin-top: 16px;
    font-size: 15px;
    font-weight: 600;
  }
`;

/* =========================
   Progress
========================= */

export const progressRow = css`
  margin-top: 16px;
  position: relative;
`;

export const progressBar = css`
  height: 20px;
  background: #eee;
  border-radius: 8px;
  overflow: hidden;
`;

export const progressFill = (percent = 0) => {
  const safe = Math.min(100, Math.max(0, Number(percent) || 0));
  return css`
    height: 100%;
    width: ${safe}%;
    background: #ff8a3d;
    border-radius: 8px;
    transition: width 0.25s ease;
  `;
};

export const progressText = css`
  position: absolute;
  left: 8px;
  top: -1px;
  font-size: 12px;
  font-weight: 600;
  color: #ff8a3d;
`;

/* =========================
   Point / Rank
========================= */

export const pointText = css`
  margin-top: 12px;
  font-size: 20px;
  font-weight: 700;
  color: #ff8a3d;
`;

export const rankText = css`
  margin-top: 4px;
  font-size: 12px;
  color: #666;
`;

/* =========================
   Calendar
========================= */

export const calendarArea = css`
  margin-top: 40px;
  display: flex;
  align-items: center;
  gap: 12px;
`;

export const arrowBtn = css`
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid #ccc;
  background: #fff;
  cursor: pointer;
`;

export const calendarRow = css`
  display: flex;
  gap: 12px;
`;

export const calendarCard = (active, isToday) => css`
  width: 140px;
  height: 160px;
  border-radius: 12px;
  border: 2px solid ${active ? "#FF8A3D" : "#d9d9d9"};
  background: #fff;
  box-sizing: border-box;
  cursor: pointer;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;

  ${isToday &&
  `
    box-shadow: 0 0 0 3px rgba(255, 138, 61, 0.22);
  `}

  &:hover,
  &:focus {
    transform: scale(1.05);
  }
`;

export const calendarHeader = css`
  height: 32px;
  padding: 0 8px;
  background: #ffd8a8;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  font-weight: 600;
  font-size: 13px;
`;

export const headerLabel = css`
  min-width: 0;
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`;

export const headerBadges = css`
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
`;

export const todayBadge = css`
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 999px;
  background: #fff3e6;
  color: #ff8a3d;
  font-weight: 700;
`;

export const calendarBody = css`
  height: calc(100% - 32px);
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
`;

export const doneBadge = css`
  font-size: 12px;
  width: 18px;
  height: 18px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #ff8a3d;
  color: white;
  font-weight: 900;
`;

export const doneInfo = css`
  margin-top: auto;
  font-size: 11px;
  color: #ff8a3d;
  display: flex;
  gap: 8px;
`;

export const dayBadge = css`
  width: fit-content;
  padding: 2px 8px;
  border-radius: 999px;
  border: 1px solid #ff8a3d;
  background: #fff3e6;
  color: #ff8a3d;
  font-size: 11px;
  font-weight: 700;
`;

export const topicText = css`
  font-size: 13px;
  font-weight: 700;
  line-height: 1.2;

  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
`;

export const metaText = css`
  font-size: 11px;
  color: #666;
  line-height: 1.2;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

export const emptyCurriculum = css`
  font-size: 12px;
  color: #bbb;
`;

/* =========================
   More
========================= */

export const more = css`
  margin-top: 25px;
  text-align: center;
  font-size: 13px;
  color: #666;
  cursor: pointer;
`;

/* =========================
   Detail Section
========================= */

export const detailSection = css`
  margin-top: 40px;
  display: flex;
  flex-direction: column;
  gap: 24px;
`;

export const detailCard = css`
  padding: 24px;
  border-radius: 12px;
  border: 1px solid #d9d9d9;
  background: #fff;
`;

export const detailTitle = css`
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 16px;
`;

export const chartPlaceholder = css`
  height: 200px;
  border-radius: 8px;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
`;

export const progressFooter = css`
  margin-top: 12px;
  text-align: right;
  font-weight: 700;
  color: #ff8a3d;
`;

/* =========================
   AI Feedback
========================= */

export const feedbackCard = css`
  padding: 24px;
  border-radius: 12px;
  background: #fff3e6;
`;

export const feedbackText = css`
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 12px;
`;

export const feedbackList = css`
  font-size: 13px;
  line-height: 1.6;

  li {
    list-style: none;
  }
`;

export const chartBox = css`
  height: 200px;
  border-radius: 8px;
  background: #f5f5f5;
  padding: 12px;
`;
