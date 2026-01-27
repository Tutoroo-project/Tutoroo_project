import { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import useAuthStore from "../../stores/useAuthStore";
import useModalStore from "../../stores/modalStore";
import { userApi } from "../../apis/users/usersApi";

function OAuth2RedirectPage() {
  const navigate = useNavigate();
  const [params] = useSearchParams();

  const login = useAuthStore((s) => s.login);

  const closeLogin = useModalStore((s) => s.closeLogin);
  const closeAll = useModalStore((s) => s.closeAll);
  const openSocialSignup = useModalStore((s) => s.openSocialSignup); // 너가 추가해둔 액션

  useEffect(() => {
    (async () => {
      const accessToken = params.get("accessToken");
      const refreshToken = params.get("refreshToken");
      const isNewUser = params.get("isNewUser") === "true";

      if (!accessToken) {
        navigate("/");
        return;
      }

      // 1) 토큰 먼저 저장
      login({ accessToken, refreshToken });

      // 2) 토큰으로 내 프로필 받아서 user까지 채우기
      try {
        const profile = await userApi.getProfile(); // /api/user/profile
        login({ accessToken, refreshToken, ...profile, isNewUser });
      } catch (e) {
        // 프로필 실패해도 토큰은 저장돼 있으니 일단 진행
        console.error(e);
      }

      // 3) 모달 정리
      closeAll();

      // 4) 신규유저면 추가정보 모달
      if (isNewUser) openSocialSignup();

      navigate("/");
    })();
  }, [params, login, navigate, closeLogin, closeAll, openSocialSignup]);

  return null;
}

export default OAuth2RedirectPage;
