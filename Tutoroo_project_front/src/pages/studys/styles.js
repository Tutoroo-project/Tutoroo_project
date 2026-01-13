import { css } from "@emotion/react";
import { theme } from "../../styles/theme";

export const pageContainer = css`
  width: 100%; 
  height: calc(100vh - 80px); 
  display: flex; 
  flex-direction: column;
`;

export const chatArea = css`
  flex: 1; 
  width: ${theme.layout.contentWidth}; 
  margin: 0 auto; 
  overflow-y: auto; 
  padding: 40px 0;
`;

export const placeholder = css`
  text-align: center; 
  color: #aaa; 
  margin-top: 100px;
`;

export const bottomArea = css`
  width: 100%; 
  border-top: 1px solid #eee; 
  padding: 20px 0; 
  background: #fff;
`;

export const bottomInner = css`
  width: ${theme.layout.contentWidth}; 
  margin: 0 auto; 
  display: flex; 
  align-items: center; 
  gap: 20px;
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