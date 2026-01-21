import { css } from "@emotion/react";

export const pageBg = css`
  background-color: #fff;
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
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
  text-align: center;
  margin-bottom: 40px;
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
  align-items: flex-start;
  gap: 24px;
`;

// 리스트
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
  border: ${rank <= 3 ? '0.1px solid #FFB703' : '0.1px solid #999'};
  
  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  }
`;

export const rankBadge = (rank) => css`
  display: flex;
  align-items: center;
  width: 70px;
  font-size: ${rank <= 3 ? '20px' : '18px'};
  font-weight: bold;
  color: ${rank <= 3 ? '#FF9F43' : '#666'};
  gap: 4px;
  
  .medal-icon {
    font-size: 20px;
  }
`;

export const userInfo = css`
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: 20px;
`;

export const userProfileImg = (imageUrl) => css`
  width: 48px;
  height: 48px;
  border-radius: 50%;
  border: 2px solid #fff;
  background-color: #fff;  
  background-image: url(${imageUrl});
  background-position: center;
  background-size: cover;
  background-repeat: no-repeat;
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

// 카드
export const myStatusArea = css`
  width: 320px;
  position: sticky;
  top: 100px;
`;

export const statusCard = css`
  text-align: center;
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.08);
  border: 1px solid #eee;
`;

export const cardTitle = css`
  font-size: 18px;
  font-weight: 700;
  color: #333;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 2px solid #f5f5f5;
`;

export const cardContent = css`
  display: flex;
  flex-direction: column;
  align-items: center;
`;

export const cardLabel = css`
  margin-bottom: 10px;
  font-size: 14px;
  color: #444;
`;

export const myProfileImg = (imageUrl) => css`
  width: 80px;
  height: 80px;
  border-radius: 50%;
  border: 4px solid #fff;
  background-color: #fff;
  background-image: url(${imageUrl});
  background-position: center;
  background-size: cover;
`;

export const bigPoint = css`
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff8f0;
  border-radius: 12px;
  font-size: 26px;
  font-weight: 700;
  color: #FF9F43;
  gap: 8px;
  padding: 12px 20px;
  width: 100%;

  .rank {
    font-size: 0.6em;
    color: #666;
    margin-right: 4px;
  }

  .point-value {
    margin-left: 8px;
  }
`;

export const isUnauthenticated = css`
  color: #999;
  font-size: 15px;
  line-height: 1.6;
  padding: 20px 0;
`;

export const myUserInfo = css`
  display: flex;
  flex-direction: column; 
  align-items: center;
  margin-bottom: 10px;
`;