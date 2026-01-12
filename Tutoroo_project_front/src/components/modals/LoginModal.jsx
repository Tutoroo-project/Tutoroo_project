/** @jsxImportSource @emotion/react */
import * as s from "./styles";
import useModalStore from "../../stores/modalStore";
import useAuthStore from "../../stores/useAuthStore";

import logoImg from "../../assets/images/mascots/logo.jpg";
import googleIcon from "../../assets/icons/socials/Google_icons.png";
import naverIcon from "../../assets/icons/socials/Naver_icons.png";
import kakaoIcon from "../../assets/icons/socials/Kakaotalk_icons.png";

function LoginModal() {
  const closeLogin = useModalStore((state) => state.closeLogin);
  const openFindId = useModalStore((state) => state.openFindId);
  const openFindPw = useModalStore((state) => state.openFindPw);
  const openSignUp = useModalStore((state) => state.openSignUp);
  const login = useAuthStore((state) => state.login);

  const handleSubmit = (e) => {
    e.preventDefault();

    // ⚠️ 임시 로그인 (백엔드 붙으면 교체)
    login({ id: 1, name: "OOO" });

    // ✅ 성공하면 모달만 닫기
    closeLogin();
  };

  return (
    <div css={s.overlay} onClick={closeLogin}>
      <div css={s.modal} onClick={(e) => e.stopPropagation()}>
        {/* 로고 */}
        <div css={s.logo}>
          <img src={logoImg} alt="Tutoroo" />
          <h2>Tutoroo</h2>
        </div>

        {/* 로그인 폼 */}
        <form onSubmit={handleSubmit} css={s.form}>
          <input type="text" placeholder="ID" />
          <input type="password" placeholder="PASSWORD" />

          <div css={s.optionRow}>
            {/* 로그인 상태 유지 */}
            <label css={s.keepLogin}>
              <input type="checkbox" />
              로그인 상태 유지
            </label>

            {/* 아이디 / 비밀번호 찾기 */}
            <div css={s.links}>
              <span css={s.findId} onClick={openFindId}>
                아이디 찾기
              </span>
              <span css={s.findPw} onClick={openFindPw}>
                비밀번호 찾기
              </span>
            </div>
          </div>

          <button type="submit" css={s.submitBtn}>
            로그인
          </button>
        </form>

        {/* 회원가입 */}
        <div css={s.signupRow}>
          <div css={s.signupMent}>아직 계정이 없으신가요? </div>
          <span css={s.signup} onClick={openSignUp}>
            회원가입
          </span>
        </div>

        {/* 소셜 로그인 */}
        <div css={s.socialRow}>
          <button css={[s.socialBtn, s.naver]}>
            <img src={naverIcon} />
          </button>

          <button css={[s.socialBtn, s.google]}>
            <img src={googleIcon} />
          </button>

          <button css={[s.socialBtn, s.kakao]}>
            <img src={kakaoIcon} />
          </button>
        </div>
      </div>
    </div>
  );
}

export default LoginModal;
