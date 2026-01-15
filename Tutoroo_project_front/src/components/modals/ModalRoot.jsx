import useModalStore from "../../stores/modalStore";
import FindIdModal from "./FindIdModal";
import FindPwModal from "./FindPwModal";
import LoginModal from "./LoginModal";
import SignupModal from "./SignupModal";

function ModalRoot() {
  // Zustand에서 설정한 각 boolean 값에 따라 해당 모달의 렌더링 여부가 결정

  const isLoginOpen = useModalStore((s) => s.isLoginOpen); // 로그인 모달 열림 여부
  const isSignUpOpen = useModalStore((s) => s.isSignUpOpen); // 회원가입 모달 열림 여부
  const isFindIdOpen = useModalStore((s) => s.isFindIdOpen); // 아이디 찾기 모달 열림 여부
  const isFindPwOpen = useModalStore((s) => s.isFindPwOpen); // 비밀번호 찾기 모달 열림 여부

  return (
    <>
      {/* 로그인 모달 */}
      {isLoginOpen && <LoginModal />}

      {/* 회원가입 모달 */}
      {isSignUpOpen && <SignupModal />}

      {/* 아이디 찾기 모달 */}
      {isFindIdOpen && <FindIdModal />}

      {/* 비밀번호 찾기 모달 */}
      {isFindPwOpen && <FindPwModal />}
    </>
  );
}

export default ModalRoot;
