import { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import useAuthStore from "../../stores/useAuthStore";
import useModalStore from "../../stores/modalStore";

function OAuth2RedirectPage() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const login = useAuthStore((s) => s.login);
  const openSocialSignup = useModalStore((s) => s.openSocialSignup);

  useEffect(() => {
    const accessToken = params.get("accessToken");
    const refreshToken = params.get("refreshToken");
    const isNewUser = params.get("isNewUser") === "true";

    if (!accessToken) {
      navigate("/"); // 실패 케이스
      return;
    }

    //  authStore에 저장 (기존 login이 토큰+유저 저장하는 구조라면 그대로)
    login({ accessToken, refreshToken, isNewUser });

    //  최초 가입이면 추가정보 모달
    if (isNewUser) openSocialSignup();

    navigate("/");
  }, [params, login, openSocialSignup, navigate]);

  return null;
}

export default OAuth2RedirectPage;
