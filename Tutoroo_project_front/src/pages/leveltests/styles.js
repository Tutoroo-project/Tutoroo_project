import { css } from "@emotion/react";
import { theme } from "../../styles/theme";

export const pageContainer = css`
  width: 100%;
  height: calc(100vh - 80px);
  display: flex;
  flex-direction: column;
`;

export const page = css`
  width: ${theme.layout.contentWidth};
  margin: 0 auto;
  padding: 40px 0 80px;
`;

export const chatArea = css`
  flex: 1;
  width: ${theme.layout.contentWidth};
  margin: 0 auto;
  overflow-y: auto;
  padding: 40px 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
`;

export const bottomArea = css`
  width: 100%;
  border-top: 1px solid #eee;
  padding: 20px 0;
  background: #fff;
`;

export const bottomInner = css`
  position: relative;
  width: ${theme.layout.contentWidth};
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 20px;
`;

export const inputWrapper = css`
  position: relative;
  flex: 1;
  display: flex;
  align-items: center;

  background-color: #f5f5f5;
  border-radius: 22px;
  padding: 0 12px;
`;

export const inputBox = css`
  height: 50px;
  flex: 1;
  padding: 0 12px;
  border: none;
  background: transparent;
  font-size: 14px;
  outline: none;
`;

export const sendBtn = css`
  width: 80px;
  height: 50px;
  border-radius: 25px;
  background-color: ${theme.colors.primary};
  color: white;
  font-weight: 700;

  &:hover {
    background-color: ${theme.colors.accent};
  }
`;

export const aiBubble = css`
  align-self: flex-start;
  max-width: 70%;
  padding: 14px 18px;
  border-radius: 14px;
  background-color: #f5f5f5;
  border: 1px solid #ddd;
  white-space: pre-wrap;
  word-break: break-word;
`;

export const userBubble = css`
  align-self: flex-end;
  max-width: 70%;
  padding: 14px 18px;
  border-radius: 14px;
  background-color: ${theme.colors.primary};
  color: white;
  white-space: pre-wrap;
  word-break: break-word;
`;

export const plusBtn = css`
  width: 28px;
  height: 28px;
  border-radius: 50%;

  border: none;
  background-color: #fff;
  color: #ff8a3d;
  font-size: 20px;
  font-weight: 600;

  cursor: pointer;
  margin-right: 8px;

  display: flex;
  align-items: center;
  justify-content: center;

  &:hover {
    background-color: #ffe8d6;
  }
`;

export const plusMenu = css`
  position: absolute;
  bottom: 44px;
  left: 8px;

  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);

  padding: 6px;
  z-index: 20;
`;

export const menuItem = css`
  width: 160px;
  padding: 8px 12px;

  background: none;
  border: none;
  border-radius: 8px;

  display: flex;
  align-items: center;
  gap: 8px;

  font-size: 14px;
  cursor: pointer;

  &:hover {
    background-color: #f5f5f5;
  }
`;

export const resultFooter = css`
  width: 100%;
  height: 96px;
  position: fixed;
  bottom: 0;
  left: 0;

  display: flex;
  align-items: center;
  justify-content: center;

  background-color: #ffffff;
  border-top: 1px solid #eee;
  z-index: 100;
`;

export const resultBtn = css`
  width: 320px;
  height: 52px;

  background-color: #ff8a3d;
  color: #fff;
  font-size: 16px;
  font-weight: 600;

  border: none;
  border-radius: 26px;
  cursor: pointer;

  box-shadow: 0 8px 20px rgba(255, 138, 61, 0.3);

  &:hover {
    background-color: #ff7a20;
  }
`;

export const resultCard = css`
  background-color: #ffffff;
  border-radius: 16px;
  padding: 28px 32px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.06);
`;

export const tutorMessage = css`
  font-size: 14px;
  color: ${theme.colors.primary};
  margin-bottom: 8px;
`;

export const title = css`
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 24px;
`;

export const summaryGrid = css`
  display: flex;
  gap: 20px;
  margin-bottom: 16px;
`;

export const summaryItem = css`
  flex: 1;
  background-color: #f5f5f5;
  border-radius: 12px;
  padding: 16px 20px;

  span {
    display: block;
    font-size: 13px;
    color: #888;
    margin-bottom: 6px;
  }

  strong {
    font-size: 18px;
    font-weight: 600;
    color: #222;
  }
`;

export const description = css`
  margin-top: 8px;
  font-size: 14px;
  line-height: 1.6;
  color: #555;
`;

export const roadmapSection = css`
  margin-top: 48px;
`;

export const sectionTitle = css`
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 16px;
`;

export const roadmapImageWrapper = css`
  width: 100%;
  margin-top: 16px;
  display: flex;
  justify-content: center;
`;

export const roadmapImage = css`
  width: 100%;
  max-width: 800px;
  height: auto;
  display: block;
  object-fit: contain;
  border-radius: 12px;
`;

export const roadmapHint = css`
  margin-top: 12px;
  text-align: center;
  font-size: 13px;
  color: #888;
`;

export const naviArea = css`
  margin-top: 48px;
  display: flex;
  justify-content: center;
`;

export const primaryBtn = css`
  width: 320px;
  height: 52px;
  border-radius: 26px;

  background-color: ${theme.colors.primary};
  color: #ffffff;
  font-size: 16px;
  font-weight: 600;

  border: none;
  cursor: pointer;

  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.15);
  transition:
    background-color 0.2s ease,
    transform 0.15s ease;

  &:hover {
    background-color: ${theme.colors.accent};
    transform: translateY(-1px);
  }

  &:active {
    transform: translateY(0);
  }
`;

export const roadmapList = css`
  margin: 16px 0 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 12px;
`;

export const roadmapItem = css`
  background: #fff;
  border: 1px solid #eee;
  border-radius: 12px;
  padding: 14px 16px;
`;

export const roadmapItemTitle = css`
  font-weight: 700;
  margin-bottom: 8px;
`;

export const roadmapItemDesc = css`
  margin: 8px 0 0;
  color: #666;
  line-height: 1.5;
`;

export const topicList = css`
  margin: 6px 0 0;
  padding-left: 18px;
`;

export const topicItem = css`
  color: #444;
  line-height: 1.5;
`;
