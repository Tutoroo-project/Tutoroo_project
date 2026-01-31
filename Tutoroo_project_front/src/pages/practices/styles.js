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

// --- 스타일 정의 ---

export const pageContainer = css`
  display: flex;
  flex-direction: column;
  height: calc(100vh - 60px); 
  background-color: #f9f9f9;
  position: relative;
`;

export const chatArea = css`
  flex: 1;
  /* Footer에 툴바가 추가되어 높이가 커졌으므로 하단 여백을 넉넉히 줍니다 */
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
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
  
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

// [수정] Bottom Area: 높이 유동적 대응 및 패딩 조정
export const bottomArea = css`
  position: fixed;
  bottom: 0;
  left: 0;
  width: 100%;
  background-color: #fff;
  border-top: 1px solid #eee;
  padding: 12px 0 20px; 
  z-index: 100;
  box-shadow: 0 -4px 20px rgba(0,0,0,0.03);
`;

export const bottomInner = css`
  width: ${theme.layout.contentWidth};
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap; /* 화면이 좁을 때 줄바꿈 허용 */
`;

// [New] 컨트롤 툴바 (스피커, 마이크, 다운로드 등)
export const controlToolbar = css`
  display: flex;
  align-items: center;
  gap: 8px;
  margin-right: 4px;
`;

// [New] 아이콘 버튼 스타일 (스피커, 마이크)
export const iconBtn = (isActive) => css`
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: 1px solid ${isActive ? theme.colors.primary : '#ddd'};
  background-color: ${isActive ? '#fff3e0' : '#f8f8f8'};
  color: ${isActive ? theme.colors.primary : '#888'};
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

// [New] 텍스트 버튼 (PDF 다운로드 등)
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
  white-space: nowrap; /* 텍스트 줄바꿈 방지 */

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
  min-width: 200px; /* 너무 줄어들지 않도록 최소 너비 설정 */
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

// [New] 녹음 중 표시 (말풍선 안의 애니메이션 텍스트)
export const recordingPulse = css`
  color: #ff4d4d;
  font-weight: bold;
  animation: ${pulse} 1.5s infinite ease-in-out;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  
  &::before {
    content: '●';
    font-size: 10px;
  }
`;