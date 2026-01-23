import { css } from "@emotion/react";
import { AiOutlineBgColors } from "react-icons/ai";


export const container = css`
    display: flex;
    flex-direction: column;
    width: 100%;
    height: calc(100vh - 72px);
    background-color: #fff;
    padding: 50px 250px;
    box-sizing: border-box;
    position: relative;
`;

export const pageTitle = css`
    font-size: 55px;
    font-weight: 600;
    color: #333;
    padding-left: 200px;


`

export const cardContainer = css`
    display: flex;
    width: 100%;
    justify-content: center;
    gap: 50px;
    margin-top: 60px;

`;

export const card = css`
    position: relative;
    width: 300px;
    height: 420px;
    border: 1px solid #eee;
    box-shadow: 0 4px 15px rgba(0,0,0,0.2);
    background-color: #fff;
    border-radius: 10px;
    overflow: hidden;
    transition: transform 0.2s;
    cursor: pointer;

    &:hover {
        transform: translateY(-5px);
    }
`;

export const disabledCard = css`
    opacity: 0.5;
    border: 1px solid #ddd;
    background-color: #f9f9f9;
    box-shadow:  none;
    cursor: not-allowed;

    &:hover {
        transform: none;
    }

`;

export const disabledOverlay = css`
    position: absolute;
    top: 10px;
    right: 10px;
    display: flex;
    align-items: center;
    gap: 5px;
    color: #999;
    font-size: 14px;
    font-weight: 600;
    z-index: 10;
`;


export const selectedCard = css`
    border: 2px solid #FF8A3D;
    box-shadow: 0 8px 20px rgba(255, 138, 61, 0.2);
`;

export const currentPlanbtn = css`
    width: 100%;
    height: 45px;
    border-radius: 10px;
    font-size: 14px;
    font-weight: 500;
    background-color: #333;
    color: #fff;
    border: none;
    cursor: default;

`;


export const checkBadge = css`
    position: absolute;
    top: -5px;
    right: 0px;
    font-size: 28px;
    
    border-radius: 50%;
    z-index: 10;
`;

export const cardHeader = (bgColor) => css`
    position: relative;
    display: flex;
    flex-direction: column;
    width: 100%;
    height: 100px;
    border-bottom: 1px solid #e9e9e9;
    align-items: center;
    justify-content: center;
    background-color: ${bgColor};
    
`;

export const freeBadge = css`
    position: absolute;
    top: 10px;
    font-size: 16px;
    color: #555;
    padding: 4px 12px;
    border-radius: 10px;
    margin-bottom: 5px;
    background-color: #ffc29a;
`;

export const planTitle = css`
    font-size: 24px;
    font-weight: 600;
    color: #333;
    margin: 0;
    margin-top: 10px ;
`;

export const cardBody = css`
    display: flex;
    flex-direction: column;
    height: calc(100% - 100px);
    padding: 20px 25px;
`;

export const priceSection = css`
    text-align: center;
    margin-bottom: 10px;
`;

export const priceText = css`
    font-size: 22px;
    font-weight: 500;
    color: #333;
    
`;

export const divider = css`
    width: 150px;
    height: 2px;
    background-color: #ddd;
    margin: 0 auto;
`;

export const featureList = css`
    flex: 1;
    list-style: none;
    padding: 0;
    margin: 0 20px;
    

    li {
        display: flex;
        align-items: center;
        font-size: 16px;
        color: #555;
        justify-content: center;
        align-items: center;
    }
`;

export const selectedBtn = css`
    width: 100%;
    height: 20px;
    border-radius: 10px;
    font-size: 14px;
    font-weight: 500;
    
`;

export const footSection = css`
    display: flex;
    justify-content: flex-end;
    margin-top: 50px;
    padding-right: 20px;
`;

export const paymentBtn = css`
    background-color: #FF8A3D;
    color: #ffffff;
    border: none;
    border-radius: 10px;
    padding: 10px 12px;
    font-size: 16px;
    font-weight: 500;
    cursor: pointer;
    box-shadow: 0 4px 10px rgba(0,0,0,0.2);

    &:hover {
        background-color: #e67e22;
    }

`;
// payment
// =========================================================================
// paymentpage
export const paymentContainer = css`
    display: flex;
    flex-direction: column;
    width: 100%;
    height: calc(100vh - 72px);
    background-color: #fff;
    padding: 50px 250px;
    box-sizing: border-box;
    position: relative;
`;

export const paymentPageTitle = css`
    font-size: 55px;
    font-weight: 600;
    color: #333;
    padding-left: 100px;


`

export const methodButtons = css`
    
    display: flex;
    gap: 15px;
    margin-top: 150px;
    margin-bottom: 30px;
    padding: 40px 30px 10px;
    
`;

const baseBtn = css`
    display: flex;
    flex: 1;

    height: 60px;
    border-radius: 10px;
    font-size: 20px;
    font-weight: 600;
    align-items: center;
    justify-content: center;
    border: 1px solid #dbdbdb;
    cursor: pointer;
    transition: all 0.2s;
    opacity: 0.6;

    &:hover {
        opacity: 0.8;
    }

`;

export const activeMethod = css`
    opacity: 1 !important;
    border: 2px solid #333;
    transform: translateY(-2px);
    box-shadow: 0 4px 10px rgba(0,0,0,0.2);
`;

export const kakaoBtn = css`
    ${baseBtn}
    background-color: #FFEB00;
    color: #3A1D1E;
`;

export const tossBtn = css`
    ${baseBtn}
    background-color: #fff ;
    color: white;
`;

export const cardBtn = css`
    ${baseBtn}
    background-color: #F2F2F2;
    color: #333;
    border: 1px solid #ddd;
`;

export const detailBox = css`
    display: flex;
    width: 100%;
    height: 200px;
    border: 1px solid #ddd;
    border-radius: 10px;
    padding: 40px;
    margin-bottom: 30px;
    justify-content: space-around;
    background-color: #fff;

`;

export const methodInfo = css`
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: flex-start;
    padding-top: 10px;

`;


export const methodTitle = (method) => css`
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 28px;
    font-weight: 700;
    margin: 0 0 20px 10px;
    
    color: ${method === "KAKAO" ? '#3A1D1E' : method === "TOSS" ? '#0064FF' : '#333'};
`;   

export const methodDesc = css`
    display: flex;
    font-size: 16px;
    color: #666;
    align-items: center;

`;

export const paymentDivider = css`
    width: 300px;
    height: 2px;
    background-color: #ddd;
    margin: 0 auto;
`;

export const priceInfo = css`
    display: flex;
    flex-direction: column;
    width: 300px;
    padding-left: 30px;
    border-left: 1px solid #eee;
    justify-content: center;
`;

export const priceRow = css`
    display: flex;
    justify-content: space-between;
    margin-bottom: 10px;
    font-size: 16px;
    color: #555;

    strong {
        color: #333;
        font-weight: 500;
    }
`;

export const totalRow = css`
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 10px;
`;

export const footer = css`
    display: flex;
    justify-content: space-between;
`;

export const backBtn = css`
    display: flex;
    background-color: #ffffff;
    border-radius: 10px;
    font-size: 14px;
    font-weight: 400;
    padding: 5px 20px;
    cursor: pointer;
    align-items: center;
    transition: 0.2s;

    &:hover {
        background-color: #dbdbdb;
    }
`;

export const payBtn = css`
    display: flex;
    font-size: 20px;
    font-weight: 400;
    border-radius: 10px;
    align-items: center;
    padding: 10px 20px;
    cursor: pointer;
    background-color: #FF8A3D;
    transition: 0.2s;

    &:hover {
        background-color: #e67e22;
    }
`



