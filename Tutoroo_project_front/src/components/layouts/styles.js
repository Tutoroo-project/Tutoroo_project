import { css } from "@emotion/react";

export const header = css`
  width: 100%;
  height: 72px;
  border-bottom: 1px solid #dbdbdb;
  background: #fff;
`;

export const inner = css`
  width: 1270px;
  height: 100%;
  margin: 0 auto;

  display: flex;
  align-items: center;
  justify-content: space-between;
`;

export const logoWrap = css`
  display: flex;
  align-items: center;
`;

export const logoImg = css`
  width: 230px;
  height: 50px;
  object-fit: contain;
`;

export const profileBtn = css`
  height: 40px;
  margin-right: 40px;
  padding: 0 16px;
  border-radius: 12px;
  border: 1px solid #000;
  background: #ffd8a8;
  font-weight: 600;
  cursor: pointer;
`;
