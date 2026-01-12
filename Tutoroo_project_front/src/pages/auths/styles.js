import { css } from "@emotion/react";

/* =========================
   Page
========================= */

export const page = css`
  min-height: 100vh;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
`;

/* =========================
   Card
========================= */

export const card = css`
  width: 400px;
  padding: 40px;
  border-radius: 16px;
  border: 1px solid #e5e5e5;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
  background: #fff;
`;

/* =========================
   Logo
========================= */

export const logoArea = css`
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 32px;

  img {
    width: 48px;
    height: 48px;
    margin-bottom: 8px;
  }

  h1 {
    font-size: 24px;
    font-weight: 800;
    color: #ff8a3d;
  }
`;

/* =========================
   Form
========================= */

export const form = css`
  display: flex;
  flex-direction: column;
  gap: 12px;

  input {
    height: 44px;
    padding: 0 12px;
    border-radius: 10px;
    border: 1px solid #ccc;
    font-size: 14px;
  }
`;

export const loginBtn = css`
  height: 44px;
  margin-top: 8px;
  border-radius: 12px;
  border: none;
  background: #ff8a3d;
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
`;

/* =========================
   Links
========================= */

export const links = css`
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #666;

  span {
    cursor: pointer;
  }
`;

/* =========================
   Social Login
========================= */

export const socialArea = css`
  margin-top: 32px;
  text-align: center;

  p {
    font-size: 13px;
    color: #999;
    margin-bottom: 12px;
  }
`;

export const socialBtns = css`
  display: flex;
  justify-content: center;
  gap: 16px;

  button {
    width: 44px;
    height: 44px;
    border-radius: 50%;
    font-weight: 700;
    color: #fff;
    border: none;
    cursor: pointer;
  }
`;

export const kakao = css`
  background: #fee500;
  color: #000;
`;

export const naver = css`
  background: #03c75a;
`;

export const google = css`
  background: #ea4335;
`;
