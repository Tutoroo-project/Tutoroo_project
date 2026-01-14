/** @jsxImportSource @emotion/react */
import * as s  from "./styles";

import { useState } from "react";
import Header from "../../components/layouts/Header";
import { useNavigate } from "react-router-dom";
import logoImg from "../../assets/images/mascots/logo.jpg";

function PasswordVerifyPage() {
    const [ password, setPassword ] = useState("");
    const navigate = useNavigate();

    const onClickHandleConfirm = () => {
        // if (!password) {
        //     alert("비밀번호를 확인해주세요.")
        //     return;
        // }

        if (password.trim() === "") { //임시 데이터
            alert("비밀번호를 입력해주세요.");
            return;
        }
        navigate("/mypage/changeinfo"); //임시 변환
    }

    const handleKeyPress = (e) => {
        if (e.key === "Enter") {
        onClickHandleConfirm();
        }
    };

    return <>
        <Header />
        
        <div css={s.authWrapper}>
            <div css={s.authCard}>
                <div css={s.titleWrapper}>
                    <img src={logoImg} alt="logo" /> 
                    <div css={s.authTitle}>비밀번호 인증</div>
                </div>
                <p css={s.authDesc}>
                안전한 서비스 이용을 위해 <br />
                비밀번호 인증 후 진행할 수 있습니다.    
                </p>
                <div css={s.inputGroup}>
                    <label>비밀번호 확인</label>
                    <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} onKeyDown={handleKeyPress} placeholder="비밀번호를 입력해주세요" />
                </div>
                <button css={s.submitBtn} onClick={onClickHandleConfirm}>확인</button>
            </div>
        </div>
           

       
    </>
}

export default PasswordVerifyPage;