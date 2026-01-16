import { css } from "@emotion/react";
import { theme } from "../../styles/theme";

export const pageContainer = css`
  width: 100%; height: calc(100vh - 80px); display: flex; flex-direction: column;
`;

export const chatArea = css`
  flex: 1; 
  width: ${theme.layout.contentWidth}; 
  margin: 0 auto; 
  overflow-y: auto; 
  padding: 40px 0;
  display: flex;
  flex-direction: column;
  gap: 20px; /* 말풍선 사이 간격 */
`;

/* ✅ 말풍선 스타일 추가 */
export const messageRow = (isUser) => css`
  display: flex;
  justify-content: ${isUser ? "flex-end" : "flex-start"};
  padding: 0 20px;
`;

export const bubble = (isUser) => css`
  max-width: 60%;
  padding: 12px 20px;
  border-radius: 16px;
  font-size: 16px;
  line-height: 1.5;
  
  background-color: ${isUser ? theme.colors.primary : "#F0F0F0"};
  color: ${isUser ? "#fff" : "#333"};
  
  border-top-right-radius: ${isUser ? "4px" : "16px"};
  border-top-left-radius: ${isUser ? "16px" : "4px"};
`;

export const aiProfileIcon = css`
  width: 40px; height: 40px; border-radius: 50%; background: #eee; margin-right: 12px;
  /* 여기에 실제 튜터 이미지 background-image로 넣어도 됨 */
`;

/* 하단 영역 (기존 유지) */
export const bottomArea = css`
  width: 100%; border-top: 1px solid #eee; padding: 20px 0; background: #fff;
`;

export const bottomInner = css`
  width: ${theme.layout.contentWidth}; margin: 0 auto; display: flex; align-items: center; gap: 20px;
`;

export const inputWrapper = css` flex: 1; `;

export const inputBox = css`
  width: 100%; height: 50px; padding: 0 20px; border-radius: 25px; border: 1px solid #ccc; font-size: 16px; outline: none;
  &:focus { border-color: ${theme.colors.primary}; }
`;

export const sendBtn = css`
  width: 80px; height: 50px; border-radius: 25px; background-color: ${theme.colors.primary}; color: white; font-weight: 700;
  &:hover { background-color: ${theme.colors.accent}; }
`;

export const placeholder = css` text-align: center; color: #aaa; margin-top: 100px; `;