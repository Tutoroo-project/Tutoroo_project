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
`;
