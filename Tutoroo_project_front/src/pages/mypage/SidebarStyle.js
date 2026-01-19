import { css } from "@emotion/react";

export const sidebarContainer = css`
    position: sticky;
    top: 60px;
    width: 250px;
    height: fit-content;
    margin-right: 200px;
    flex-shrink: 0; 

   
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


export const menuLink = css`
    display: block;
    text-decoration: none;
    color: #555;
    padding: 18px 20px;
    font-size: 24px;
    transition: all 0.2s;
    border-left: 4px solid transparent; 

    &:hover {
        background-color: #f9f9f9;
        color: #333;
    }
`;


export const activeLink = css`
    background-color: #FFF8F0; 
    color: #333;
    font-weight: 600;
    border-left: 4px solid #FF8A3D; 
`;