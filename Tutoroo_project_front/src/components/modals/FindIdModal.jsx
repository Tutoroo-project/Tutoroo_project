/** @jsxImportSource @emotion/react */
import Swal from "sweetalert2";
import * as s from "./styles";
import useModalStore from "../../stores/modalStore";
import { authApi } from "../../apis/users/usersApi";
import { useState } from "react";

// 아이디 찾기 모달 컴포넌트
function FindIdModal() {
  // 모달 열기 닫기
  const closeFindId = useModalStore((state) => state.closeFindId);
  const openLogin = useModalStore((state) => state.openLogin);

  // 상태 지정
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [email, setEmail] = useState("");

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!name || !phone || !email) {
      Swal.fire({
        icon: "warning",
        title: "입력 오류",
        text: "모든 항목을 입력해주세요.",
        confirmButtonColor: "#FF8A3D",
        showClass: {
          popup: `
          animate__animated
          animate__shakeX
          animate__faster
        `,
        },
      });
      return;
    }

    try {
      const data = await authApi.findId({
        name: name.trim(),
        phone: phone.trim(),
        email: email.trim(),
      });

      const foundId = data?.result;
      const msg = data?.message ?? "아이디 찾기 완료";

      Swal.fire({
        icon: "success",
        title: "아이디 찾기 완료 🎉",
        html: `
        <div style="font-size:14px; margin-bottom:6px;">
          ${msg}
        </div>
        ${foundId ? `<strong style="font-size:18px;">${foundId}</strong>` : ""}
      `,
        confirmButtonText: "로그인 하러가기",
        confirmButtonColor: "#FF8A3D",
        showClass: {
          popup: `animate__animated animate__fadeInUp animate__faster`,
        },
        hideClass: {
          popup: `animate__animated animate__fadeOutDown animate__faster`,
        },
      }).then(() => {
        closeFindId();
        openLogin();
      });
    } catch (err) {
      const status = err?.response?.status;

      let msg = "아이디 찾기에 실패했습니다. 잠시 후 다시 시도해주세요.";
      if (status === 404) msg = "일치하는 회원 정보를 찾을 수 없습니다.";
      if (status === 400) msg = "입력값을 다시 확인해주세요.";
      if (status === 500) msg = "서버 오류가 발생했습니다.";

      Swal.fire({
        icon: "error",
        title: "아이디 찾기 실패",
        text: msg,
        confirmButtonColor: "#FF8A3D",
        showClass: {
          popup: `
          animate__animated
          animate__shakeX
          animate__faster
        `,
        },
      });
    }
  };

  return (
    <div css={s.overlay}>
      {/* 모달 내부 클릭했을때 overlay 클릭 이벤트 차단*/}
      <div css={s.modal} onClick={(e) => e.stopPropagation()}>
        {/* 모달 타이틀 */}
        <div css={s.title}>아이디 찾기</div>

        <form css={s.form} onSubmit={handleSubmit}>
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

          {/* 아이디 찾기 버튼 */}
          <button css={s.submitBtn} type="submit">
            아이디 찾기
          </button>
        </form>

        {/* 로그인 모달로 이동 */}
        <div css={s.loginRow}>
          <span css={s.loginMent}>로그인 화면으로 돌아가기</span>
          <span
            css={s.loginLink}
            onClick={() => {
              closeFindId();
              openLogin();
            }}
          >
            로그인
          </span>
        </div>
      </div>
    </div>
  );
}

export default FindIdModal;
