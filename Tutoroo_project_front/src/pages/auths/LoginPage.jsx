import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import useModalStore from "../../stores/modalStore";

// 로그인 전용 라우트용 페이지 (URL에 /login 을 쳐서 오면 자동으로 모달이 열림)
function LoginPage() {
  const navigate = useNavigate();
  // 모달 동작
  const openLogin = useModalStore((s) => s.openLogin);

  useEffect(() => {
    openLogin(); //페이지 진입시 로그인 모달 열기
    navigate("/", { replace: true }); // URL을 "/"로 교체
  }, [openLogin, navigate]);

  return null;
}

export default LoginPage;
