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

// ✅ 쉬는시간 버튼 컨테이너
export const breakButtonContainer = css`
  display: flex;
  justify-content: center;
  margin-top: 20px;
  padding: 20px 0;
`;

// ✅ 쉬는시간 건너뛰기 버튼
export const skipBreakButton = css`
  padding: 14px 32px;
  border-radius: 24px;
  border: 2px solid ${theme.colors.primary};
  background: linear-gradient(135deg, #fff 0%, #fffbf5 100%);
  color: ${theme.colors.primary};
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(255, 138, 61, 0.2);
  
  display: flex;
  align-items: center;
  gap: 8px;

  &:hover {
    background: ${theme.colors.primary};
    color: #fff;
    transform: translateY(-2px);
    box-shadow: 0 6px 16px rgba(255, 138, 61, 0.3);
  }

  &:active {
    transform: translateY(0);
  }
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
  box-shadow: 0 -4px 20px rgba(0,0,0,0.03);
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
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  
  &::before {
    content: '●';
    font-size: 10px;
    color: #ff4d4d;
  }
`;

export const testOptions = css`
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
`;

export const optionButton = css`
  padding: 12px 16px;
  border-radius: 8px;
  border: 2px solid #e0e0e0;
  background-color: #fff;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 15px;

  &:hover {
    border-color: ${theme.colors.primary};
    background-color: #fffdf5;
  }
`;

export const fileInfo = css`
  font-size: 12px;
  color: #666;
  max-width: 150px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`;

export const feedbackContainer = css`
  width: 100%;
  display: flex;
  justify-content: center;
`;

export const feedbackSection = css`
  width: 600px;
  padding: 24px;
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
`;

export const feedbackLabel = css`
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 16px;
  text-align: center;
  color: #333;
`;

export const starContainer = css`
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-bottom: 20px;
`;

export const star = (isActive) => css`
  font-size: 40px;
  cursor: pointer;
  color: ${isActive ? '#FFD700' : '#ddd'};
  transition: all 0.2s;
  
  &:hover {
    transform: scale(1.2);
  }
`;

export const feedbackTextarea = css`
  width: 100%;
  padding: 12px;
  border-radius: 8px;
  border: 1px solid #ddd;
  font-size: 14px;
  resize: vertical;
  font-family: inherit;
  
  &:focus {
    outline: none;
    border-color: ${theme.colors.primary};
  }
`;

export const submitFeedbackBtn = css`
  width: 100%;
  height: 48px;
  margin-top: 16px;
  border-radius: 8px;
  background-color: ${theme.colors.primary};
  color: #fff;
  font-weight: 600;
  font-size: 16px;
  border: none;
  cursor: pointer;
  transition: background-color 0.2s;

  &:hover {
    background-color: ${theme.colors.primaryDark || "#e0a800"};
  }

  &:disabled {
    background-color: #ddd;
    cursor: not-allowed;
  }
`;

export const imageAttachedBadge = css`
  display: inline-block;
  padding: 4px 8px;
  border-radius: 4px;
  background-color: rgba(255, 255, 255, 0.2);
  font-size: 12px;
  margin-bottom: 8px;
  color: inherit;
`;

export const imagePreviewContainer = css`
  position: relative;
  width: 100px;
  height: 100px;
  margin-right: 8px;
  border-radius: 8px;
  overflow: hidden;
  border: 2px solid ${theme.colors.primary};
  flex-shrink: 0;
`;

export const imagePreview = css`
  width: 100%;
  height: 100%;
  object-fit: cover;
`;

export const removeImageBtn = css`
  position: absolute;
  top: 4px;
  right: 4px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background-color: rgba(0, 0, 0, 0.6);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border: none;
  font-size: 16px;
  
  &:hover {
    background-color: rgba(0, 0, 0, 0.8);
  }
`;