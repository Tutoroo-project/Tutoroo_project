import { css } from "@emotion/react";

/* =========================
   공통 Overlay / Modal
========================= */

export const overlay = css`
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
`;

export const modal = css`
  width: 360px;
  background-color: #ffffff;
  border-radius: 16px;
  padding: 28px 24px 32px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
`;

/* =========================
   로고 영역
========================= */

export const logo = css`
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 24px;

  img {
    width: 48px;
    height: 48px;
    margin-bottom: 8px;
  }

  h2 {
    font-size: 24px;
    font-weight: 700;
    color: #ff8a3d;
  }
`;

/* =========================
   Form 공통
========================= */

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

/* =========================
   아이디 / 비밀번호 찾기
========================= */

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

export const submitBtn = css`
  margin-top: 4px; // 옵션과 너무 붙지 않게
  height: 44px;
  border-radius: 8px;
  background-color: #ff8a3d;
  color: white;
  font-size: 16px;
  font-weight: 600;

  &:hover {
    background-color: #ff7a1f;
  }
`;

export const signupRow = css`
  display: flex;
  justify-content: center;
  margin-top: 14px;
`;

export const signupMent = css`
  font-size: 14px;
`;

export const signup = css`
  font-size: 14px;
  margin-left: 8px;
  color: #ff8a3d;
  cursor: pointer;

  &:hover {
    text-decoration: underline;
  }
`;

/* =========================
   소셜 로그인
========================= */

export const socialRow = css`
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 22px;
`;

export const socialBtn = css`
  width: 48px;
  height: 48px;
  border-radius: 50%;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;

  img {
    width: 22px;
    height: 22px;
  }
`;

export const naver = css`
  background-color: #03c75a;
`;

export const google = css`
  background-color: #ffffff;
  border: 1px solid #dddddd;
`;

export const kakao = css`
  background-color: #fee500;
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
  gap: 4px;
`;

export const required = css`
  color: #ff3b30;
  font-size: 20px;
  line-height: 1;
`;
