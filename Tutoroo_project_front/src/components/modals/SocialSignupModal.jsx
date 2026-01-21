/** @jsxImportSource @emotion/react */
import { useMemo, useRef, useState } from "react";
import Swal from "sweetalert2";

import useModalStore from "../../stores/modalStore";
import useAuthStore from "../../stores/useAuthStore";
import { authApi } from "../../apis/users/usersApi";

import * as s from "./styles";

function SocialSignupModal() {
  const closeSocialSignup = useModalStore((st) => st.closeSocialSignup);
  const login = useAuthStore((st) => st.login);

  const fileInputRef = useRef(null);

  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [age, setAge] = useState("");
  const [gender, setGender] = useState(""); // male | female
  const [parentPhone, setParentPhone] = useState("");

  const [profileImage, setProfileImage] = useState(null);
  const [previewUrl, setPreviewUrl] = useState("");

  const [isSubmitting, setIsSubmitting] = useState(false);

  const ageNum = useMemo(() => Number(age), [age]);
  const needsParentPhone =
    Number.isFinite(ageNum) && ageNum >= 8 && ageNum < 20;

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

  const handleClose = () => {
    if (isSubmitting) return;
    closeSocialSignup();
  };

  const handlePickImage = () => {
    if (isSubmitting) return;
    fileInputRef.current?.click();
  };

  const handleImageChange = (e) => {
    const file = e.target.files?.[0] ?? null;
    setProfileImage(file);

    if (previewUrl) URL.revokeObjectURL(previewUrl);

    if (file) {
      const url = URL.createObjectURL(file);
      setPreviewUrl(url);
    } else {
      setPreviewUrl("");
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (isSubmitting) return;

    //  백엔드 AuthDTO.SocialSignupRequest 검증과 동일하게 체크
    if (!email.trim() || !emailRegex.test(email.trim())) {
      Swal.fire({
        icon: "warning",
        title: "이메일 확인",
        text: "올바른 이메일 형식으로 입력해주세요.",
        confirmButtonColor: "#FF8A3D",
      });
      return;
    }

    if (!phone.trim()) {
      Swal.fire({
        icon: "warning",
        title: "휴대폰 확인",
        text: "휴대폰 번호를 입력해주세요.",
        confirmButtonColor: "#FF8A3D",
      });
      return;
    }

    if (!age || Number.isNaN(ageNum) || ageNum < 8) {
      Swal.fire({
        icon: "warning",
        title: "나이 확인",
        text: "나이는 8세 이상으로 입력해주세요.",
        confirmButtonColor: "#FF8A3D",
      });
      return;
    }

    if (!gender) {
      Swal.fire({
        icon: "warning",
        title: "성별 확인",
        text: "성별을 선택해주세요.",
        confirmButtonColor: "#FF8A3D",
      });
      return;
    }

    if (needsParentPhone && !parentPhone.trim()) {
      Swal.fire({
        icon: "warning",
        title: "보호자 연락처",
        text: "20세 미만은 보호자 연락처가 필요합니다.",
        confirmButtonColor: "#FF8A3D",
      });
      return;
    }

    const payload = {
      email: email.trim(),
      phone: phone.trim(),
      age: ageNum,
      gender: gender,
      parentPhone: needsParentPhone ? parentPhone.trim() : null,
    };

    setIsSubmitting(true);
    try {
      const res = await authApi.completeSocialSignup({
        data: payload,
        profileImage, // 선택
      });

      //  응답: AuthDTO.LoginResponse
      login(res);

      Swal.fire({
        icon: "success",
        title: "가입 완료",
        text: "추가 정보 입력이 완료되었습니다!",
        confirmButtonColor: "#FF8A3D",
      });

      closeSocialSignup();
    } catch (err) {
      Swal.fire({
        icon: "error",
        title: "처리 실패",
        text: "추가 정보 저장 중 오류가 발생했습니다.",
        confirmButtonColor: "#FF8A3D",
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div css={s.overlay}>
      <div css={s.modal} onClick={(e) => e.stopPropagation()}>
        {/* 상단 헤더 (X 대신 텍스트 닫기 버튼 스타일 재사용) */}
        <div css={s.header}>
          <div />
          <div css={s.logoWrap}>
            {/* 로고가 필요하면 여기에 img 넣어도 됨 */}
          </div>
          <button css={s.exitBtn} onClick={handleClose} type="button">
            닫기
          </button>
        </div>

        <h2 css={s.title}>추가 정보 입력</h2>
        <p css={s.description}>소셜 로그인 최초 1회만 입력하면 됩니다.</p>

        <form css={s.form} onSubmit={handleSubmit}>
          <input
            type="text"
            placeholder="이메일"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            disabled={isSubmitting}
          />

          <input
            type="text"
            placeholder="휴대폰 번호"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            disabled={isSubmitting}
          />

          <input
            type="number"
            placeholder="나이 (8세 이상)"
            min={8}
            value={age}
            onChange={(e) => setAge(e.target.value)}
            disabled={isSubmitting}
          />

          <select
            css={s.select}
            value={gender}
            onChange={(e) => setGender(e.target.value)}
            disabled={isSubmitting}
          >
            <option value="">성별 선택</option>
            <option value="male">남성</option>
            <option value="female">여성</option>
          </select>

          {needsParentPhone && (
            <input
              type="text"
              placeholder="보호자 연락처 (20세 미만 필수)"
              value={parentPhone}
              onChange={(e) => setParentPhone(e.target.value)}
              disabled={isSubmitting}
            />
          )}

          {/* 프로필 이미지 업로드 (선택) */}
          <div css={s.uploadBox} onClick={handlePickImage} role="button">
            <input
              ref={fileInputRef}
              css={s.hiddenFileInput}
              type="file"
              accept="image/*"
              onChange={handleImageChange}
              disabled={isSubmitting}
            />

            {previewUrl ? (
              <img
                src={previewUrl}
                alt="profile preview"
                style={{
                  width: "100%",
                  height: "100%",
                  objectFit: "cover",
                  borderRadius: 12,
                }}
              />
            ) : (
              <div css={s.uploadContent}>
                <div css={s.uploadIcon}>🖼️</div>
                <div css={s.uploadText}>프로필 이미지 업로드 (선택)</div>
                <div css={s.uploadSubText}>클릭해서 사진을 선택하세요</div>
                <div css={s.uploadBtn}>파일 선택</div>
              </div>
            )}
          </div>

          <button css={s.submitBtn} type="submit" disabled={isSubmitting}>
            {isSubmitting ? "처리 중..." : "완료"}
          </button>
        </form>
      </div>
    </div>
  );
}

export default SocialSignupModal;
