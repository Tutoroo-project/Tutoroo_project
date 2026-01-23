/** @jsxImportSource @emotion/react */
import { useState } from "react";
import Header from "../../components/layouts/Header";
import Sidebar from "./Sidebar";
import * as s  from "./styles";
import { useNavigate } from "react-router-dom";
import { userApi } from "../../apis/users/usersApi";

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

    const handlePasswordChange = async() => {
        if (!passwords.currentPassword || !passwords.newPassword || !passwords.confirmPassword) {
            alert("모든 필드를 입력해주세요.");
            return;
        }

        if (passwords.newPassword !== passwords.confirmPassword) {
            alert("새로운 비밀번호가 일치하지 않습니다.")
            return;
        }

        const passwordRegex = /^(?=.*[a-zA-Z])(?=.*[!@#$%^*+=-])(?=.*[0-9]).{8,15}$/;
        if (!passwordRegex.test(passwords.newPassword)) {
            alert("비밀번호는 8~15자 영문, 숫자, 특수문자를 포함해야 합니다.");
            return;
        }

        const requestData = {
            currentPassword : passwords.currentPassword,
            newPassword: passwords.newPassword,
            confirmPassword: passwords.confirmPassword
        };

        try {
            await userApi.changePassword({
                currentPassword: passwords.currentPassword,
                newPassword: passwords.newPassword,
                confirmPassword: passwords.confirmPassword
            });

            alert("비밀번호가 성공적으로 변경되었습니다.\n보안을 위해 다시 로그인해주세요.");
            
            localStorage.removeItem("accessToken");
            localStorage.removeItem("refreshToken");
           
            navigate("/login", { replace: true });


        } catch (error) {
            console.error(error);
            const message = error.response?.data?.message || "비밀번호 변경 중 오류가 발생했습니다.";
            alert(message);
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

                                {passwords.newPassword && passwords.confirmPassword && (
                                    passwords.newPassword === passwords.confirmPassword ? (
                                        <p style={{color: '#4CAF50', fontSize: '12px', marginTop: '4px'}}>
                                            ✔ 비밀번호가 일치합니다.
                                        </p>
                                    ) : (
                                        <p style={{color: '#F44336', fontSize: '12px', marginTop: '4px'}}>
                                            ❌ 비밀번호가 일치하지 않습니다.
                                        </p>
                                    )
                                )}
                            </div>
                        </div>

                        <button css={s.actionBtn} onClick={handlePasswordChange}>비밀번호 변경하기</button>
                    </div>

                </main>
            </div>
        </>
    );
}

export default ChangePasswordPage;