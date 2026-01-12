import { Route, Routes } from "react-router-dom";
import DashboardPage from "../pages/dashboards/DashboardPage";
import LoginPage from "../pages/auths/LoginPage";
import PasswordVerifyPage from "../pages/mypage/PasswordVerifyPage";

function Router() {
  return (
    <Routes>
      <Route path="/" element={<DashboardPage />} />

      {/* 선택: /login 진입 시 대시보드로 보내고 로그인 모달만 오픈 */}
      <Route path="/login" element={<LoginPage />} />
      <Route path="/mypage/verify" element={<PasswordVerifyPage />} />
      
    </Routes>
  );
}

export default Router;