/** @jsxImportSource @emotion/react */
import Swal from "sweetalert2";
import * as s from "./styles";
import useModalStore from "../../stores/modalStore";
import { authApi } from "../../apis/users/usersApi";
import { useState } from "react";

// 비밀번호 찾기 모달 컴포넌트
function FindPwModal() {
  // 모달 열기 닫기
  const closeFindPw = useModalStore((state) => state.closeFindPw);
  const openLogin = useModalStore((state) => state.openLogin);

  // 상태 지정
  const [username, setUsername] = useState("");
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [email, setEmail] = useState("");

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [fieldErrors, setFieldErrors] = useState({});

  const clearError = (key) => {
    setFieldErrors((prev) => {
      if (!prev[key]) return prev;
      const next = { ...prev };
      delete next[key];
      return next;
    });
  };

  const handleSubmit = async (e) => {
    // 페이지 새로고침 방지
    e.preventDefault();
    if (isSubmitting) return;

    const nextErrors = {};
    if (!username.trim()) nextErrors.username = true;
    if (!name.trim()) nextErrors.name = true;
    if (!phone.trim()) nextErrors.phone = true;
    if (!email.trim()) nextErrors.email = true;

    if (Object.keys(nextErrors).length > 0) {
      setFieldErrors(nextErrors);

      Swal.fire({
        icon: "warning",
        title: "입력 오류",
        text: "빨간색 표시된 항목을 확인해주세요.",
        confirmButtonColor: "#FF8A3D",
        showClass: {
          popup: `animate__animated animate__shakeX animate__faster`,
        },
      });
      return;
    }

    setIsSubmitting(true);

    try {
      // 백엔드는 username + email만 사용
      const msg = await authApi.findPassword({
        username: username.trim(),
        email: email.trim(),
      });

      await Swal.fire({
        icon: "success",
        title: "비밀번호 찾기 완료 🎉",
        html: `
          <div style="font-size:14px; margin-bottom:6px;">
            ${msg ?? "가입된 이메일로 임시 비밀번호를 발송했습니다."}
          </div>
        `,
        confirmButtonText: "로그인 하러가기",
        confirmButtonColor: "#FF8A3D",
        showClass: {
          popup: `animate__animated animate__fadeInUp animate__faster`,
        },
        hideClass: {
          popup: `animate__animated animate__fadeOutDown animate__faster`,
        },
      });

      closeFindPw();
      openLogin();
    } catch (err) {
      const status = err?.response?.status;
      const serverMsg = err?.response?.data?.message;

      let msg =
        serverMsg ?? "비밀번호 찾기에 실패했습니다. 잠시 후 다시 시도해주세요.";
      if (status === 404) msg = "일치하는 회원 정보를 찾을 수 없습니다.";
      if (status === 400) msg = "입력값을 다시 확인해주세요.";
      if (status === 500) msg = "서버 오류가 발생했습니다.";

      Swal.fire({
        icon: "error",
        title: "비밀번호 찾기 실패",
        text: msg,
        confirmButtonColor: "#FF8A3D",
        showClass: {
          popup: `animate__animated animate__shakeX animate__faster`,
        },
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div css={s.overlay}>
      {/* 모달 내부 클릭했을때 overlay 클릭 이벤트 차단*/}
      <div css={s.modal} onClick={(e) => e.stopPropagation()}>
        {/* 모달 타이틀 */}
        <div css={s.title}>비밀번호 찾기</div>

        <form css={s.form} onSubmit={handleSubmit}>
          {/* 아이디 */}
          <label css={s.formLabel}>
            <span css={s.required}>*</span>
            아이디
          </label>
          <input
            css={s.input}
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="가입 시 사용한 아이디"
          />

          {/* 이름 */}
          <label css={s.formLabel}>
            <span css={s.required}>*</span>
            이름
          </label>
          <input
            css={s.input}
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="이름"
          />

          {/* 전화번호 */}
          <label css={s.formLabel}>
            <span css={s.required}>*</span>
            전화번호
          </label>
          <input
            css={s.input}
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            placeholder="010-0000-0000"
          />

          {/* 이메일 */}
          <label css={s.formLabel}>
            <span css={s.required}>*</span>
            이메일
          </label>
          <input
            css={s.input}
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="가입 시 사용한 이메일"
          />

          {/* 비밀번호 찾기 버튼 */}
          <button css={s.submitBtn} type="submit">
            비밀번호 찾기
          </button>
        </form>

        {/* 로그인 모달로 가기*/}
        <div css={s.loginRow}>
          <span css={s.loginMent}>로그인 화면으로 돌아가기</span>
          <span css={s.loginLink} onClick={openLogin}>
            로그인
          </span>
        </div>
      </div>
    </div>
  );
}

export default FindPwModal;
