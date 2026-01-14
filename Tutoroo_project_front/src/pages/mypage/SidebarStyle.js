import { css } from "@emotion/react";

export const sidebarContainer = css`
    
    width: 250px;
    margin-right: 50px;
    flex-shrink: 0; //화면 줄어도 사이드바 유지
   
`;

export const sidebarTitle = css`
    font-size: 32px;
    font-weight: 700;
    color: #333;
    padding-bottom: 20px;
    border-bottom: 2px solid #333;
    
    
`;

export const menuList = css`
    list-style: none;
    padding: 0;
    margin: 0;
`

export const menuItem = css`
    border-bottom: 1px solid #eee;
`;

// 기본 링크 스타일
export const menuLink = css`
    display: block;
    text-decoration: none;
    color: #555;
    padding: 18px 20px;
    font-size: 24px;
    transition: all 0.2s;
    border-left: 4px solid transparent; /* 활성화 시 border 생성을 위해 미리 공간 확보 */

    &:hover {
        background-color: #f9f9f9;
        color: #333;
    }
`;

// 활성화(클릭됨) 되었을 때 추가되는 스타일
export const activeLink = css`
    background-color: #FFF8F0; /* 연한 주황색 배경 */
    color: #333;
    font-weight: 600;
    border-left: 4px solid #FF8A3D; /* 진한 주황색 포인트 바 */
`;