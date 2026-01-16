/** @jsxImportSource @emotion/react */
import * as s from "./styles";
import { useNavigate } from "react-router-dom";
import logoImg from "../../assets/images/mascots/logover2.png"; 

function Header() {
  const navigate = useNavigate(); 

  return (
    <header css={s.header}>
      <div css={s.inner}>
        <div 
          css={s.logoWrap} 
          onClick={() => navigate("/")}
        >
          <img css={s.logoImg} src={logoImg} alt="Tutoroo" />
        </div>
        <button css={s.profileBtn} onClick={() => navigate("/mypage/verify")}>프로필 ▼</button>
      </div>
    </header>
  );
}

export default Header;