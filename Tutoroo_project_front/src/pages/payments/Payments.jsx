/** @jsxImportSource @emotion/react */
import { useState } from "react";
import * as s  from "./styles";
import Header from "../../components/layouts/Header";
import { FaCheck, FaCheckCircle } from "react-icons/fa";
import { useNavigate } from "react-router-dom";

function Payments() {
    const navigate = useNavigate();
    const [ selectedPlan, setSelectedPlan ] = useState("STANDARD");

    const plans = [
        {
            id: "BASIC",
            title: "BASIC",
            price: "무료",
            headerColor: "#ffffff", 
            features: ["구독 AI 튜터 무료 체험"],
            badge: "무료제공"
            
        },
        {
            id: "STANDARD",
            title: "STANDARD",
            price: "₩9,900 / 월",
            headerColor: "#fde8c8", 
            features: ["AI 튜터 무제한", "개인 학습 플랜", "진도 분석 리포트"],
            badge: null
            
        },
        {
            id: "PREMIUM",
            title: "PREMIUM",
            price: "₩29,900 / 월",
            headerColor: "#f3d8f8",
            features: ["AI 풀 기능 사용", "개인 맞춤형 코칭", "주간 상세 리포트"],
            badge: null
            
        }
    ]

    const handlePaymentClick = () => {
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

                    return (
                        <div 
                            key={plan.id} 
                            css={
                                [
                                   s.card, isSelected && s.selectedCard
                                ]} 
                            onClick={() => setSelectedPlan(plan.id)} >
                            {isSelected && <div css={s.checkBadge}><FaCheckCircle /></div>}

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
                                    isSelected ? (
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
                <button css={s.paymentBtn} onClick={handlePaymentClick}>결제 페이지로 이동</button>
            </div>
        </div>
    </>
}

export default Payments;