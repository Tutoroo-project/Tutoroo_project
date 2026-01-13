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
                    <input type="text" placeholder="이메일" />
                    <input type="password" placeholder="비밀번호" />

                    <button type="submit" css={s.loginBtn}>
                        로그인
                    </button>
                </form>

                {/* 하단 링크 */}
                <div css={s.links}>
                    <span>아이디 / 비밀번호 찾기</span>
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
