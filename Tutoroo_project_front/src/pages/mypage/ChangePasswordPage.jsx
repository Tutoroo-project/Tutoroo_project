/** @jsxImportSource @emotion/react */
import { useState } from "react";
import Header from "../../components/layouts/Header";
import Sidebar from "./Sidebar";
import * as s  from "./styles";
import { useNavigate } from "react-router-dom";

function ChangePasswordPage() {
    const navigate = useNavigate();
    const [ passwords, setPasswords ] = useState({
            currentPassword: "",
            newPassword: "",
            confirmPassword: ""
    });

    const handleInputChange = (e) => {
            const {name, value} = e.target; 
            setPasswords({...passwords, [name]: value});
    }

    const handlePasswordChange = () => {
        if (!passwords.currentPassword || !passwords.newPassword || !passwords.confirmPassword) {
            alert("모든 창을 입력해주세요")
        }
    }
        
    return (
        <>
            <Header />
            <div css={s.wrapper}>
                <Sidebar />
                <main css={s.mainContainer}>
                    
                    
                    <div css={s.commonCard}>
                        
                        <h1 css={s.cardTitle}>비밀번호 변경</h1>
                        
                        <div css={[s.cardContent, s.centerMain]} >
                            <div css={s.commonInputGroup}>
                                <label>현재 비밀번호</label>
                                <input 
                                    type="password" name="currentPassword" 
                                    placeholder="현재 비밀번호를 입력해주세요" 
                                    value={passwords.currentPassword} 
                                    onChange={handleInputChange}
                                />
                            </div>
                            <div css={s.commonInputGroup}>
                                <label>새로운 비밀번호</label>
                                <input 
                                    type="password" name="newPassword" 
                                    placeholder="8자 이상 (영문/숫자/특수문자 포함)" 
                                    value={passwords.newPassword} 
                                    onChange={handleInputChange}
                                />
                            </div>
                            <div css={s.commonInputGroup}>
                                <label>새로운 비밀번호 확인</label>
                                <input 
                                    type="password" name="confirmPassword" 
                                    placeholder="비밀번호를 다시 입력해주세요" 
                                    value={passwords.confirmPassword} 
                                    onChange={handleInputChange}
                                />
                            </div>
                        </div>

                        <button css={s.actionBtn}>비밀번호 변경하기</button>
                    </div>

                </main>
            </div>
        </>
    );
}

export default ChangePasswordPage;