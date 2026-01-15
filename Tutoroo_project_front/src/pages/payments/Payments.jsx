/** @jsxImportSource @emotion/react */
import { useState } from "react";
import * as s  from "./styles";
import Header from "../../components/layouts/Header";

function Payments() {

    const [ selectedPlan, setSelectedPlan ] = useState("STANDARD");

    const plans = [
        {
            id: "BASIC",
            title: "BASIC",
            price: "무료",
            headerColor: "white", // 헤더 배경색
            features: ["구독 AI 튜터 무료 체험"],
            
        },
        {
            id: "STANDARD",
            title: "STANDARD",
            price: "₩9,900 / 월",
            headerColor: "#FFE0B2", // 연한 주황
            features: ["AI 튜터 무제한", "개인 학습 플랜", "진도 분석 리포트"],
            
        },
        {
            id: "PREMIUM",
            title: "PREMIUM",
            price: "₩29,900 / 월",
            headerColor: "#E1BEE7", // 연한 보라
            features: ["AI 풀 기능 사용", "개인 맞춤형 코칭", "주간 상세 리포트"],
            
        }
    ]

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
                            css={[s.card, isSelected && s.selectedCard]} 
                            onClick={() => setSelectedPlan(plan.id)} ></div>
                    )
                })}
            </div>
        </div>
    </>
}

export default Payments;