import { css } from "@emotion/react";
import { theme } from "../../styles/theme";

export const pageContainer = css`
  display: flex;
  flex-direction: column;
  height: calc(100vh - 80px);
`;

export const chatArea = css`
  display: flex;
  flex-direction: column;
  flex: 1; 
  width: ${theme.layout.contentWidth};
  min-width: 800px;
  margin: 0 auto; 
  overflow-y: auto; 
  padding: 40px 0;
  gap: 20px;
`;

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
  width: 40px; 
  height: 40px; 
  border-radius: 50%; 
  background: #eee;
  margin-right: 12px;
`;

export const bottomArea = css`
  width: 100%; 
  border-top: 1px solid #eee; 
  background: #fff;
`;

export const bottomInner = css`
  display: flex; 
  align-items: center; 
  min-width: 800px;
  max-width: 1440px;
  margin: 0 auto; 
  gap: 40px;
`;

export const inputWrapper = css` 
  flex: 1; 
`;

export const inputBox = css`
  width: 100%; 
  height: 50px; 
  padding: 0 20px; 
  border-radius: 25px; 
  border: 1px solid #ccc; 
  font-size: 16px; 
  outline: none;
  
    &:focus { 
      border-color: ${theme.colors.primary}; 
    }
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

export const placeholder = css` 
  text-align: center; 
  color: #aaa; 
  margin-top: 100px; `
;