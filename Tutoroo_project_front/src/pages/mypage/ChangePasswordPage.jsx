/** @jsxImportSource @emotion/react */
import { useState } from "react";
import Header from "../../components/layouts/Header";
import Sidebar from "./Sidebar";
import * as s  from "./styles";

function ChangePasswordPage() {
   const [ passwords, setPasswords ] = useState({
        currentPassword: "",
        newPassword: "",
        confirmPassword: ""
   });

   const handleInputChange = (e) => {
        const {name, value} = e.target; //사건이 일어난 주인공 태그를 말함
        setPasswords({...passwords, [name]: value});
   }
    
    return <>
        <Header />
        <div css={s.wrapper}>
            <Sidebar />

            <main css={s.mainContainer}>
                <div css={s.passwordCard}>
                    <h1 css={s.pageTitle}>비밀번호 변경</h1>
                    <div css={s.passwordFormSection}>
                        <div css={s.passwordInputGroup}>
                            <label>현재 비밀번호</label>
                            <input 
                            type="password" 
                            name="currentPassword" 
                            placeholder="현재 비밀번호를 입력해주세요" 
                            value={passwords.currentPassword} 
                            onChange={handleInputChange}/>
                        </div>
                        <div css={s.passwordInputGroup}>
                            <label>새로운 비밀번호</label>
                            <input 
                            type="password" 
                            name="newPassword" 
                            placeholder="8자 이상 입력해주세요" 
                            value={passwords.newPassword} 
                            onChange={handleInputChange}/>
                        </div>
                        <div css={s.passwordInputGroup}>
                            <label>현재 비밀번호</label>
                            <input 
                            type="password" 
                            name="currentPassword" 
                            placeholder="새로운 비밀번호를 다시 입력해주세요" 
                            value={passwords.confirmPassword} 
                            onChange={handleInputChange}/>
                        </div>
                        
                    </div>
                    <button css={s.subBtn}>변경하기</button>
                </div>

            </main>

        </div>
    </>
}

export default ChangePasswordPage;