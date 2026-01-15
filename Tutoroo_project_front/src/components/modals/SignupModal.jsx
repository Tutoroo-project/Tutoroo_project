/** @jsxImportSource @emotion/react */
import * as s from "./styles";
import { FiCamera } from "react-icons/fi";
import useModalStore from "../../stores/modalStore";
import useAuthStore from "../../stores/useAuthStore";
import { useState } from "react";

function SignUpModal() {
  // ================== state ==================
  const [username, setUsername] = useState("");
  const [isChecking, setIsChecking] = useState(false);
  const [isDuplicated, setIsDuplicated] = useState(null);
  // null: 확인 안 함 | true: 중복 | false: 사용 가능

  const [isValidUsername, setIsValidUsername] = useState(null);
  // null | true | false (아이디 유효성 검사)


  const closeSignUp = useModalStore((state) => state.closeSignUp);
  const openLogin = useModalStore((state) => state.openLogin);
  const login = useAuthStore((state) => state.login);

  // ================== handlers ==================
  const handleCheckDuplicate = async () => {
    if (!username) return;

    setIsChecking(true);

    // ⚠️ 임시 중복확인 (백엔드 연동 시 API 호출로 교체)
    setTimeout(() => {
      if (username === "admin") {
        setIsDuplicated(true); // 중복
      } else {
        setIsDuplicated(false); // 사용 가능
      }
      setIsChecking(false);
    }, 500);
  };

  const handleSubmit = (e) => {
    e.preventDefault();

    // 중복확인 안 했거나, 중복이면 제출 차단
    if (isDuplicated !== false) return;

    // ⚠️ 임시 회원가입 성공 처리
    login({ id: 1, name: "OOO" });
    closeSignUp();
  };

  // ================== render ==================
  return (
    <div css={s.overlay} onClick={closeSignUp}>
      <div css={s.modal} onClick={(e) => e.stopPropagation()}>
        <div css={s.title}>회원가입</div>

        <form css={s.form} onSubmit={handleSubmit}>
          {/* 아이디 */}
          <label css={s.formLabel}>
            <span css={s.required}>*</span>
            아이디
          </label>

          <div css={s.inputWithButton}>
            <input
              css={s.input}
              value={username}
              onChange={(e) => {
                const value = e.target.value;
                setUsername(value);

                const usernameRegex = /^(?=.*[a-z])[a-z0-9_]{4,12}$/;

                if (!value) {
                  setIsValidUsername(null);
                  setIsDuplicated(null);
                } else if (!usernameRegex.test(value)) {
                  setIsValidUsername(false);
                  setIsDuplicated(null); // 형식 틀리면 중복 의미 없음
                } else {
                  setIsValidUsername(true);
                  setIsDuplicated(null); // 형식 바뀌면 다시 중복확인 필요
                }
              }}
              placeholder="아이디를 입력하세요"
            />

            <button
              type="button"
              css={s.dupCheckBtn}
              onClick={handleCheckDuplicate}
              disabled={
                isChecking ||
                isDuplicated === false ||
                isValidUsername !== true
              }
            >
              {isChecking
                ? "확인중..."
                : isDuplicated === false
                  ? "확인완료"
                  : "중복확인"}
            </button>

          </div>

          {/* 아이디 상태 메시지 */}
          {isValidUsername === false && (
            <p css={s.helperText}>
              아이디는 4~12자 영문 소문자, 숫자, _ 만 사용 가능합니다.
            </p>
          )}

          {isValidUsername === true && isDuplicated === true && (
            <p css={s.errorText}>이미 사용 중인 아이디입니다.</p>
          )}

          {isValidUsername === true && isDuplicated === false && (
            <p css={s.successText}>사용 가능한 아이디입니다.</p>
          )}

          {/* 비밀번호 */}
          <label css={s.formLabel}>
            <span css={s.required}>*</span>
            비밀번호
          </label>
          <input
            css={s.input}
            placeholder="비밀번호를 8자 이상 입력해주세요."
            type="password"
          />
          <label css={s.formLabel}>
            <span css={s.required}>*</span>
            비밀번호 확인
          </label>
          <input css={s.input}placeholder="비밀번호를 다시 입력해주세요." type="password" />

          {/* 이메일 */}
          <label css={s.formLabel}>
            <span css={s.required}>*</span>
            이메일
          </label>
          <input css={s.input} placeholder="email@email.com" />

          {/* 이름 */}
          <label css={s.formLabel}>
            <span css={s.required}>*</span>
            이름
          </label>
          <input css={s.input} placeholder="이름" />

          {/* 나이 + 성별 */}
          <div css={s.row}>
            <div css={s.field}>
              <label css={s.formLabel}>
                <span css={s.required}>*</span>
                나이
              </label>
              <input css={s.input} placeholder="나이" />
            </div>

            <div css={s.field}>
              <label css={s.formLabel}>
                <span css={s.required}>*</span>
                성별
              </label>
              <select css={s.select}>
                <option css={s.option} value="">성별 선택</option>
                <option css={s.option} value="M">남성</option>
                <option css={s.option} value="F">여성</option>
              </select>
            </div>
          </div>

          {/* 이름 */}
          <label css={s.formLabel}>
            <span css={s.required}>*</span>
            전화번호
          </label>
          <input css={s.input} placeholder="010-1234-5678" />

          {/* 프로필 이미지 업로드 */}
          <label css={s.uploadBox}>
            <input
              type="file"
              accept="image/*"
              css={s.hiddenFileInput}
              onChange={(e) => {
                const file = e.target.files[0];
                if (!file) return;

                if (file.size > 5 * 1024 * 1024) {
                  alert("5MB 이하의 이미지만 업로드 가능합니다.");
                  return;
                }

                console.log(file); // 👉 여기서 state / API 연동
              }}
            />

            <div css={s.uploadContent}>
              <div css={s.uploadIcon}>
                <FiCamera />
              </div>

              <p css={s.uploadText}>이미지를 드래그하거나 선택해서 업로드</p>

              <span css={s.uploadSubText}>최대 5MB 이하</span>

              <div css={s.uploadBtn}>파일 선택</div>
            </div>
          </label>

          {/* 가입 버튼 */}
          <button
            css={s.submitBtn}
            type="submit"
            disabled={isDuplicated !== false}
          >
            회원가입 완료
          </button>
        </form>

        <div css={s.loginRow}>
          <span css={s.loginMent}>이미 계정이 있나요?</span>
          <span css={s.loginLink} onClick={openLogin}>
            로그인
          </span>
        </div>
      </div>
    </div>
  );
}

export default SignUpModal;
