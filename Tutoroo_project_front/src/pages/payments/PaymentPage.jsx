/** @jsxImportSource @emotion/react */
import { useState } from "react";
import * as s  from "./styles";
import Header from "../../components/layouts/Header";
import { RiKakaoTalkFill } from "react-icons/ri";
import { SiNaver } from "react-icons/si";
import { FaCreditCard } from "react-icons/fa";

function PaymentPage() {
    const [ payMethod, setPayMethod ] = useState("KAKAO");

   const method = {
        KAKAO: {
            title: "kakaopay",
            desc: "카카오페이로 빠르고 간편하게 결제하세요",
            color: "#3A1D1E" // 카카오 글자색
        },
        NAVER: {
            title: "N pay",
            desc: "네이버페이로 포인트 혜택을 받으세요",
            color: "#fff"
        },
        CARD: {
            title: "카드 결제",
            desc: "한국에서 발행된 모든 신용/체크카드를 지원합니다",
            color: "#333"
        }
    };

    return <>
        <Header />
        <div css={s.container}>
            <h2 css={s.pageTitle}>결제 수단 선택</h2>

            <div css={s.methodButtons}>
                <button 
                    css={[s.kakaoBtn, payMethod === "KAKAO" && s.activeMethod]} 
                    onClick={() => setPayMethod("KAKAO")}>

                        <RiKakaoTalkFill size={36} style={{backgroundColor: "#f0f0f0", padding: "0"}}/>
                        kakaopay
                </button>

                 <button 
                    css={[s.naverBtn, payMethod === "NAVER" && s.activeMethod]} 
                    onClick={() => setPayMethod("NAVER")}>

                        <SiNaver size={18} style={{marginRight: '5px'}}/>
                        pay 결제
                </button>

                 <button 
                    css={[s.cardBtn, payMethod === "CARD" && s.activeMethod]} 
                    onClick={() => setPayMethod("CARD")}>

                        <FaCreditCard size={20} style={{marginRight: '8px', color: '#666'}}/>
                        카드 결제
                </button>
            </div>

            {/* <div css={s.detailBox}>
                <div css={s.methodInfo}>
                    
                </div>
            </div> */}
        </div>
    </>
}

export default PaymentPage;
