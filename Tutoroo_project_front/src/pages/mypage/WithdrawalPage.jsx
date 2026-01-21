/** @jsxImportSource @emotion/react */
import { TiWarning } from "react-icons/ti";
import Header from "../../components/layouts/Header";
import Sidebar from "./Sidebar";
import * as s  from "./styles";
import { useState } from "react";
import Swal from "sweetalert2";
import { userApi } from "../../apis/users/usersApi";

function WithdrawalPage() {

    const [ agree, setAgree ] = useState(false);
    const [ reason, setReason ] = useState("");
    const [ password, setPassword ] = useState("");
    const [ isLoading, setIsLoading ] = useState(false);

    const handleWithdraw = async () => {
        if(!agree) 
            Swal.fire("알림", "안내사항에 동의해주세요", "warning")
      
        if (!password) 
            Swal.fire("알림", "비밀번호를 입력해주세요.", "warning");
     
        const confirm = await Swal.fire({
            title: '정말 탈퇴하시겠습니까?',
            text: "탈퇴 후 90일간 데이터가 보관되며 이후 영구 삭제됩니다.",
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#d33',
            confirmButtonText: '탈퇴하기',
            cancelButtonText: '취소'
        });

        if (confirm.isConfirmed) {
            setIsLoading(true);
            try {
                await userApi.withdraw(password, reason);
                await Swal.fire("완료", "탈퇴 처리가 완료되었습니다.", "success");

                localStorage.clear();
                window.location.href = "/login";

            } catch(error) {
                console.log("탈퇴 실패: ", error)
                Swal.fire("실패", "오류가 발생했습니다.", "error");
            } finally {
                setIsLoading(false);
            }
        }
    }

 


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