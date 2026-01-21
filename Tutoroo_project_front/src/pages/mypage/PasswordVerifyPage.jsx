/** @jsxImportSource @emotion/react */
import * as s  from "./styles";

import { useState } from "react";
import Header from "../../components/layouts/Header";
import { useNavigate } from "react-router-dom";
import logoImg from "../../assets/images/mascots/logo.jpg";
import Swal from "sweetalert2";
import { userApi } from "../../apis/users/usersApi";

function PasswordVerifyPage() {
    const [ password, setPassword ] = useState("");
    const navigate = useNavigate();

    const onClickHandleConfirm = async() => {
        if (password.trim() === "") {
            Swal.fire("입력 오류", "비밀번호를 입력해주세요.", "warning");
            return;
        }

        try {
             await userApi.verifyPassword(password);

            Swal.fire({
                icon: 'success',
                title: '인증 성공',
                text: '비밀번호가 확인되었습니다.',
                confirmButtonColor: '#3085d6', 
                confirmButtonText: '확인'
            }).then((result) => {
                if (result.isConfirmed) {
                    navigate("/mypage/changeinfo");
                }
            });
        } catch (error) {
            console.error(error);
            const errorMessage = error.response?.data?.message || '비밀번호가 일치하지 않습니다.';
            Swal.fire({
                icon: 'error',
                title: '인증 실패',
                text: errorMessage,
                confirmButtonColor: '#d33'
            });
            setPassword("");
        }
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
                    <input 
                        type="password" 
                        value={password} 
                        onChange={(e) => setPassword(e.target.value)} 
                        onKeyDown={handleKeyPress} 
                        placeholder="비밀번호를 입력해주세요" />
                </div>
                <button css={s.submitBtn} onClick={onClickHandleConfirm}>확인</button>
            </div>
        </div>
           

       
    </>
}

export default PasswordVerifyPage;