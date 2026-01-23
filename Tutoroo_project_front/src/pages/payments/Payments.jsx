/** @jsxImportSource @emotion/react */
import { useEffect, useState } from "react";
import * as s  from "./styles";
import Header from "../../components/layouts/Header";
import { FaCheck, FaCheckCircle } from "react-icons/fa";
import { useNavigate } from "react-router-dom";
import axios from "axios";

function Payments() {
    const navigate = useNavigate();
    const [ currentSubscription, setCurrentSubscription] = useState(null);
    const [ selectedPlan, setSelectedPlan ] = useState("BASIC");

    const plans = [
        {
            id: "BASIC",
            title: "BASIC",
            price: "무료",
            headerColor: "#ffffff", 
            features: ["구독 AI 튜터 무료 체험"],
            badge: "무료제공",
            level: 0
            
        },
        {
            id: "STANDARD",
            title: "STANDARD",
            price: "₩9,900 / 월",
            headerColor: "#fde8c8", 
            features: ["AI 튜터 무제한", "개인 학습 플랜", "진도 분석 리포트"],
            badge: null,
            level: 1
            
        },
        {
            id: "PREMIUM",
            title: "PREMIUM",
            price: "₩29,900 / 월",
            headerColor: "#f3d8f8",
            features: ["AI 풀 기능 사용", "개인 맞춤형 코칭", "주간 상세 리포트"],
            badge: null,
            level: 2
            
        }
    ];

    useEffect(() => {
        const fetchUserPlan = async() => {
            try {
                const token = localStorage.getItem("accessToken");
                if(!token) return ;
                    const response = await axios.get("http://localhost:8080/api/user/me", {
                        header: { Authorization: `Bearer ${token}`}
                    });

                    console.log("서버 응답 데이터:", response.data);

                    const userPlanId = response.data.plan || "BASIC";
                    setCurrentSubscription(userPlanId);

                    if (userPlanId !== "BASIC") {
                        setSelectedPlan(userPlanId);
                    }
                
            } catch (error) {
                console.error("유저 구독 정보 로드 실패", error);
            }
        };
        fetchUserPlan();

    },[]);

    const getCurrentPlanLevel = () => {
        const current = plans.find(p => p.id === currentSubscription) 
        return current ? current : 0;
    }

    const handlePaymentClick = () => {
        if (currentSubscription === selectedPlan && selectedPlan !== "BASIC") {
            alert("이미 이용 중인 플랜입니다.");
            return;
        }

        const planData = plans.find(plan => plan.id === selectedPlan);

        console.log("보내는 데이터:", planData);

        navigate("/subscribe/payment", { state: { plan: planData}});
    }

    return <>
        <Header />
        <div css={s.container}>
            <h2 css={s.pageTitle}>구독 플랜 선택</h2>

            <div css={s.cardContainer}>
                {plans.map((plan) => {
                    const isSelected = selectedPlan === plan.id;
                    const isCurrent = currentSubscription === plan.id;
                    const currentLevel = getCurrentPlanLevel();
                    const isDowngrade = plan.level < currentLevel;
                    const isDisabled = isDowngrade;

                    return (
                        <div 
                            key={plan.id} 
                            css={
                                [
                                   s.card, isSelected && s.selectedCard, isDisabled && s.disabledCard
                                ]} 
                               
                            onClick={() => { if (!isDisabled) setSelectedPlan(plan.id); }} >
                            {isSelected && !isDisabled && <div css={s.checkBadge}><FaCheckCircle /></div>}
                            {isDisabled && <div css={s.disabledOverlay}><FaBan /> <span>선택 불가</span></div>}

                            <div css={s.cardHeader(plan.headerColor)}>
                                {plan.badge && <span css={s.freeBadge}>{plan.badge}</span>}
                                <h3 css={s.planTitle}>{plan.title}</h3>
                            </div>

                            <div css={s.cardBody}>
                                <div css={s.priceSection}>
                                    <span css={s.priceText}>{plan.price}</span>
                                    <div css={s.divider}></div>
                                </div>
                                <ul css={s.featureList}>
                                    {
                                        plan.features.map((feature, index) => (
                                            <li key={index}>
                                                <FaCheck size={12} color="#FF8A3D" style={{marginRight: '8px'}}/>
                                                {feature}
                                            </li>
                                        ))
                                    }
                                </ul>

                                { 
                                    isCurrent ? (
                                        <button css={s.currentPlanBtn} disabled>이용 중</button>
                                    ) : isDisabled ? (
                                        <div style={{height: "45px"}}></div>
                                    ) : isSelected ? (
                                        <button css={s.selectedBtn}>선택됨</button>
                                    ) : (
                                        <div style={{height: "45px"}}></div>
                                    )
                                }

                            </div>
                        </div>
                    )
                })}
            </div>
            <div css={s.footSection}>
                <button css={s.paymentBtn} 
                    onClick={handlePaymentClick}
                    disabled={currentSubscription === selectedPlan && selectedPlan !== "BASIC"}
                    style={{ 
                        opacity: (currentSubscription === selectedPlan && selectedPlan !== "BASIC") ? 0.5 : 1,
                        cursor: (currentSubscription === selectedPlan && selectedPlan !== "BASIC") ? 'not-allowed' : 'pointer'
                    }}
                >
                    {
                        (currentSubscription === selectedPlan && selectedPlan !== "BASIC")? "이용 중인 플랜입니다.." : "결페 페이지로 이동"
                    }
                </button>
            </div>
        </div>
    </>
}

export default Payments;