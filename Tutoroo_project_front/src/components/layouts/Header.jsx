/** @jsxImportSource @emotion/react */
import { useEffect, useRef, useState } from "react";
import * as s from "./styles";
import { useNavigate } from "react-router-dom";
import logoImg from "../../assets/images/mascots/logover2.png";
import useAuthStore from "../../stores/useAuthStore";

function Header() {
  const navigate = useNavigate();

  const [isOpen, setIsOpen] = useState(false);
  const wrapRef = useRef(null);

  const logout = useAuthStore((state) => state.logout);
  const isLoggedIn = useAuthStore((state) => state.isLoggedIn);

  useEffect(() => {
    const handleOutside = (e) => {
      if (!wrapRef.current) return;
      if (!wrapRef.current.contains(e.target)) setIsOpen(false);
    };

    const handleKey = (e) => {
      if (e.key === "Escape") setIsOpen(false);
    };

    document.addEventListener("mousedown", handleOutside);
    document.addEventListener("keydown", handleKey);
    return () => {
      document.removeEventListener("mousedown", handleOutside);
      document.removeEventListener("keydown", handleKey);
    };
  }, []);

  const go = (path) => {
    navigate(path);
    setIsOpen(false);
  };

  const handleLogout = () => {
    logout();
    setIsOpen(false);
    navigate("/"); // 원하면 "/signin" 같은 곳으로 변경
  };

  return (
    <header css={s.header}>
      <div css={s.inner}>
        <div css={s.logoWrap} onClick={() => navigate("/")}>
          <img css={s.logoImg} src={logoImg} alt="Tutoroo" />
        </div>

        <div css={s.profileWrap} ref={wrapRef}>
          <button
            type="button"
            css={[s.profileBtn, isOpen && s.profileBtnActive]}
            onClick={() => setIsOpen((prev) => !prev)}
            aria-haspopup="menu"
            aria-expanded={isOpen}
          >
            프로필 <span css={[s.caret, isOpen && s.caretOpen]}>▼</span>
          </button>

          {isOpen && (
            <div css={s.profileMenu} role="menu">
              <button type="button" css={s.menuItem} role="menuitem" onClick={() => go("/mypage/verify")}>
                마이페이지
              </button>
              <button type="button" css={s.menuItem} role="menuitem" onClick={() => go("/subscribe")}>
                구독/결제관리
              </button>
              <button type="button" css={s.menuItem} role="menuitem" onClick={() => go("/ranking")}>
                랭킹
              </button>

              {/* ✅ 로그인 상태일 때만 로그아웃 노출 */}
              {isLoggedIn && (
                <>
                  <div css={s.menuDivider} />
                  <button
                    type="button"
                    css={[s.menuItem, s.logoutItem]}
                    role="menuitem"
                    onClick={handleLogout}
                  >
                    로그아웃
                  </button>
                </>
              )}
            </div>
          )}
        </div>
      </div>
    </header>
  );
}

export default Header;
