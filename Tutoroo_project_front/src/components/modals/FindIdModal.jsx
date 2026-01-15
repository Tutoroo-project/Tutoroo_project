/** @jsxImportSource @emotion/react */
import Swal from "sweetalert2";
import * as s from "./styles";
import useModalStore from "../../stores/modalStore";
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

  const handleSubmit = (e) => {
    // 페이지 새로고침 방지
    e.preventDefault();

    // 필수 입력값 누락 시 경고 알림
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

    // 임시로 일단 성공처리 백엔드 연동하고 API 호출로 변경할거임
    const foundId = "tutoroo_user01";

    Swal.fire({
      icon: "success",
      title: "아이디 찾기 완료 🎉",
      html: `
      <div style="font-size:14px; margin-bottom:6px;">
        회원님의 아이디는
      </div>
      <strong style="font-size:18px;">${foundId}</strong>
    `,
      confirmButtonText: "로그인 하러가기",
      confirmButtonColor: "#FF8A3D",
      showClass: {
        popup: `
        animate__animated
        animate__fadeInUp
        animate__faster
      `,
      },
      hideClass: {
        popup: `
        animate__animated
        animate__fadeOutDown
        animate__faster
      `,
      },
    }).then(() => {
      // Alert 확인 후 비밀번호 찾기 모달 닫고 로그인 모달 열거임
      closeFindId();
      openLogin();
    });
  };

  return (
    // 배경 클릭하면 비밀번호 찾기 모달 닫기
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
          <span css={s.loginLink} onClick={openLogin}>
            로그인
          </span>
        </div>
      </div>
    </div>
  );
}

export default FindIdModal;
