import { css } from "@emotion/react";
import { theme } from "../../styles/theme"; 

export const container = css`
  width: ${theme.layout.contentWidth};
  margin: 0 auto;
  padding-top: 60px;
  display: flex;
  flex-direction: column;
  align-items: center;
`;

export const title = css`
  font-size: 32px;
  font-weight: 800;
  margin-bottom: 50px;
`;

export const contentWrap = css`
  display: flex;
  gap: 80px;
  justify-content: center;
  width: 100%;
`;

export const listPanel = css`
  width: 380px;
  display: flex;
  flex-direction: column;
  gap: 16px;
`;

export const detailPanel = css`
  width: 500px;
  height: 550px;
  border-radius: 20px;
  background-color: #FFF9F0; 
  border: 1px solid #EADDD2;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  box-sizing: border-box;
`;

export const tutorItem = (isActive) => css`
  display: flex;
  align-items: center;
  width: 100%;
  height: 72px; 
  padding: 0 24px;
  border-radius: 12px;
  border: 1px solid ${isActive ? theme.colors.primary : "#ddd"};
  background: ${isActive ? "#FFF" : "#FFF"};
  box-shadow: ${isActive ? "0 0 0 2px " + theme.colors.primary : "none"};
  cursor: pointer;
  
  /* ✅ 리스트 아이콘 이미지 스타일 */
  .icon { 
    width: 58px; 
    height: 58px; 
    object-fit: contain; 
    margin-right: 16px; 
  }
  
  .name { 
    padding-left: 24px;
    flex: 1; 
    text-align: left; 
    font-size: 20px;
    font-weight: 700; 
    color: ${isActive ? theme.colors.primary : "#333"}; 
  }

  .arrow { 
    color: #999; 
  }
`;

export const customBtn = (isActive) => css`
  ${tutorItem(isActive)};
  justify-content: center;
  height: 64px;
  .name { flex: unset; }
`;

export const infoBox = css`
  text-align: center; 
  width: 100%; 
  display: flex; 
  flex-direction: column;
  align-items: center;
`;

/* ✅ 상세 패널의 큰 프로필 이미지 스타일 */
export const detailProfileImg = css`
  width: 160px; 
  height: 160px; 
  object-fit: contain; 
  margin-bottom: 20px;
`;

export const guideText = css`
  font-size: 18px;
  margin-bottom: 20px; 
  word-break: keep-all; 
  line-height: 1.5;
`;

export const descBox = css`
  background: rgba(255,255,255,0.6); 
  padding: 16px; 
  border-radius: 8px; 
  margin-bottom: 30px; 
  width: 100%;
`;

export const customInput = css`
  width: 100%; 
  height: 120px; 
  padding: 16px; 
  border: 1px solid #ccc; 
  border-radius: 12px; 
  resize: none; 
  margin-bottom: 24px;
`;

export const startBtn = css`
  width: 100%; 
  height: 52px; 
  background-color: ${theme.colors.primary}; 
  color: #fff; 
  font-weight: 700; 
  font-size: 18px; 
  border-radius: 12px;
  
  &:hover { 
    background-color: ${theme.colors.accent}; 
  }
`;