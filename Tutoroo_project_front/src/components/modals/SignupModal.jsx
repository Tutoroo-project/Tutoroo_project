/** @jsxImportSource @emotion/react */
import * as s from "./styles";
import useModalStore from "../../stores/modalStore";
import useAuthStore from "../../stores/useAuthStore";

function SignUpModal() {
  const closeSignUp = useModalStore((state) => state.closeSignUp);
  const openLogin = useModalStore((state) => state.openLogin);
  const login = useAuthStore((state) => state.login);

  const handleSubmit = (e) => {
    e.preventDefault();

    // ⚠️ 임시 회원가입 → 성공했다고 치고 로그인 처리
    login({ id: 1, name: "OOO" });

    closeSignUp();
  };

  return (
    <div css={s.overlay} onClick={closeSignUp}>
      <div css={s.modal} onClick={(e) => e.stopPropagation()}>
        <div css={s.title}>회원가입</div>

        <form css={s.form} onSubmit={handleSubmit}>
          <label css={s.formLabel}>
            <span css={s.required}>*</span>
            아이디
          </label>
          <input placeholder="아이디를 입력하세요" />
          <input placeholder="비밀번호를 8자 이상 입력해주세요" type="password" />
          <input placeholder="비밀번호 확인" type="password" />
          <input placeholder="이름" />

          <button css={s.submitBtn} type="submit">
            가입하기
          </button>
        </form>

        <div css={s.footer}>
          <span>이미 계정이 있나요?</span>
          <span className="link" onClick={openLogin}>
            로그인
          </span>
        </div>
      </div>
    </div>
  );
}

export default SignUpModal;
