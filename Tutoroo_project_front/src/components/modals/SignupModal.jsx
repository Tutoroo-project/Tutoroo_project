/** @jsxImportSource @emotion/react */
import * as s from "./styles";
import { FiCamera } from "react-icons/fi";
import useModalStore from "../../stores/modalStore";
import { BsPersonCircle } from "react-icons/bs";
import { useState } from "react";
import Swal from "sweetalert2";
import { authApi } from "../../apis/users/usersApi";

// 회원가입 모달 컴포넌트
function SignUpModal() {
  // 상태 지정해둔곳
  const [username, setUsername] = useState("");

  // 아이디 중복 확인하는 상태값
  const [isChecking, setIsChecking] = useState(false);
  const [isDuplicated, setIsDuplicated] = useState(null);
  // null: 확인 안 함 | true: 중복 | false: 사용 가능

  // 아이디 유효성 검사
  const [isValidUsername, setIsValidUsername] = useState(null);
  // null | true | false

  // 회원가입 입력값 상태들
  const [fieldErrors, setFieldErrors] = useState({});
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [email, setEmail] = useState("");
  const [name, setName] = useState("");
  const [age, setAge] = useState(""); // input은 string
  const [gender, setGender] = useState("");
  const [phone, setPhone] = useState("");
  const [parentPhone, setParentPhone] = useState(""); // 19세 미만이면 필수
  const [profileImage, setProfileImage] = useState(null);

  const [isSubmitting, setIsSubmitting] = useState(false);

  const closeSignUp = useModalStore((state) => state.closeSignUp);
  const openLogin = useModalStore((state) => state.openLogin);

  const clearError = (key) => {
    setFieldErrors((prev) => {
      if (!prev[key]) return prev;
      const next = { ...prev };
      delete next[key];
      return next;
    });
  };

  // 아이디 중복확인 핸들러
  const handleCheckDuplicate = async () => {
    if (!username) return;

    //  아이디 형식이 맞아야만 체크
    if (isValidUsername !== true) {
      Swal.fire({
        icon: "warning",
        title: "아이디 형식 오류",
        text: "아이디 형식을 먼저 맞춰주세요.",
        confirmButtonColor: "#FF8A3D",
      });
      return;
    }

    setIsChecking(true);

    try {
      const available = await authApi.checkId(username); // true면 사용가능
      if (available) {
        setIsDuplicated(false); // 사용 가능
      } else {
        setIsDuplicated(true); // 중복
      }
    } catch (e) {
      Swal.fire({
        icon: "error",
        title: "중복확인 실패",
        text: "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
        confirmButtonColor: "#FF8A3D",
      });
    } finally {
      setIsChecking(false);
    }
  };

  // 회원가입 폼 제출
  const handleSubmit = async (e) => {
    e.preventDefault();
    if (isSubmitting) return;

    const nextErrors = {};
    const ageNumber = Number(age);

    // username: 필수 + 형식 + 중복확인 완료
    if (
      !username.trim() ||
      isValidUsername !== true ||
      isDuplicated !== false
    ) {
      nextErrors.username = true;
    }

    // password
    if (!password || password.length < 8) nextErrors.password = true;

    // passwordConfirm
    if (!passwordConfirm || password !== passwordConfirm)
      nextErrors.passwordConfirm = true;

    // required
    if (!email.trim()) nextErrors.email = true;
    if (!name.trim()) nextErrors.name = true;
    if (!phone.trim()) nextErrors.phone = true;
    if (!gender) nextErrors.gender = true;

    // age
    if (!age || Number.isNaN(ageNumber) || ageNumber < 8) nextErrors.age = true;

    // parentPhone: 20세 미만이면 필수 (백엔드 조건 기준 유지)
    if (ageNumber < 20 && !parentPhone.trim()) nextErrors.parentPhone = true;

    // 에러 있으면: 빨간색 표시 + 안내 후 종료
    if (Object.keys(nextErrors).length > 0) {
      setFieldErrors(nextErrors);

      // 메시지는 우선순위로 분기해주면 UX 좋아짐
      let msg = "빨간색 표시된 항목을 확인해주세요.";
      if (nextErrors.username && isDuplicated !== false)
        msg = "아이디 중복확인을 완료해주세요.";
      else if (nextErrors.password) msg = "비밀번호는 8자 이상 입력해주세요.";
      else if (nextErrors.passwordConfirm)
        msg = "비밀번호가 일치하지 않습니다.";
      else if (nextErrors.age) msg = "나이는 8세 이상으로 입력해주세요.";
      else if (nextErrors.parentPhone)
        msg = "20세 미만은 보호자 연락처가 필요합니다.";

      Swal.fire({
        icon: "warning",
        title: "입력 확인",
        text: msg,
        confirmButtonColor: "#FF8A3D",
      });
      return;
    }

    setIsSubmitting(true);

    try {
      const joinData = {
        username,
        password,
        name,
        gender,
        age: ageNumber,
        phone,
        email,
        parentPhone: ageNumber < 20 ? parentPhone : null,
      };

      await authApi.join({ data: joinData, profileImage });

      Swal.fire({
        icon: "success",
        title: "회원가입 완료",
        text: "회원가입이 완료되었습니다! 로그인 후 이용해주세요.",
        confirmButtonColor: "#FF8A3D",
      });

      closeSignUp();
      openLogin();
    } catch (err) {
      const status = err?.response?.status;

      let msg = "회원가입 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
      if (status === 409) msg = "이미 사용 중인 아이디입니다.";
      if (status === 400) msg = "입력값을 다시 확인해주세요.";
      if (status === 413) msg = "프로필 이미지 용량이 너무 큽니다. (20MB 이하)";
      if (status === 500) msg = "서버 오류가 발생했습니다.";

      Swal.fire({
        icon: "error",
        title: "회원가입 실패",
        text: msg,
        confirmButtonColor: "#FF8A3D",
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    // 배경 클릭 시 회원가입 모달 닫기
    <div css={s.overlay}>
      {/* 모달 내부 클릭했을때 overlay 클릭 이벤트 차단*/}
      <div css={s.modal} onClick={(e) => e.stopPropagation()}>
        {/* 모달 타이틀 */}
        <div css={s.title}>회원가입</div>

        <form css={s.form} onSubmit={handleSubmit}>
          {/* 아이디 입력*/}
          <label css={s.formLabel}>
            <span css={s.required}>*</span>
            아이디
          </label>

          <div css={s.inputWithButton}>
            <input
              css={[s.input, fieldErrors.username && s.fieldError]}
              value={username}
              onChange={(e) => {
                const value = e.target.value;
                setUsername(value);
                clearError("username");

                const usernameRegex = /^(?=.*[a-z])[a-z0-9_]{4,12}$/;

                if (!value) {
                  setIsValidUsername(null);
                  setIsDuplicated(null);
                } else if (!usernameRegex.test(value)) {
                  setIsValidUsername(false);
                  setIsDuplicated(null);
                } else {
                  setIsValidUsername(true);
                  setIsDuplicated(null);
                }
              }}
              placeholder="아이디를 입력하세요"
            />

            {/* 중복 확인 버튼 */}
            <button
              type="button"
              css={s.dupCheckBtn}
              onClick={handleCheckDuplicate}
              disabled={
                isChecking || isDuplicated === false || isValidUsername !== true
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
            css={[s.input, fieldErrors.password && s.fieldError]}
            placeholder="비밀번호를 8자 이상 입력해주세요."
            type="password"
            value={password}
            onChange={(e) => {
              setPassword(e.target.value);
              clearError("password");
            }}
          />
          <label css={s.formLabel}>
            <span css={s.required}>*</span>
            비밀번호 확인
          </label>
          <input
            css={[s.input, fieldErrors.passwordConfirm && s.fieldError]}
            placeholder="비밀번호를 다시 입력해주세요."
            type="password"
            value={passwordConfirm}
            onChange={(e) => {
              setPasswordConfirm(e.target.value);
              clearError("passwordConfirm");
            }}
          />

          {/* 이메일 */}
          <label css={s.formLabel}>
            <span css={s.required}>*</span>
            이메일
          </label>
          <input
            css={[s.input, fieldErrors.email && s.fieldError]}
            placeholder="example@email.com"
            value={email}
            onChange={(e) => {
              setEmail(e.target.value);
              clearError("email");
            }}
          />

          {/* 이름 */}
          <label css={s.formLabel}>
            <span css={s.required}>*</span>
            이름
          </label>
          <input
            css={[s.input, fieldErrors.name && s.fieldError]}
            placeholder="홍길동"
            value={name}
            onChange={(e) => {
              setName(e.target.value);
              clearError("name");
            }}
          />
          {/* 나이 + 성별 */}
          <div css={s.row}>
            <div css={s.field}>
              <label css={s.formLabel}>
                <span css={s.required}>*</span>
                나이
              </label>
              <input
                css={[s.input, fieldErrors.age && s.fieldError]}
                placeholder="정확한 나이를 기입해주세요"
                type="number"
                value={age}
                onChange={(e) => {
                  setAge(e.target.value);
                  clearError("age");
                }}
              />
            </div>

            <div css={s.field}>
              <label css={s.formLabel}>
                <span css={s.required}>*</span>
                성별
              </label>
              <select
                css={[s.select, fieldErrors.gender && s.fieldError]}
                value={gender}
                onChange={(e) => {
                  setGender(e.target.value);
                  clearError("gender");
                }}
              >
                <option css={s.option} value="">
                  성별 선택
                </option>
                <option css={s.option} value="Male">
                  남성
                </option>
                <option css={s.option} value="Female">
                  여성
                </option>
              </select>
            </div>
          </div>

          {/*  20세 미만이면 보호자 연락처 */}
          {age && Number(age) < 20 && (
            <>
              <label css={s.formLabel}>
                <span css={s.required}>*</span>
                보호자 연락처
              </label>
              <input
                css={[s.input, fieldErrors.parentPhone && s.fieldError]}
                placeholder="부모님 연락처를 기입해주세요."
                value={parentPhone}
                onChange={(e) => {
                  setParentPhone(e.target.value);
                  clearError("parentPhone");
                }}
              />
            </>
          )}

          {/* 전화번호 */}
          <label css={s.formLabel}>
            <span css={s.required}>*</span>
            전화번호
          </label>
          <input
            css={[s.input, fieldErrors.phone && s.fieldError]}
            placeholder="xxx-xxxx-xxxx"
            value={phone}
            onChange={(e) => {
              setPhone(e.target.value);
              clearError("phone");
            }}
          />

          {/* 프로필 이미지 업로드 */}
          <label css={s.formLabel}>
            <BsPersonCircle size={35} />
            프로필 이미지 업로드
          </label>
          <label css={s.uploadBox}>
            <input
              type="file"
              accept="image/*"
              css={s.hiddenFileInput}
              onChange={(e) => {
                const file = e.target.files[0];
                if (!file) return;

                // 파일 크기 제한 (20MB)
                if (file.size > 20 * 1024 * 1024) {
                  Swal.fire({
                    icon: "warning",
                    title: "업로드 제한",
                    text: "20MB 이하의 이미지만 업로드 가능합니다.",
                    confirmButtonColor: "#FF8A3D",
                  });
                  return;
                }
                console.log(file);
                setProfileImage(file);
              }}
            />

            <div css={s.uploadContent}>
              <div css={s.uploadIcon}>
                <FiCamera />
              </div>

              <p css={s.uploadText}>이미지를 드래그하거나 선택해서 업로드</p>

              <span css={s.uploadSubText}>최대 20MB 이하</span>

              <div css={s.uploadBtn}>파일 선택</div>
            </div>
          </label>

          {/* 회원가입 버튼 */}
          <button
            css={s.submitBtn}
            type="submit"
            disabled={isDuplicated !== false || isSubmitting}
          >
            {isSubmitting ? "가입 중..." : "회원가입 완료"}
          </button>
        </form>

        {/* 로그인 모달로 이동 */}
        <div css={s.loginRow}>
          <span css={s.loginMent}>이미 계정이 있나요?</span>
          <span
            css={s.loginLink}
            onClick={() => {
              closeSignUp();
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

export default SignUpModal;
