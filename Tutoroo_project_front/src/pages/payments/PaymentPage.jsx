/** @jsxImportSource @emotion/react */
import { useEffect, useState } from "react";
import * as s  from "./styles";
import Header from "../../components/layouts/Header";
import { RiKakaoTalkFill } from "react-icons/ri";
import { FaCheck, FaCreditCard } from "react-icons/fa";
import { useLocation, useNavigate } from "react-router-dom";
import axios from "axios";

function PaymentPage() {
    const [ payMethod, setPayMethod ] = useState("KAKAO");
    const navigate = useNavigate();
    const location = useLocation();

    const [ userInfo, setUserInfo ] = useState({
        id: null,
        name: "",
        email: "",
        phone: ""
    })

    const plan = location.state?.plan;

    useEffect(() => {
        if (!plan) {
            alert("잘못된 접근입니다. 플랜을 먼저 선택해주세요.");
            navigate("/subscribe")
        }

       
        const { IMP } = window; 
        if(IMP) {
            IMP.init("imp37560047"); 
        }

        const fetchMe = async() => {
            try {
                const token = localStorage.getItem("accessToken");

                if (!token) return;
                const response = await axios.get("http://localhost:8080/api/user/me", {
                    headers: { Authorization: `Bearer ${token}`}
                })

                setUserInfo({
                    id: response.data.id,
                    name: response.data.name,
                    email: response.data.email,
                    phone: response.data.phone
                })
            } catch (error) {
                console.log("유저 정보 로드 실패", error);
                // 로그인 안 된 상태면 로그인 페이지로 튕겨내기 로직 필요
            }
        }

        fetchMe();
    },[plan, navigate]);

    if (!plan) return<>플랜을 다시 선택해주세요</>

    const parsePrice = (priceString) => {
        if (priceString === "무료") return 0;
        return parseInt(priceString.replace(/[^0-9]/g, ""), 10);
    };

    const getPgCode = (method) => {
        switch(method) {
            case "KAKAO": return "kakaopay";
            case "TOSS": return "tosspay";
            case "CARD" : return "html5_inicis";
            default: return "kakaopay";
        }
    }

    const handlePayment = () => {
        const amount = parsePrice(plan.price);
        if (amount === 0) {

            // 무료 구독 처리 로직(API 호출 등) 추가
           alert("무료 플랜은 결제 과정 없이 바로 구독됩니다.")
           return;
        }

        if (!userInfo.id) {
            alert("유저 정보를 불러오는 중입니다. 잠시 후 다시 시도해주세요");
            return;
        }

        const merchantUid = `order_${userInfo.id}_${new Date().getTime()}`;

        const { IMP } = window;

        IMP.request_pay({
            pg: getPgCode(payMethod),
            pay_method: 'card',
            merchant_uid: merchantUid,   
            name: plan.title,            
            amount: amount,              
            buyer_email: userInfo.email, 
            buyer_name: userInfo.name,   
            buyer_tel: userInfo.tel,     
        }, async (resp) => {
            if (resp.success) {
                try {
                    const token = localStorage.getItem("accessToken");

                    const verifyData = {
                        impUid: resp.imp_uid,
                        merchantUid: resp.merchant_uid,
                        planId: plan.id || null,      
                        amount: resp.paid_amount,
                        itemName: plan.title, 
                        payMethod: resp.pay_method,
                        pgProvider: resp.pg_provider
                    };

                    const response = await axios.post(
                        "http://localhost:8080/api/payment/verify",
                        verifyData,
                        { headers: { Authorization: `Bearer ${token}`}}
                    );

                    if (response.data.success) {
                        alert(`결제 성공! ${response.data.message}`);
                        navigate("/");
                    } else {
                        alert("결제는 되었으나 서버 검증에 실패했습니다. 고객센터에 문의하세요.");
                    }
                } catch (error) {
                    console.error("검증 API 호출 에러", error);
                    alert("서버 통신 중 오류가 발생했습니다.");
                }
            } else {
               console.error("결제 실패", resp);
                alert(`결제에 실패하였습니다. 내용: ${resp.error_msg}`);
            }
        })

    };

    
    

   const method = {
        KAKAO: {
            title: "kakaopay",
            desc: "카카오페이로 빠르고 간편하게 결제하세요",
            color: "#3A1D1E" 
        },
        TOSS: {
            title: "Toss Pay",
            desc: "토스로 쉽고 안전하게 결제하세요",
            color: "#0064FF"
        },
        CARD: {
            title: "카드 결제",
            desc: "한국에서 발행된 모든 신용/체크카드를 지원합니다",
            color: "#333"
        }
    };

    return <>
        <Header />
        <div css={s.paymentContainer}>
            <h2 css={s.paymentPageTitle}>결제 수단 선택</h2>

            <div css={s.methodButtons}>
                <button 
                    css={[s.kakaoBtn, payMethod === "KAKAO" && s.activeMethod]} 
                    onClick={() => setPayMethod("KAKAO")}>

                        <RiKakaoTalkFill size={36} style={{backgroundColor: "#f0f0f0", padding: "0"}}/>
                        kakaopay
                </button>

                 <button 
                    css={[s.tossBtn, payMethod === "TOSS" && s.activeMethod]} 
                    onClick={() => setPayMethod("TOSS")}
                    style={{ 
                        color: payMethod === "TOSS" ? "#0064FF" : "#aaa", 
                        borderColor: payMethod === "TOSS" ? "#0064FF" : "#ddd" 
                    }} 
                >
                    <img 
                        src="https://static.toss.im/icons/png/4x/icon-toss-logo.png" 
                        alt="Toss Logo" 
                        style={{ width: "36px", height: "36px", marginRight: "8px" }} 
                    />
                        Toss Pay
                </button>

                 <button 
                    css={[s.cardBtn, payMethod === "CARD" && s.activeMethod]} 
                    onClick={() => setPayMethod("CARD")}>

                        <FaCreditCard size={20} style={{marginRight: '8px', color: '#666'}}/>
                        카드 결제
                </button>
            </div>

            <div css={s.detailBox}>
                <div css={s.methodInfo}>
                    <h3 css={s.methodTitle(payMethod)}>
                        {payMethod === "KAKAO" && <RiKakaoTalkFill />}    
                        {payMethod === "TOSS" && <img 
                                                    src="https://static.toss.im/icons/png/4x/icon-toss-logo.png" 
                                                    alt="Toss" 
                                                    style={{ width: "30px", height: "30px", marginRight: "5px" }} 
        />}    
                        {payMethod === "CARD" && <FaCreditCard />}  

                        <span style={{marginLeft: "5px"}}>{method[payMethod].title}</span> 
                    </h3>
                    <p css={s.methodDesc}>
                        <FaCheck size={12} color="#03C75A" style={{marginRight: "8px"}}/>
                        {method[payMethod].desc}
                    </p>
                </div>

                <div css={s.priceInfo}>
                    <div css={s.priceRow}>
                        <span>구독 요금({plan.title})</span>
                        <span>{plan.price}</span>
                        {/* =============================== */}
                    </div>
                    <div css={s.paymentDivider}></div>

                    <div css={s.totalRow}>
                        <span>부가세</span>
                        <strong className="total"> 0 원</strong>
                    </div>

                    <div css={s.paymentDivider}></div>

                    <div css={s.totalRow}>
                        <span>총 결제 금액</span>
                        <strong className="total">{plan.price}</strong>
                    </div>
                </div>
            </div>

            <div css={s.footer}>
                <button css={s.backBtn} onClick={() => navigate(-1)}> ← 뒤로 가기 </button>
                <button css={s.payBtn} onClick={handlePayment}>
                    결제하기 <FaCheck style={{marginLeft: '8px'}}/>
                </button>
            </div>
        </div>
    </>
}

export default PaymentPage;
