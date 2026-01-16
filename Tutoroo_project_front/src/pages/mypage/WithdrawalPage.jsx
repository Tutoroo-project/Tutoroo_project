/** @jsxImportSource @emotion/react */
import { TiWarning } from "react-icons/ti";
import Header from "../../components/layouts/Header";
import Sidebar from "./Sidebar";
import * as s  from "./styles";
import { useState } from "react";

function WithdrawalPage() {

    const handleWithdraw = () => {
        if(!agree) {
            alert("안내사항에 동의해주세요")
            return;
        }

        if (!password) {
            alert("비밀번호를 입력해주세요.")
            return;
        }

        if(window.confirm("정말 탈퇴하시겠습니까? 확인을 누르시면 되돌릴 수 없습니다.")) {
            alert("탈퇴 처리가 완료되었습니다.")
            console.log("탈퇴 사유: " , reason)
        }   
    }

    const [ agree, setAgree ] = useState(false);
    const [ reason, setReason ] = useState("");
    const [ password, setPassword ] = useState("");


   return (
        <>
            <Header />
            <div css={s.wrapper}>
                <Sidebar />
                <main css={s.mainContainer}>
                    
                   
                    <div css={s.commonCard}>
                        
                        <h1 css={s.cardTitle} style={{color:'#d32f2f'}}>회원탈퇴</h1>
                        
                        <div css={s.cardContent}>
                            <div css={s.warningBox}>
                                <h3><TiWarning /> 탈퇴 시 유의사항</h3>
                                <ul>
                                    <li>회원 탈퇴 시 모든 정보가 즉시 삭제됩니다.</li>
                                    <li>삭제된 데이터는 복구할 수 없습니다.</li>
                                    <li>구독 중인 서비스는 해지 후 탈퇴해주세요.</li>
                                </ul>
                            </div>

                            <div css={s.agreementSection}>
                                <input type="checkbox" id="agreeCheck" checked={agree} onChange={(e) => setAgree(e.target.checked)} />
                                <label htmlFor="agreeCheck">위 내용을 확인하였으며 동의합니다.</label>
                            </div>

                            <div css={s.reasonBox}>
                                <label>탈퇴 사유</label>
                                <textarea 
                                    placeholder="탈퇴 사유를 입력해주세요 (선택사항)"
                                    value={reason} 
                                    onChange={(e) => setReason(e.target.value)} 
                                    maxLength={300} 
                                />
                            </div>

                            <div css={s.commonInputGroup}>
                                <label>비밀번호 확인</label>
                                <input 
                                    type="password" 
                                    placeholder="본인 확인을 위해 비밀번호를 입력해주세요" 
                                    value={password} 
                                    onChange={(e) => setPassword(e.target.value)} 
                                />
                            </div>
                        </div>

                        <button css={s.actionBtn} onClick={handleWithdraw}>탈퇴하기</button>
                    </div>

                </main>
            </div>
        </>
    );
}

export default WithdrawalPage;