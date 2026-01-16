/** @jsxImportSource @emotion/react */
import * as s from "./styles";
import { FiCamera } from "react-icons/fi";
import useModalStore from "../../stores/modalStore";
import useAuthStore from "../../stores/useAuthStore";
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

  // ✅ 회원가입 입력값 상태들
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

  // 회원가입 성공시 로그인 처리 (임시로)
  const login = useAuthStore((state) => state.login);

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

    // 중복확인 안 했거나, 중복이면 제출 차단
    if (isDuplicated !== false) {
      Swal.fire({
        icon: "warning",
        title: "중복확인 필요",
        text: "아이디 중복확인을 완료해주세요.",
        confirmButtonColor: "#FF8A3D",
      });
      return;
    }

    // ✅ 기본 입력값 검증
    if (!password || password.length < 8) {
      Swal.fire({
        icon: "warning",
        title: "비밀번호 오류",
        text: "비밀번호는 8자 이상 입력해주세요.",
        confirmButtonColor: "#FF8A3D",
      });
      return;
    }

    if (password !== passwordConfirm) {
      Swal.fire({
        icon: "warning",
        title: "비밀번호 확인",
        text: "비밀번호가 일치하지 않습니다.",
        confirmButtonColor: "#FF8A3D",
      });
      return;
    }

    const ageNumber = Number(age);
    if (
      !name ||
      !email ||
      !phone ||
      !gender ||
      !age ||
      Number.isNaN(ageNumber)
    ) {
      Swal.fire({
        icon: "warning",
        title: "입력 오류",
        text: "필수 항목을 모두 입력해주세요.",
        confirmButtonColor: "#FF8A3D",
      });
      return;
    }

    if (ageNumber < 8) {
      Swal.fire({
        icon: "warning",
        title: "나이 제한",
        text: "8세 이상만 가입 가능합니다.",
        confirmButtonColor: "#FF8A3D",
      });
      return;
    }

    //  백엔드 조건: 20세 미만이면 parentPhone 필수
    if (ageNumber < 20 && !parentPhone.trim()) {
      Swal.fire({
        icon: "warning",
        title: "보호자 연락처 필요",
        text: "20세 미만은 보호자 연락처를 입력해야 합니다.",
        confirmButtonColor: "#FF8A3D",
      });
      return;
    }

    setIsSubmitting(true);

    try {
      //  회원가입 요청 payload
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

      //  가입 성공하면 바로 로그인까지
      const loginData = await authApi.login({ username, password });
      login(loginData);

      Swal.fire({
        icon: "success",
        title: "회원가입 완료",
        text: "회원가입이 완료되었습니다!",
        confirmButtonColor: "#FF8A3D",
      });

      closeSignUp();
    } catch (err) {
      const status = err?.response?.status;

      // 400/409 등은 백엔드 예외 케이스가 섞일 수 있음
      let msg = "회원가입 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";

      if (status === 409) msg = "이미 사용 중인 아이디입니다.";
      if (status === 400) msg = "입력값을 다시 확인해주세요.";

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
              css={s.input}
              value={username}
              onChange={(e) => {
                const value = e.target.value;
                setUsername(value);

                // 아이디 형식 검사 (4~12자 영문 소문자, 숫자, _가능)
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
            css={s.input}
            placeholder="비밀번호를 8자 이상 입력해주세요."
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />

          <label css={s.formLabel}>
            <span css={s.required}>*</span>
            비밀번호 확인
          </label>
          <input
            css={s.input}
            placeholder="비밀번호를 다시 입력해주세요."
            type="password"
            value={passwordConfirm}
            onChange={(e) => setPasswordConfirm(e.target.value)}
          />

          {/* 이메일 */}
          <label css={s.formLabel}>
            <span css={s.required}>*</span>
            이메일
          </label>
          <input
            css={s.input}
            placeholder="email@email.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />

          {/* 이름 */}
          <label css={s.formLabel}>
            <span css={s.required}>*</span>
            이름
          </label>
          <input
            css={s.input}
            placeholder="이름"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />

          {/* 나이 + 성별 */}
          <div css={s.row}>
            <div css={s.field}>
              <label css={s.formLabel}>
                <span css={s.required}>*</span>
                나이
              </label>
              <input
                css={s.input}
                placeholder="나이"
                type="number"
                value={age}
                onChange={(e) => setAge(e.target.value)}
              />
            </div>

            <div css={s.field}>
              <label css={s.formLabel}>
                <span css={s.required}>*</span>
                성별
              </label>
              <select
                css={s.select}
                value={gender}
                onChange={(e) => setGender(e.target.value)}
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

          {/*  19세 미만이면 보호자 연락처 */}
          {age && Number(age) < 19 && (
            <>
              <label css={s.formLabel}>
                <span css={s.required}>*</span>
                보호자 연락처
              </label>
              <input
                css={s.input}
                placeholder="010-0000-0000"
                value={parentPhone}
                onChange={(e) => setParentPhone(e.target.value)}
              />
            </>
          )}

          {/* 전화번호 */}
          <label css={s.formLabel}>
            <span css={s.required}>*</span>
            전화번호
          </label>
          <input
            css={s.input}
            placeholder="010-1234-5678"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
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

                // 파일 크기 제한 (5MB)
                if (file.size > 5 * 1024 * 1024) {
                  Swal.fire({
                    icon: "warning",
                    title: "업로드 제한",
                    text: "5MB 이하의 이미지만 업로드 가능합니다.",
                    confirmButtonColor: "#FF8A3D",
                  });
                  return;
                }

                // 나중에 API 연결 되고 수정하겠습니다
                console.log(file);
                setProfileImage(file);
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
