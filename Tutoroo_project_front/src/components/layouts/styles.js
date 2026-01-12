import { css } from "@emotion/react";

export const header = css`
  width: 100%;
  height: 72px;
  border-bottom: 1px solid #000;
  background: #fff;
`;

export const inner = css`
  width: 1200px;
  height: 100%;
  margin: 0 auto;

  display: flex;
  align-items: center;
  justify-content: space-between;
`;

export const logoWrap = css`
  display: flex;
  align-items: center;
  gap: 10px;
`;

export const logoImg = css`
  width: 36px;
  height: 36px;
  object-fit: contain;
`;

export const logoText = css`
  font-size: 22px;
  font-weight: 800;
  color: #ff8a3d;
`;

export const profileBtn = css`
  height: 40px;
  padding: 0 16px;
  border-radius: 12px;
  border: 1px solid #000;
  background: #ffd8a8;
  font-weight: 600;
  cursor: pointer;
`;
