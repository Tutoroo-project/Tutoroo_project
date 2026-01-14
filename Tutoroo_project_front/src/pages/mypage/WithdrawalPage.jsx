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


    return <>
        <Header />
        <div css={s.wrapper}>
            <Sidebar />
            <main css={s.mainContainer}>
                <div css={s.withdrawalCard}>
                    <h1 css={s.pageTitle}>회원탈퇴</h1>
                    <div css={s.withdrawalFormSection}>
                        <div css={s.warningBox}>
                            <h3>
                                <TiWarning />
                                탈퇴 시 유의사항
                            </h3>
                            <ul>
                                <li>회원 탈퇴 시 회원님의 모든 정보가 즉시 삭제됩니다.</li>
                                <li>삭제된 데이터는 복구할 수 없습니다.</li>
                                <li>결제 진행된 구독서비스를 해제 후 탈퇴해주시기 바랍니다.</li>
                            </ul>
                        </div>

                        <div css={s.agreementSection}>
                            <input type="checkbox"  id="agreeCheck" checked={agree} onChange={(e) => setAgree(e.target.checked) } />
                            <label htmlFor="agreeCheck">안내사항을 모두 확인하였으며, 이에 동의</label>
                        </div>

                        <div css={s.reasonBox}>
                            <div className="label-row"></div>
                            <label>탈퇴 사유</label>
                            <span>최대 300자 이내</span>
                        </div>
                        <textarea 
                        placeholder="탈퇴사유에 대하여 입력해주세요"
                         value={reason} 
                         onChange={(e) => setReason(e.target.value)} 
                         maxLength={300} />
                    </div>

                    <div css={s.passwordInputGroup}>
                        <label>비밀번호 확인</label>
                        <input 
                            type="password" 
                            placeholder="비밀번호를 입력해주세요" 
                            value={password} 
                            onChange={(e) => setPassword(e.target.value)} />
                    </div>

                    <button css={s.subBtn}>탈퇴하기</button>

                </div>
            </main>
        </div>
    </>
}

export default WithdrawalPage;