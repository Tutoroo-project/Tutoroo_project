/** @jsxImportSource @emotion/react */

import * as s from "./styles";
import logoImg from "../../assets/images/mascots/logover2.png"; // ✅ 네 경로 기준

function Header() {
  return (
    <header css={s.header}>
      <div css={s.inner}>
        <div css={s.logoWrap}>
          <img css={s.logoImg} src={logoImg} alt="Tutoroo" />
        </div>

        <button css={s.profileBtn}>내 정보 ▼</button>
      </div>
    </header>
  );
}

export default Header;
