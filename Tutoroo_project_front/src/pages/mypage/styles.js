import { css } from "@emotion/react";

  /* 
    인증 페이지 (verify)
   ----------------------------------------------------------- */
export const authWrapper = css`
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: calc(100vh - 72px); 
    background-color: #ffffff;
`;

export const authCard = css`
    width: 550px;
    height: 450px;
    border: 1px solid #dbdbdb;
    border-radius: 30px;
    background-color: #fff;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
    text-align: center;
`;

export const titleWrapper = css`
    display: flex;
    align-items: center;
    gap: 20px;
    margin-bottom: 24px;

    img {
        margin-left: 50px;
        width: 120px; 
        height: auto;
    }
`;

export const authTitle = css`
    font-size: 48px;
    font-weight: 500;
    margin-bottom: 24px;
    padding-top: 10px;
`;

export const authDesc = css`
    font-size: 30px;
    color: #666;
    line-height: 1.6;
    margin-bottom: 32px;
`;

export const inputGroup = css`
    text-align: center;
    margin-bottom: 24px;

    label {
        display: block;
        font-size: 24px;
        color: #666;
        margin-bottom: 8px;
    }

    input {
        width: 420px;
        height: 50px;
        padding: 0 20px;
        font-size: 16px;
        border-radius: 10px;
        box-sizing: border-box;
        
        &::placeholder {
            color: #666;
        }
    }
`;

export const submitBtn = css`
    width: 420px;
    height: 50px;
    border: none;
    border-radius: 10px;
    background-color: #FF8A3D;
    font-weight: 600;
    cursor: pointer;
`;

/*
   마이페이지 공통 레이아웃 (Layout, Card, Input, Button)
   ----------------------------------------------------------- */
export const wrapper = css`
    display: flex;
    width: 100%;
    min-height: calc(100vh - 72px);
    height: auto;
    background-color: #ffffff;
    padding-top: 60px;
    /* padding-bottom: 100px; */
    
    box-sizing: border-box;
    justify-content: center;
`;

export const mainContainer = css`
    display: flex;
    width: 550px; 
    height: 100%;
    justify-content: center;
    align-items: flex-start;
    padding-top: 20px;
`;

export const centerMain = css`
    align-items: center;
`


export const commonCard = css`
    display: flex;
    width: 550px; 
    height: calc(100% - 20px);
    flex-direction: column;
    align-items: center;
    background-color: #fff;
    border: 1px solid #dbdbdb;
    border-radius: 30px;
    margin-bottom: 20px;
    box-sizing: border-box;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
`;


export const cardTitle = css`
    flex-shrink: 0;
    width: 100%;
    border-bottom: 1px solid #eee;
    text-align: center;
    font-size: 40px; 
    font-weight: 600;
    color: #333;
    padding: 20px 0; 
    margin: 0;
`;


export const cardContent = css`
    width: 100%;
    padding: 30px 40px 0;
    box-sizing: border-box;
      
    &::-webkit-scrollbar {
        width: 8px;
    }
    &::-webkit-scrollbar-thumb {
        background-color: #ccc;
        border-radius: 4px;
    }
`;

export const actionBtn = css`
    width: 90%;
    height: 55px;
    margin: 20px auto;
    border: none;
    border-radius: 10px;
    background-color: #FF8A3D;
    color: white;
    font-size: 20px;
    font-weight: 600;
    cursor: pointer;
    flex-shrink: 0;
    transition: 0.2s;

    &:hover {
        background-color: #e67e22;
    }
`;


export const commonInputGroup = css`
    display: flex;
    flex-direction: column;
    width: 100%;
    margin-bottom: 20px;

    label {
        font-size: 16px;
        font-weight: 600;
        color: #333;
        margin-left: 5px;
        margin-bottom: 8px;
        text-align: left;
    }

    input {
        width: 100%;
        height: 50px;
        padding: 0 15px;
        border: 1px solid #dbdbdb;
        border-radius: 10px;
        font-size: 16px;
        outline: none;
        box-sizing: border-box;

        &:focus {
            border-color: #FF8A3D;
        }
    }
`;

/* -----------------------------------------------------------
   3. 페이지별 내부 요소 스타일 (ChangeInfo, Withdrawal 등)
   ----------------------------------------------------------- */

 //프로필 이미지 
export const profileSection = css`
    display: flex;
    flex-direction: column;
    align-items: center;
    margin-bottom: 30px;
`;

export const imageUploadBox = css`
    display: flex;
    width: 100%;
    height: 250px; 
    border: 2px dashed #dbdbdb;
    border-radius: 10px;
    justify-content: center;
    align-items: center;
    cursor: pointer;
    background-color: #fafafa;
    margin-top: 15px;
    overflow: hidden;
`;

export const uploadPlaceholder = css`
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 10px;
    color: #999;

    p {
        font-size: 18px;
        font-weight: 500;
        margin: 0;
    }

    button {
        margin-top: 10px;
        padding: 8px 16px;
        background-color: #FFE0B7;
        border: none;
        border-radius: 10px;
        font-weight: 600;
        cursor: pointer;
        color: #333;
    }

    input {
        width: 100%;
        height: 50px;
        padding: 0 15px;
        border: 1px solid #dbdbdb;
        border-radius: 10px;
        font-size: 16px;
        outline: none;
        box-sizing: border-box;
    }
`;

/* [회원탈퇴] 경고 박스 */
export const warningBox = css`
    width: 100%;
    border: 1px solid #FFD0D0;
    border-radius: 10px;
    background-color: #FFF5F5;
    padding: 20px;
    box-sizing: border-box;
    margin-bottom: 25px;

    h3 {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 18px;
        color: #D32F2F;
        font-weight: 700;
        margin: 0 0 10px 0;
    }
    ul {
        margin: 0;
        padding-left: 20px;
        li {
            font-size: 14px;
            color: #D32F2F;
            margin-bottom: 4px;
            list-style-type: disc;
        }
    }
`;


export const agreementSection = css`
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 20px;
    width: 100%;

    input[type="checkbox"] {
        width: 18px;
        height: 18px;
        cursor: pointer;
        accent-color: #FF8A3D;
    }

    label {
        font-size: 15px;
        font-weight: 600;
        color: #333;
        cursor: pointer;
    }
`;


export const reasonBox = css`
    width: 100%;
    margin-bottom: 20px;
    
   

    label {
        display: block;
        font-size: 16px;
        font-weight: 600;
        color: #333;
        margin-bottom: 8px;
        margin-left: 5px; /* 라벨 정렬 맞춤 */
    }

    textarea {
        width: 100%;
        height: 150px;
        padding: 15px;
        border: 1px solid #dbdbdb;
        border-radius: 10px;
        font-size: 14px;
        resize: none;
        box-sizing: border-box;
        outline: none;
        font-family: inherit;

        &::placeholder {
            color: #aaa;
        }
        &:focus {
            border-color: #FF8A3D;
        }
    }
`;