/** @jsxImportSource @emotion/react */
import * as s  from "./styles";

import { useState } from "react";
import Header from "../../components/layouts/Header";
import { useNavigate } from "react-router-dom";
import logoImg from "../../assets/images/mascots/logo.jpg";

function PasswordVerifyPage() {
    const [ password, setPassword ] = useState("");
    const navigate = useNavigate();
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
                    <input type="password" placeholder="비밀번호를 입력해주세요" />
                </div>
                <button css={s.submitBtn}>확인</button>
            </div>
        </div>
           

       
    </>
}

export default PasswordVerifyPage;