import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import useModalStore from "../../stores/modalStore";

function LoginPage() {
  const navigate = useNavigate();
  const openLogin = useModalStore((s) => s.openLogin);

  useEffect(() => {
    openLogin();
    navigate("/", { replace: true });
  }, [openLogin, navigate]);

  return null;
}

export default LoginPage;
