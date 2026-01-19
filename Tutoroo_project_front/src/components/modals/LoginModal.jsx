/** @jsxImportSource @emotion/react */
import * as s from "./styles";
import useModalStore from "../../stores/modalStore";
import useAuthStore from "../../stores/useAuthStore";
import Swal from "sweetalert2";
import { useState } from "react";
import { authApi } from "../../apis/users/usersApi";

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

  // 입력값 상태 추가
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  // 로그인 폼 제출 시 실행
  const handleSubmit = async (e) => {
    // 페이지 새로고침 방지
    e.preventDefault();

    //  입력 검증
    if (!username || !password) {
      Swal.fire({
        icon: "warning",
        title: "입력 오류",
        text: "아이디와 비밀번호를 입력해주세요.",
        confirmButtonColor: "#FF8A3D",
      });
      return;
    }

    // 실제 로그인 API 연동
    try {
      setIsSubmitting(true);

      const data = await authApi.login({ username, password });
      // data: { accessToken, refreshToken, username, name, role, isNewUser }
      login(data);

      // 로그인 성공하면 모달만 닫기
      closeLogin();
    } catch (err) {
      const status = err?.response?.status;
      const msg =
        status === 401
          ? "아이디 또는 비밀번호가 올바르지 않습니다."
          : "로그인 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";

      Swal.fire({
        icon: "error",
        title: "로그인 실패",
        text: msg,
        confirmButtonColor: "#FF8A3D",
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div css={s.overlay}>
      {/* 모달 내부 클릭했을때 overlay 클릭 이벤트 차단*/}
      <div css={s.modal} onClick={(e) => e.stopPropagation()}>
        {/* 로고 */}
        <div css={s.logo}>
          <img src={logoImg} alt="Tutoroo" />
        </div>

        {/* 로그인 폼 */}
        <form onSubmit={handleSubmit} css={s.form}>
          <input
            css={s.input}
            type="text"
            placeholder="ID"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoComplete="username"
          />{" "}
          {/* 아이디 입력 창 */}
          <input
            css={s.input}
            type="password"
            placeholder="PASSWORD"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
          />{" "}
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
          <button type="submit" css={s.submitBtn} disabled={isSubmitting}>
            {isSubmitting ? "로그인 중..." : "로그인"}
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
          <button css={[s.socialBtn]} type="button">
            <img src={naverIcon} css={s.naver} />
          </button>

          <button css={[s.socialBtn]} type="button">
            <img src={googleIcon} css={s.google} />
          </button>

          <button css={[s.socialBtn]} type="button">
            <img src={kakaoIcon} css={s.kakao} />
          </button>
        </div>
      </div>
    </div>
  );
}

export default LoginModal;
