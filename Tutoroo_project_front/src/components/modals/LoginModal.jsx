/** @jsxImportSource @emotion/react */
import * as s from "./styles";
import useModalStore from "../../stores/modalStore";
import useAuthStore from "../../stores/useAuthStore";

// 이미지 import
import logoImg from "../../assets/images/mascots/logo.png";
import googleIcon from "../../assets/icons/socials/Google_icons.png";
import naverIcon from "../../assets/icons/socials/Naver_icons.png";
import kakaoIcon from "../../assets/icons/socials/Kakaotalk_icons.png";

// 로그인 모달 컴포넌트
// Zustand(modalStore)를 통해 모달 열림/닫힘 제어
// Zustand(authStore)를 통해 로그인 상태 관리

function LoginModal() {
  // 모달 열기 / 닫기
  const closeLogin = useModalStore((state) => state.closeLogin);
  const openFindId = useModalStore((state) => state.openFindId);
  const openFindPw = useModalStore((state) => state.openFindPw);
  const openSignUp = useModalStore((state) => state.openSignUp);

  // 로그인 성공 시 사용자 정보를 전역 상태에 저장
  const login = useAuthStore((state) => state.login);

  // 로그인 폼 제출 시 실행
  const handleSubmit = (e) => {
    // 페이지 새로고침 방지
    e.preventDefault();

    // 임시 로그인 백엔드 붙으면 교체 하면 될듯
    login({ id: 1, name: "OOO" });

    // 로그인 성공하면 모달만 닫기
    closeLogin();
  };

  return (
    // 배경 클릭하면 모달 닫힘 (나중에 빼기)
    <div css={s.overlay} onClick={closeLogin}>
      {/* 모달 내부 클릭했을때 overlay 클릭 이벤트 차단*/}
      <div css={s.modal} onClick={(e) => e.stopPropagation()}>
        {/* 로고 */}
        <div css={s.logo}>
          <img src={logoImg} alt="Tutoroo" />
        </div>

        {/* 로그인 폼 */}
        <form onSubmit={handleSubmit} css={s.form}>
          <input css={s.input} type="text" placeholder="ID" />{" "}
          {/* 아이디 입력 창 */}
          <input css={s.input} type="password" placeholder="PASSWORD" />{" "}
          {/* 패스워드 입력 창 */}
          {/* 로그인 옵션 영역 */}
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
          {/* 로그인 버튼 */}
          <button type="submit" css={s.submitBtn}>
            로그인
          </button>
        </form>

        {/* 회원가입 링크*/}
        <div css={s.signupRow}>
          <div css={s.signupMent}>아직 계정이 없으신가요? </div>
          <span css={s.signupLink} onClick={openSignUp}>
            회원가입
          </span>
        </div>

        {/* 소셜 로그인 API 연동 예정 (백엔드 붙으면)*/}
        <div css={s.socialRow}>
          <button css={[s.socialBtn]}>
            <img src={naverIcon} css={s.naver} />
          </button>

          <button css={[s.socialBtn]}>
            <img src={googleIcon} css={s.google} />
          </button>

          <button css={[s.socialBtn]}>
            <img src={kakaoIcon} css={s.kakao} />
          </button>
        </div>
      </div>
    </div>
  );
}

export default LoginModal;
