import { css } from "@emotion/react";

export const pageBg = css`
  background-color: #fff;
  min-height: calc(100vh - 72px);
  padding-bottom: 80px;
`;

export const container = css`
  max-width: 1440px;
  min-width: 800px;
  margin: 0 auto;
  padding-top: 60px;
`;

export const topSection = css`
  position: relative;
  text-align: center;
  margin-bottom: 50px;
  display: flex;
  justify-content: center;
  align-items: center;
`;

export const pageTitle = css`
  font-size: 32px;
  font-weight: 800;
  color: #000;
`;

export const filterWrap = css`
  position: absolute;
  right: 0;
  display: flex;
  gap: 12px;
`;

export const filterSelect = css`
  padding: 8px 16px;
  border-radius: 20px;
  border: 1px solid #ccc;
  background: #fff;
  font-size: 14px;
  cursor: pointer;
  outline: none;
  &:hover {
     border-color: #FF9F43; 
    }
`;

export const contentRow = css`
  display: flex;
  gap: 40px;
  align-items: flex-start;
`;

export const rankListArea = css`
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
`;

export const rankCard = (rank) => css`
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 80px;
  padding: 0 40px;
  border-radius: 16px;
  border: 1px solid #E5E5E5;
  background-color: #fff;
  
    ${
    rank <= 3 && `
    border-color: #FF9F43;
    box-shadow: 0 4px 12px rgba(255, 159, 67, 0.1);
    `}
`;

export const rankNullText = css`
  text-align: center;
  padding: "50px";
  color: "#666";
`;

export const loadingText = css`
  text-align: center;
  padding: 50px;
`;

export const isUnauthenticated = css`
  text-align: center;
  color: #999;
  padding: 20px;
`;

export const rankBadge = (rank) => css`
  display: flex;
  align-items: center;
  font-size: 20px;
  font-weight: 800;
  width: 100px;
  color: ${rank === 1 ? "#FFB703" : rank === 2 ? "#A9A9A9" : rank === 3 ? "#CD7F32" : "#333"};
  
    .medal-icon { 
      margin-right: 8px; font-size: 24px; 
    }
`;

export const userInfo = css`
  display: flex;
  justify-content: center;
  align-items: center;
  flex: 1;
  gap: 16px;
  margin-top: 20px;
`;

export const userIcon = css`
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 1px solid #eee;
  background-color: #f9f9f9;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23ccc'%3E%3Cpath d='M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: center;
`;

export const userProfileImg = css`
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #eee;
`;

export const userName = css`
  font-size: 20px;
  font-weight: 700;
  color: #000;
`;

export const pointText = css`
  font-size: 20px;
  font-weight: 700;
  color: #FF9F43;
`;

export const myStatusArea = css`
  width: 400px;
  flex-shrink: 0;
`;

export const statusCard = css`
  border: 1px solid #d9d9d9;
  border-radius: 16px;
  padding: 30px;
  background: #fff;
`;

export const cardTitle = css`
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 30px;
  text-align: center;
  color: #333;
`;

export const cardContent = css`
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: center; 
`;

export const cardLabel = css`
  font-size: 14px;
  color: #999;
`;

export const bigPoint = css`
  font-size: 36px;
  font-weight: 800;
  color: #FF9F43;
`;
