import { Route, Routes } from "react-router-dom";
import DashboardPage from "../pages/dashboards/DashboardPage";
import LoginPage from "../pages/auths/LoginPage";
import PasswordVerifyPage from "../pages/mypage/PasswordVerifyPage";
import ChangeInfoPage from "../pages/mypage/ChangeInfoPage";
import Sidebar from "../pages/mypage/Sidebar";
import ChangePasswordPage from "../pages/mypage/ChangePasswordPage";
import WithdrawalPage from "../pages/mypage/WithdrawalPage";

import TutorSelectionPage from "../pages/tutor/TutorSelectionPage";
import StudyPage from "../pages/studys/StudyPage";

function Router() {
  return (
    <Routes>
      <Route path="/" element={<DashboardPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/mypage/verify" element={<PasswordVerifyPage />} />
      {/* <Route path="sidebar" element={<Sidebar />} /> */}
      <Route path="mypage/changeinfo" element={<ChangeInfoPage/>}/>
      <Route path="mypage/changepassword" element={<ChangePasswordPage />} />
      <Route path="mypage/withdrawl" element={<WithdrawalPage />} />
      

      <Route path="/tutor" element={<TutorSelectionPage />} />
      <Route path="/study" element={<StudyPage />} />
    </Routes>
  );
}

export default Router;