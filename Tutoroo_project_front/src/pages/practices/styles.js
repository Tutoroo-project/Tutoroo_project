import { css, keyframes } from "@emotion/react";
import { theme } from "../../styles/theme";

// --- 애니메이션 정의 ---
const pulse = keyframes`
  0% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(255, 77, 77, 0.7);
  }
  70% {
    transform: scale(1);
    box-shadow: 0 0 0 6px rgba(255, 77, 77, 0);
  }
  100% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(255, 77, 77, 0);
  }
`;

// --- 기존 스타일(유지) ---
export const pageContainer = css`
  display: flex;
  flex-direction: column;
  height: calc(100vh - 60px);
  background-color: #f9f9f9;
  position: relative;
`;

export const chatArea = css`
  flex: 1;
  padding: 40px 20px 160px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
  width: ${theme.layout.contentWidth};
  margin: 0 auto;

  &::-webkit-scrollbar {
    width: 8px;
  }
  &::-webkit-scrollbar-thumb {
    background-color: #ddd;
    border-radius: 4px;
  }
`;

export const messageRow = (isUser) => css`
  display: flex;
  justify-content: ${isUser ? "flex-end" : "flex-start"};
  align-items: flex-start;
  gap: 12px;
  width: 100%;
`;

export const aiProfileIcon = css`
  width: 54px;
  height: 54px;
  border-radius: 50%;
  background-color: #fff;
  border: 1px solid #ddd;
  flex-shrink: 0;

  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;

  img {
    width: 85%;
    height: 85%;
    object-fit: contain;
  }
`;

export const bubble = (isUser) => css`
  max-width: 60%;
  padding: 16px 22px;
  border-radius: 20px;
  font-size: 16px;
  line-height: 1.6;
  white-space: pre-wrap;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);

  & > img {
    display: block;
    object-fit: contain;
    max-width: 100%;
    border-radius: 8px;
    margin-bottom: 10px;
  }

  ${isUser
    ? css`
        background-color: ${theme.colors.primary};
        color: #fff;
        border-top-right-radius: 4px;
      `
    : css`
        background-color: #fff;
        color: #333;
        border: 1px solid #e0e0e0;
        border-top-left-radius: 4px;
      `}
`;

export const placeholder = css`
  text-align: center;
  margin-top: 100px;
  color: #999;
  font-size: 16px;
`;

export const bottomArea = css`
  position: fixed;
  bottom: 0;
  left: 0;
  width: 100%;
  background-color: #fff;
  border-top: 1px solid #eee;
  padding: 12px 0 20px;
  z-index: 100;
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.03);
`;

export const bottomInner = css`
  width: ${theme.layout.contentWidth};
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
`;

export const controlToolbar = css`
  display: flex;
  align-items: center;
  gap: 8px;
  margin-right: 4px;
`;

export const iconBtn = (isActive) => css`
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: 1px solid ${isActive ? theme.colors.primary : "#ddd"};
  background-color: ${isActive ? "#fff3e0" : "#f8f8f8"};
  color: ${isActive ? theme.colors.primary : "#888"};
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;

  &:hover {
    background-color: #fff;
    border-color: ${theme.colors.primary};
    color: ${theme.colors.primary};
  }

  &:active {
    transform: scale(0.95);
  }
`;

export const textBtn = css`
  height: 44px;
  padding: 0 16px;
  border-radius: 22px;
  border: 1px solid #ddd;
  background-color: #fff;
  color: #666;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;

  &:hover {
    border-color: ${theme.colors.primary};
    color: ${theme.colors.primary};
    background-color: #fffdf5;
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
`;

export const inputWrapper = css`
  flex: 1;
  position: relative;
  min-width: 200px;
`;

export const inputBox = css`
  width: 100%;
  height: 50px;
  padding: 0 24px;
  border-radius: 100px;
  border: 1px solid #ddd;
  font-size: 16px;
  background-color: #f8f8f8;
  transition: all 0.2s;

  &:focus {
    outline: none;
    border-color: ${theme.colors.primary};
    background-color: #fff;
    box-shadow: 0 0 0 4px rgba(255, 165, 0, 0.1);
  }

  &:disabled {
    background-color: #eee;
    color: #999;
    cursor: not-allowed;
  }
`;

export const sendBtn = css`
  width: 70px;
  height: 50px;
  border-radius: 100px;
  background-color: ${theme.colors.primary};
  color: #fff;
  font-weight: 700;
  border: none;
  cursor: pointer;
  transition: background-color 0.2s;
  font-size: 16px;
  flex-shrink: 0;

  &:hover {
    background-color: ${theme.colors.primaryDark || "#e0a800"};
  }

  &:disabled {
    background-color: #ddd;
    cursor: not-allowed;
  }
`;

export const recordingPulse = css`
  color: #ff4d4d;
  font-weight: bold;
  animation: ${pulse} 1.5s infinite ease-in-out;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;

  &::before {
    content: "●";
    font-size: 10px;
  }
`;

/* =========================
   Infinite Practice (QA) UI
========================= */

export const headerPanel = css`
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;
  padding: 18px 20px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid #eee;
`;

export const pageTitle = css`
  margin: 0;
  font-size: 22px;
  font-weight: 800;
  color: #222;
`;

export const pageSubTitle = css`
  margin: 6px 0 0;
  color: #666;
  font-size: 13px;
`;

export const badgeRow = css`
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
`;

export const badge = css`
  font-size: 12px;
  padding: 6px 10px;
  border-radius: 999px;
  background: #f7f7f7;
  border: 1px solid #eee;
  color: #555;
`;

export const questionCard = css`
  background: #fff;
  border: 1px solid #eee;
  border-radius: 16px;
  padding: 18px 20px;
`;

export const questionHeader = css`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
`;

export const questionTitleRow = css`
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
`;

export const qNo = css`
  font-size: 14px;
  font-weight: 800;
  color: #222;
`;

export const typeTag = css`
  font-size: 12px;
  font-weight: 700;
  padding: 4px 10px;
  border-radius: 999px;
  background: #fff3e0;
  border: 1px solid #ffe0b2;
  color: ${theme.colors.primary};
`;

export const topicTag = css`
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 999px;
  background: #f7f7f7;
  border: 1px solid #eee;
  color: #666;
`;

export const questionText = css`
  font-size: 15px;
  line-height: 1.7;
  color: #222;
  white-space: pre-wrap;
`;

export const referenceImage = css`
  margin-top: 12px;
  width: 100%;
  max-height: 360px;
  object-fit: contain;
  border-radius: 12px;
  border: 1px solid #eee;
  background: #fafafa;
`;

export const optionsWrapper = css`
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
`;

export const optionItem = (checked) => css`
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid ${checked ? theme.colors.primary : "#eee"};
  background: ${checked ? "#fff3e0" : "#fafafa"};
  cursor: pointer;

  input {
    accent-color: ${theme.colors.primary};
  }

  span {
    font-size: 14px;
    color: #222;
  }
`;

export const answerTextarea = css`
  margin-top: 14px;
  width: 100%;
  min-height: 120px;
  resize: vertical;
  padding: 14px 16px;
  border-radius: 12px;
  border: 1px solid #ddd;
  background: #fafafa;
  font-size: 14px;
  line-height: 1.7;

  &:focus {
    outline: none;
    border-color: ${theme.colors.primary};
    background: #fff;
    box-shadow: 0 0 0 4px rgba(255, 165, 0, 0.12);
  }
`;

export const controlGroup = css`
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
`;

export const controlLabel = css`
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #555;
  font-weight: 700;
`;

export const selectBox = css`
  height: 38px;
  border-radius: 10px;
  border: 1px solid #ddd;
  background: #fff;
  padding: 0 10px;
  font-weight: 700;
  color: #333;
`;

export const checkboxLabel = css`
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #555;
  font-weight: 700;

  input {
    accent-color: ${theme.colors.primary};
  }
`;

export const actionGroup = css`
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
`;

export const actionBtnPrimary = css`
  height: 44px;
  padding: 0 16px;
  border-radius: 12px;
  background-color: ${theme.colors.primary};
  color: #fff;
  border: none;
  font-weight: 800;
  cursor: pointer;

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
`;

export const actionBtn = css`
  height: 44px;
  padding: 0 16px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid #ddd;
  color: #333;
  font-weight: 800;
  cursor: pointer;

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
`;

export const resultCard = css`
  background: #fff;
  border: 1px solid #eee;
  border-radius: 16px;
  padding: 18px 20px;
`;

export const resultHeader = css`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;

  h3 {
    margin: 0;
    font-size: 18px;
    font-weight: 900;
    color: #222;
  }
`;

export const scorePill = css`
  padding: 6px 12px;
  border-radius: 999px;
  background: #f7f7f7;
  border: 1px solid #eee;
  font-size: 13px;
  font-weight: 900;
  color: #333;
`;

export const resultSummary = css`
  margin: 10px 0 0;
  color: #444;
  line-height: 1.7;
  white-space: pre-wrap;
`;

export const resultList = css`
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
`;

export const resultItem = css`
  padding: 14px 16px;
  border-radius: 12px;
  border: 1px solid #eee;
  background: #fafafa;
`;

export const resultItemHeader = css`
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 8px;
`;

export const resultQNo = css`
  font-weight: 900;
  color: #222;
`;

export const correctPill = css`
  padding: 4px 10px;
  border-radius: 999px;
  background: #e8f5e9;
  border: 1px solid #c8e6c9;
  color: #2e7d32;
  font-size: 12px;
  font-weight: 900;
`;

export const wrongPill = css`
  padding: 4px 10px;
  border-radius: 999px;
  background: #ffebee;
  border: 1px solid #ffcdd2;
  color: #c62828;
  font-size: 12px;
  font-weight: 900;
`;

export const weakTag = css`
  padding: 4px 10px;
  border-radius: 999px;
  background: #f7f7f7;
  border: 1px solid #eee;
  color: #666;
  font-size: 12px;
  font-weight: 900;
`;

export const resultRow = css`
  margin-top: 6px;
  display: flex;
  gap: 8px;
  line-height: 1.6;

  b {
    flex-shrink: 0;
    color: #222;
  }

  span {
    color: #444;
    white-space: pre-wrap;
  }
`;

export const weakList = css`
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
`;

export const weakItem = css`
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid #eee;
  background: #fafafa;
`;

export const weakTopic = css`
  font-weight: 900;
  color: #222;
`;

export const weakMeta = css`
  margin-top: 4px;
  font-size: 13px;
  color: #666;
`;

export const recoBox = css`
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #eee;
`;

export const recoTitle = css`
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 900;
  color: #222;
`;

export const recoItem = css`
  margin-top: 6px;
  color: #444;
  line-height: 1.6;
  white-space: pre-wrap;
`;
