import useModalStore from "../../stores/modalStore";
import FindIdModal from "./FindIdModal";
import FindPwModal from "./FindPwModal";
import LoginModal from "./LoginModal";
import SignupModal from "./SignupModal";

function ModalRoot() {
  const isLoginOpen = useModalStore((s) => s.isLoginOpen);
  const isSignUpOpen = useModalStore((s) => s.isSignUpOpen);
  const isFindIdOpen = useModalStore((s) => s.isFindIdOpen);
  const isFindPwOpen = useModalStore((s) => s.isFindPwOpen);

  return (
    <>
      {isLoginOpen && <LoginModal />}
      {isSignUpOpen && <SignupModal />}
      {isFindIdOpen && <FindIdModal />}
      {isFindPwOpen && <FindPwModal />}
    </>
  );
}

export default ModalRoot;
