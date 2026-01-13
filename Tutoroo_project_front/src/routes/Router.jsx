import { Route, Routes } from "react-router-dom";
import DashboardPage from "../pages/dashboards/DashboardPage";
import LoginPage from "../pages/auths/LoginPage";

import TutorSelectionPage from "../pages/tutor/TutorSelectionPage";
import StudyPage from "../pages/studys/StudyPage";

function Router() {
  return (
    <Routes>
      <Route path="/" element={<DashboardPage />} />
      <Route path="/login" element={<LoginPage />} />

      <Route path="/tutor" element={<TutorSelectionPage />} />
      <Route path="/study" element={<StudyPage />} />
    </Routes>
  );
}

export default Router;