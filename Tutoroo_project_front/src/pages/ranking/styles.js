import { css } from "@emotion/react";

// --- [기존 페이지 레이아웃] ---
export const pageBg = css`
  background-color: #f5f5f5;
  min-height: calc(100vh - 72px);
  padding-bottom: 80px;
`;

export const container = css`
  max-width: 1200px;
  min-width: 800px;
  margin: 0 auto;
  padding-top: 60px;
  padding-left: 20px;
  padding-right: 20px;
`;

export const topSection = css`
  position: relative;
  text-align: center;
  margin-bottom: 40px;
  display: flex;
  justify-content: space-between;
  align-items: center;
`;

export const pageTitle = css`
  font-size: 32px;
  font-weight: 800;
  color: #333;
`;

export const filterWrap = css`
  display: flex;
  gap: 12px;
`;

export const filterSelect = css`
  padding: 8px 16px;
  border-radius: 8px;
  border: 1px solid #ddd;
  background: #fff;
  font-size: 14px;
  cursor: pointer;
  outline: none;
  &:hover {
     border-color: #FF9F43;
  }
`;

export const contentWrap = css`
  display: flex;
  gap: 24px;
  align-items: flex-start;
`;

export const rankListArea = css`
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
`;

export const loadingText = css`
  text-align: center;
  padding: 40px;
  color: #888;
`;

export const rankCard = (rank) => css`
  display: flex;
  align-items: center;
  background-color: #fff;
  border-radius: 12px;
  padding: 16px 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  transition: transform 0.2s;
  border: ${rank <= 3 ? '1px solid #FFB703' : '1px solid transparent'};
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  }
`;

export const rankBadge = (rank) => css`
  width: 60px;
  font-size: ${rank <= 3 ? '20px' : '18px'};
  font-weight: bold;
  color: ${rank <= 3 ? '#FF9F43' : '#666'};
  display: flex;
  align-items: center;
  gap: 4px;
  
  .medal-icon {
    font-size: 24px;
  }
`;

export const userInfo = css`
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: 20px;
`;

export const profileFrame = css`
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background-color: #E0E0E0; 
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden; 
  border: 2px solid #fff;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
`;

export const profileImgContent = css`
  width: 100%;
  height: 100%;
  object-fit: cover; 
`;

export const userProfileImg = css`
  width: 48px;
  height: 48px;
  border-radius: 50%;
`;

export const userName = css`
  font-size: 18px;
  font-weight: 600;
  color: #333;
`;

export const pointText = css`
  font-size: 18px;
  font-weight: 700;
  color: #FF9F43;
`;


// --- [내 랭킹 카드 스타일] ---
export const myStatusArea = css`
  width: 320px;
  position: sticky;
  top: 100px;
`;

export const statusCard = css`
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.08);
  border: 1px solid #eee;
  text-align: center;
`;