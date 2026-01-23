import { css } from "@emotion/react";
import { theme } from "../../styles/theme"; 

export const pageContainer = css`
  display: flex;
  flex-direction: column;
  height: calc(100vh - 60px); 
  background-color: #f9f9f9;
  position: relative;
`;

export const chatArea = css`
  flex: 1;
  padding: 40px 20px 120px; 
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

/* [수정] AI 프로필 아이콘: 이미지 태그를 감싸도록 변경 */
export const aiProfileIcon = css`
  width: 54px;
  height: 54px;
  border-radius: 50%;
  background-color: #fff;
  border: 1px solid #ddd;
  flex-shrink: 0;
  
  /* 내부 이미지 중앙 정렬 및 둥글게 처리 */
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
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
  
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
  padding: 20px 0;
  z-index: 100;
  box-shadow: 0 -4px 20px rgba(0,0,0,0.03);
`;

export const bottomInner = css`
  width: ${theme.layout.contentWidth};
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 16px;
`;

export const inputWrapper = css`
  flex: 1;
  position: relative;
`;

export const inputBox = css`
  width: 100%;
  height: 56px;
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
`;

export const sendBtn = css`
  width: 80px;
  height: 56px;
  border-radius: 100px;
  background-color: ${theme.colors.primary};
  color: #fff;
  font-weight: 700;
  border: none;
  cursor: pointer;
  transition: background-color 0.2s;
  font-size: 16px;

  &:hover {
    background-color: ${theme.colors.primaryDark || "#e0a800"};
  }

  &:disabled {
    background-color: #ddd;
    cursor: not-allowed;
  }
`;