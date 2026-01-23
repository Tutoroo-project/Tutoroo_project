import { css } from "@emotion/react";

export const wrapper = css`
    display: flex;
    width: 100%;
    height: calc(100vh - 60px);
    align-items: center;
    justify-content: center;
    
    
`;

export const contentBox = css`
    display: flex;
    flex-direction: row;
    align-items: flex-start;
    gap: 50px;
`;

export const mainContainer = css`
    width: 1200px;
    height: 80vh;
    background-color: #dbdbdb;
    border-radius: 20px;
`

export const btn = css`
    width: 75px;
    height: 25px;

    padding: 12px 20px;
    border: none;
    border-radius: 12px;
    background-color: #ffe6d5;
    color: #e67025;
    font-weight: 700;
    border: none;
    cursor: pointer;
    font-size: 16px;
    transition: background 0.2s;

    white-space: nowrap;

    &:hover {
        background-color: #e67025;
    }
`;