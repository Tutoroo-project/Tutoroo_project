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
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
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

// passwordVerify 부분
// -------------------------------------------------------------------
// changeInfo 부분

export const infoPageWrapper = css`
    display: flex;
    width: 100%;
    height: calc(100vh - 72px); /* 헤더 제외 높이 고정 */
    background-color: #ffffff;
    overflow: hidden; /* ★ 전체 화면 스크롤 막기 */
    box-sizing: border-box;
    justify-content: center;
    padding-top: 50px;
`;

// [회원정보 변경 전용] 오른쪽 메인 영역 (여기만 스크롤 됨)
export const infoPageMainContainer = css`
    
    height: 100%; 
    display: flex;
    justify-content: center;
    align-items: center; /* 위쪽부터 내용 쌓기 */
    padding-top: 50px;
    padding-bottom: 100px; /* 스크롤 끝부분 여유 공간 */
    overflow-y: auto; /* ★ 내용이 길어지면 여기서만 스크롤 발생 */
`;


export const wrapper = css`
    display: flex;
    width: 100%;
    height: calc(100vh - 72px);
    background-color: #ffffff;
    padding-top: 50px;
    box-sizing: border-box;
    justify-content: center;
    

`;

export const mainContainer = css`
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100%;
    max-width: 800px;
`;

export const scrollableCard = css`
    width: 550px;
    height: calc(100% - 50px);
    padding: 40px;
    background-color: #fff;
    border: 1px solid #dbdbdb;
    border-radius: 30px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
    overflow-y: auto;

    &::-webkit-scrollbar {
        width: 8px;
    }
    &::-webkit-scrollbar-thumb {
        background-color: #ccc;
        border-radius: 4px;
    }
    &::-webkit-scrollbar-track {
        background-color: transparent;
    }

`;

export const profileSection = css`
    margin-bottom: 20px;

`;

export const sectionTitle = css`
    display: flex;
    align-items: center;
    font-size: 24px;
    font-weight: 500;
    margin-bottom: 15px;
    
    .icon-person {
        width: 50px;
        height: 50px;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 10px;
        font-size: 20px;
    
    }

`;

export const imageUploadBox = css`
    display: flex;
    width: 100%;
    height: 300px;
    border: 2px solid #dbdbdb;
    border-radius: 10px;
    justify-content: center;
    align-items: center;
    background-color: #fff;

`;

export const uploadPlaceholder = css`
    display: flex;
    text-align: center;
    flex-direction: column;
    align-items: center;
    gap: 10px;

    p {
        font-size: 22px;
        font-weight: 500;
        margin: 0;
    }

    span {
        font-size: 18px;
        color: #999;
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
    
`;

export const formSection = css`
    display: flex;
    flex-direction: column;
    gap: 5px;
    margin-bottom: 20px;
    width: 100%;
`;
export const infoInputGroup = css`
    width: 100%;
    display: flex;
    flex-direction: column;
    
    
    

    label {
        display: block;
        font-size: 20px;
        color: #333;
        margin-bottom: 10px;
        font-weight: 400;
        text-align: left;
    }

    input {
        width: 100%;
        height: 50px;
        margin-bottom: 3px;
        padding: 0 15px;
        border: 1px solid #ccc;
        border-radius: 10px;
        font-size: 22px;
        outline: none;
        box-sizing: border-box;

        &:focus {
            border-color: #222222;
        }
    }
`;

export const subBtn = css`
    
    display: flex;
    justify-content: center;
    align-items: center;
    width: 100%;
    height: 50px;
    border: none;
    border-radius: 10px;
    background-color:#FF8A3D ;
    font-size: 28px;
    font-weight: 500;
    cursor: pointer;
    transition: 0.2s;

    &:hover {
        background-color: #e67e22;
    }


`;

// changeInfo
// ----------------------------------------------------------------------------
// changepassword

export const passwordCard = css`
    display: flex;
    flex-direction: column;
    align-items: center;
    width: 550px;
    height: auto;
    min-height: 400px;
    padding: 60px 40px;
    border-radius: 30px;

    background-color: #fff;
    border: 1px solid #dbdbdb;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);

`



export const pageTitle = css`
    font-size: 40px;
    font-weight: 600;
    color: #333;    
    text-align: center;
    margin-top: 20px;
    margin-bottom: 50px;
    
    
`;

export const passwordFormSection = css` //label + input 전체 레이아웃
    display: flex;
    flex-direction: column;
    align-items: center;
    width: 100%;
    margin-bottom: 50px; //버튼과의 간격
    gap: 20px; //입력창들과의 간격
    padding-bottom: 40px;
    border-bottom: 1px solid #e0e0e0;
    margin-bottom: 40px;
    
`;

export const passwordInputGroup = css` //label + input 하나씩 묶음
    width: 100%;
    display: flex;
    flex-direction: column;
    

    label {
        font-size: 24px;
        font-weight: 500;
        color: #333;
        margin-bottom: 10px;
        text-align: left;
    }

    input {
        width: 100%;
        height: 45px;
        padding: 0 15px;
        border: 1px solid #dbdbdb;
        border-radius: 10px;

        &::placeholder {
            color: #aaa;
            font-size: 15px;
        }

        &:focus {
            border-color: #FF8A3D; /* 포커스 시 주황색 */
        }

    }
    

`;
// changepassword
// ------------------------------------------------------------------------------------------------------
// withdrawal

export const withdrawalCard = css`
    display: flex;
    flex-direction: column;
    width: 550px;
    height: auto;
    /* min-height: 600px; */
    border: 1px solid #dbdbdb;
    border-radius: 30px;
    background-color: #fff;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
    align-items: center;
    
`;

export const withdrawalFormSection = css`
    display: flex;
    flex-direction: column;
    width: 100%;
    border-bottom: 1px solid #dbdbdb;
    margin-bottom: 40px;
    padding-bottom: 40px;
   
`;

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
        font-size: 20px;
        color: #D32F2F;
        font-weight: 600;
        margin: 0 0 10px 0;

        svg {
            font-size: 18px;
            color: #FBC02D; /* 노란색 아이콘 */
            background-color: #D32F2F; /* 배경을 채워주는 느낌이 필요하면 조정 */
            border-radius: 50%;
        }

    }

    ul {
        margin: 0;
        padding-left: 20px;
        
        li {
            font-size: 14px;
            color: #D32F2F;
            margin-bottom: 6px;
            line-height: 1.5;
            list-style-type: disc; /* 점 모양 */
        }
    }
`
