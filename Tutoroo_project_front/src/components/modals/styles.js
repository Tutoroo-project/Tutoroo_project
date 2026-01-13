import { css } from "@emotion/react";

/* ===============================
   Overlay
================================ */
export const overlay = css`
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(3px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
`;

/* ===============================
   Modal Container
================================ */
export const modal = css`
  width: 360px;
  padding: 24px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.18);
`;

/* ===============================
   Logo Area
================================ */
export const logo = css`
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 18px;

  img {
    width: 46px;
    height: 46px;
    object-fit: contain;
    margin-bottom: 6px;
  }

  h2 {
    font-size: 22px;
    font-weight: 700;
    color: #ff8a3d;
  }
`;

/* ===============================
   Form
================================ */
export const form = css`
  display: flex;
  flex-direction: column;
  gap: 8px;

  input {
    height: 40px;
    padding: 0 12px;
    border-radius: 8px;
    border: 1px solid #dcdcdc;
    font-size: 14px;

    &:focus {
      outline: none;
      border-color: #ff8a3d;
    }
  }
`;

/* ===============================
   Login / Submit Button
================================ */
export const submitBtn = css`
  margin-top: 12px;
  height: 44px;
  border-radius: 10px;
  background: #ff8a3d;
  color: #ffffff;
  font-size: 15px;
  font-weight: 700;
  border: none;
  cursor: pointer;

  &:hover {
    background: #ff7a1f;
  }
`;

/* ===============================
   Bottom Links
================================ */
export const linkRow = css`
  margin-top: 10px;
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #666;

  span {
    cursor: pointer;
  }
`;

export const signupLink = css`
  color: #ff8a3d;
  font-weight: 600;
`;

/* ===============================
   Social Login
================================ */
export const socialRow = css`
  margin-top: 16px;
  display: flex;
  justify-content: center;
  gap: 14px;
`;

export const socialBtn = css`
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  cursor: pointer;

  img {
    width: 22px;
    height: 22px;
    object-fit: contain;
  }
`;

/* --- Provider Colors --- */
export const naver = css`
  background: #03c75a;
`;

export const google = css`
  background: #ffffff;
  border: 1px solid #e5e5e5;
`;

export const kakao = css`
  background: #fee500;
`;
