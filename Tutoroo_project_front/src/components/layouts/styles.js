import { css } from "@emotion/react";

export const header = css`
  width: 100%;
  height: 72px;
  border-bottom: 1px solid #dbdbdb;
  background: #fff;
`;

export const inner = css`
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 1440px;
  min-width: 800px;
  height: 72px;
  margin: 0 auto;
`;

export const logoWrap = css`
  display: flex;
  align-items: center;
  cursor: pointer;
`;

export const logoImg = css`
  width: 230px;
  height: 50px;
  object-fit: contain;
`;

export const profileBtn = css`
  width: 90px;
  height: 34px;
  padding: 0 16px;
  border-radius: 12px;
  border: 1px solid #dbdbdb;
  background: #ffd8a8;
  font-weight: 500;
  cursor: pointer;

  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  white-space: nowrap;
  line-height: 1;
  text-decoration: none;
`;

export const profileWrap = css`
  position: relative;
  display: flex;
  align-items: center;
`;

export const profileBtnActive = css`
  border-color: #ff8a3d;
`;

export const caret = css`
  display: inline-block;
  margin-left: 4px;
  transition: transform 0.15s ease;
`;

export const caretOpen = css`
  transform: rotate(180deg);
`;

export const profileMenu = css`
  position: absolute;
  right: 0;
  top: calc(100% + 8px);
  width: 170px;
  padding: 8px;
  border: 1px solid #dbdbdb;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  z-index: 50;
`;

export const menuItem = css`
  width: 100%;
  height: 40px;
  padding: 0 12px;
  border: none;
  background: transparent;
  border-radius: 10px;
  text-align: left;
  font-size: 14px;
  cursor: pointer;

  &:hover {
    background: #fff1e0;
  }
`;

export const menuDivider = css`
  height: 1px;
  margin: 8px 0;
  background: rgba(0, 0, 0, 0.08);
`;

export const logoutItem = css`
  color: #e11d48;
  font-weight: 600;
`;