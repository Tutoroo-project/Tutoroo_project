import { css } from "@emotion/react";

/* ===============================
    공통 Overlay / Modal
================================ */
export const overlay = css`
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
`;

/* ===============================
   Modal Container
================================ */
export const modal = css`
  width: 540px;
  max-height: 90vh;
  background-color: #ffffff;
  border-radius: 16px;
  padding: 28px 24px 32px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);

  overflow-y: auto;

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-thumb {
    background-color: #ddd;
    border-radius: 4px;
  }
`;

/* ===============================
   로고 영역
================================ */
export const logo = css`
  display: flex;
  flex-direction: column;
  align-items: center;

  img {
    width: 300px;
    height: 270px;
  }
`;

/* ===============================
   Form 공통
================================ */
export const form = css`
  display: flex;
  flex-direction: column;
  gap: 10px;

  input {
    height: 53px;
    border-radius: 8px;
    border: 1px solid #dddddd;
    padding: 0 12px;
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
  height: 53px;
  border-radius: 10px;
  background: #ff8a3d;
  color: black;
  font-size: 20px;
  font-weight: 700;
  border: none;
  cursor: pointer;

  &:hover {
    background: #ff7a1f;
  }
`;

export const optionRow = css`
  display: flex;
  justify-content: space-between;
  height: 15px;
`;

export const keepLogin = css`
  display: flex;
  gap: 3px;
  font-size: 13px;
  color: #888;
  cursor: pointer;

  input {
    padding: 0;
    height: 13px;
    accent-color: #ffffff;
    transform: scale(0.9);
  }
`;

export const fieldError = css`
  border-color: #ff3b30 !important;
  background: rgba(255, 59, 48, 0.06);

  &:focus {
    outline: none;
    border-color: #ff3b30 !important;
    box-shadow: 0 0 0 3px rgba(255, 59, 48, 0.12);
  }
`;

/* ===============================
   Bottom Links
================================ */
export const links = css`
  display: flex;
  gap: 10px;
  font-size: 12px;
  color: #999;

  span:hover {
    text-decoration: underline;
  }
`;
export const findId = css`
  font-size: 13px;
  color: #777777;
  cursor: pointer;

  &:hover {
    text-decoration: underline;
  }
`;

export const findPw = css`
  font-size: 13px;
  color: #777777;
  cursor: pointer;

  &:hover {
    text-decoration: underline;
  }
`;
/* =========================
   로그인 / 회원가입 버튼
========================= */

export const signupRow = css`
  display: flex;
  justify-content: center;
  margin-top: 14px;
`;

export const signupMent = css`
  font-size: 14px;
`;

export const signupLink = css`
  font-size: 14px;
  margin-left: 8px;
  color: #ff8a3d;
  cursor: pointer;

  &:hover {
    text-decoration: underline;
  }
`;

/* ===============================
   소셜 로그인
================================ */
export const socialRow = css`
  margin-top: 22px;
  display: flex;
  justify-content: center;
  gap: 16px;
`;

export const socialBtn = css`
  width: 48px;
  height: 48px;
  border-radius: 50%;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  cursor: pointer;

  img {
    width: 100%;
    height: 100%;
  }
`;

/* =========================
   FindId / FindPw / Signup 공통
========================= */

export const title = css`
  font-size: 20px;
  font-weight: 700;
  text-align: center;
  margin-bottom: 20px;
`;

export const description = css`
  font-size: 14px;
  color: #666666;
  text-align: center;
  margin-bottom: 20px;
`;

/* =========================
   회원가입 Modal창
========================= */
export const formLabel = css`
  font-size: 20px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;
`;

export const required = css`
  color: #ff3b30;
  font-size: 20px;
  line-height: 1;
`;

export const inputWithButton = css`
  position: relative;
  width: 100%;
`;

export const input = css`
  width: 100%;
  height: 44px;
  padding: 0 92px 0 12px;
  border-radius: 8px;
  border: 1px solid #dddddd;
  font-size: 14px;

  &:focus {
    outline: none;
    border-color: #ff8a3d;
  }
`;

export const dupCheckBtn = css`
  position: absolute;
  top: 50%;
  right: 8px;
  transform: translateY(-50%);

  height: 28px;
  padding: 0 10px;
  border-radius: 8px;
  border: none;
  background-color: #ff8a3d;
  color: #fff;
  font-size: 12px;
  cursor: pointer;

  &:disabled {
    background-color: #ddd;
    cursor: not-allowed;
  }
`;

/* 가로 정렬 row */
export const row = css`
  display: flex;
  gap: 12px;
`;

/* 각 필드 */
export const field = css`
  flex: 1;
  display: flex;
  flex-direction: column;
`;

/* select는 input이랑 높이 동일해야 함 */
export const select = css`
  height: 53px;
  padding: 0 12px;
  border-radius: 8px;
  border: 1px solid #dddddd;
  font-size: 14px;
  background-color: #fff;

  &:focus {
    outline: none;
    border-color: #ff8a3d;
  }
`;

export const uploadBox = css`
  width: 100%;
  height: 360px;
  border: 1px solid #000000;
  border-radius: 12px;
  cursor: pointer;
  background-color: #fff;

  display: flex;
  align-items: center;
  justify-content: center;

  &:hover {
    background-color: #fff6ef;
  }
`;

export const hiddenFileInput = css`
  display: none;
`;

export const uploadContent = css`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
`;

export const uploadIcon = css`
  font-size: 28px;
`;

export const uploadText = css`
  font-size: 14px;
  color: #333;
`;

export const uploadSubText = css`
  font-size: 12px;
  color: #999;
`;

export const uploadBtn = css`
  margin-top: 8px;
  padding: 6px 14px;
  background-color: #ffe2cc;
  border-radius: 6px;
  font-size: 12px;
  color: #ff8a3d;
`;

export const loginRow = css`
  display: flex;
  justify-content: center;
  margin-top: 14px;
`;

export const loginMent = css`
  font-size: 14px;
`;

export const loginLink = css`
  font-size: 14px;
  margin-left: 8px;
  color: #ff8a3d;
  cursor: pointer;

  &:hover {
    text-decoration: underline;
  }
`;

export const helperText = css`
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.4;
  user-select: none;
`;

export const errorText = css`
  ${helperText};
  color: #e53935;
`;

export const successText = css`
  ${helperText};
  color: #2e7d32;
`;
