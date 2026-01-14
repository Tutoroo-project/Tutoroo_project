/** @jsxImportSource @emotion/react */
import { Link, useLocation } from "react-router-dom";
import * as s from "./SidebarStyle";


function Sidebar() {
    const location = useLocation();
    
    const menus = [
        {name: "회원 정보 변경", path: "/mypage/changeinfo"},
        {name: "비밀번호 변경", path: "/mypage/changepassword"},
        {name: "회원 탈퇴", path: "/mypage/withdrawl"}
    ];

    return(
        <aside css={s.sidebarContainer}>
            <div css={s.sidebarTitle}>회원 정보 수정</div>
            <ul css={s.menuList}>
                {
                    menus.map((menu, index) => {
                        const isActive = location.pathname === menu.path;
                        return(
                            <li key={index} css={s.menuItem}>
                                <Link to={menu.path}
                                    css={[s.menuLink, isActive && s.activeLink]}
                                >{menu.name}</Link>
                            </li>
                        )
                    })
                }
            </ul>
        </aside>
    );
}

export default Sidebar;