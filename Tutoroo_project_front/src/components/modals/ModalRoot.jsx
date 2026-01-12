import useModalStore from "../../stores/modalStore";
import LoginModal from "./LoginModal";
import SignupModal from "./SignupModal";

function ModalRoot() {
  const isLoginOpen = useModalStore((s) => s.isLoginOpen);
  const isSignUpOpen = useModalStore((s) => s.isSignUpOpen);

  return (
    <>
      {isLoginOpen && <LoginModal />}
      {isSignUpOpen && <SignupModal />}
    </>
  );
}

export default ModalRoot;
