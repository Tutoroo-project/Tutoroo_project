import { Route, Routes } from "react-router-dom";
import DashboardPage from "../pages/dashboards/DashboardPage";
import LoginPage from "../pages/auths/LoginPage";
import PasswordVerifyPage from "../pages/mypage/PasswordVerifyPage";
import ChangeInfoPage from "../pages/mypage/ChangeInfoPage";
import ChangePasswordPage from "../pages/mypage/ChangePasswordPage";
import WithdrawalPage from "../pages/mypage/WithdrawalPage";
import RankingPage from "../pages/ranking/RankingPage";
import TutorSelectionPage from "../pages/tutor/TutorSelectionPage";
import StudyPage from "../pages/studys/StudyPage";
import LevelTestPage from "../pages/leveltests/LevelTestPage";
import LevelTestResultPage from "../pages/leveltests/LevelTestResultPage";
import Payments from "../pages/payments/Payments";
import PaymentPage from "../pages/payments/PaymentPage";

function Router() {
  return (
    <Routes>
      <Route path="/" element={<DashboardPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/mypage/verify" element={<PasswordVerifyPage />} />
      <Route path="mypage/changeinfo" element={<ChangeInfoPage />} />
      <Route path="mypage/changepassword" element={<ChangePasswordPage />} />
      <Route path="mypage/withdrawal" element={<WithdrawalPage />} />
      <Route path="/ranking" element={<RankingPage />} />
      <Route path="/subscribe" element={<Payments />} />
      <Route path="/subscribe/payment" element={<PaymentPage />} />
      <Route path="/level-test" element={<LevelTestPage />} />
      <Route path="/level-test/result" element={<LevelTestResultPage />} />
      <Route path="/tutor" element={<TutorSelectionPage />} />
      <Route path="/study" element={<StudyPage />} />
    </Routes>
  );
}

export default Router;
