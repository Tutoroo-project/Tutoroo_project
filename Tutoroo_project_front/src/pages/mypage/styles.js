import { css } from "@emotion/react";

export const authWrapper = css`
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: calc(100vh - 72px); 
    background-color: #ffffff;
`

export const authCard = css`
    width: 550px;
    height: 450px;
    border: 1px solid #dbdbdb;
    border-radius: 30px;
    background-color: #fff;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    text-align: center;
    
`

export const titleWrapper = css`
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    margin-bottom: 24px;

    img {
        width: 35px; /* 로고 크기 조정 */
        height: auto;
    }
`

export const authTitle = css`
    font-size: 48px;
    font-weight: 500;
    margin-bottom: 24px;
    padding-top: 10px;

`

export const authDesc = css`
    font-size: 30px;
    color: #666;
    line-height: 1.6;
    margin-bottom: 32px;
`

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
`

export const submitBtn = css`
    width: 420px;
    height: 50px;
    border: none;
    border-radius: 10px;
    background-color: #FF8A3D;
    font-weight: 600;
    cursor: pointer;
`;