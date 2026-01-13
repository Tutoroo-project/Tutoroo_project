import { css } from "@emotion/react";
import { theme } from "../../styles/theme";

export const statusWidget = css`
  width: 160px; 
  height: 80px; 
  border: 2px solid ${theme.colors.primary};
  border-radius: 12px; 
  background-color: #fff;
  display: flex; 
  flex-direction: column; 
  align-items: center; 
  justify-content: center;
`;

export const statusLabel = css`
  font-size: 14px; 
  color: ${theme.colors.primary}; 
  font-weight: 600;
`;

export const timerText = css`
  font-size: 24px; 
  font-weight: 700; 
  color: ${theme.colors.primary};
`;