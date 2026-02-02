import { css } from "@emotion/react";

// ===========================================
// [기존 스타일 유지] - 절대 수정하지 않음
// ===========================================
export const wrapper = css`
    display: flex;
    width: 100%;
    height: calc(100vh - 60px);
    align-items: center;
    justify-content: center;
`;

export const contentBox = css`
    display: flex;
    flex-direction: row;
    align-items: flex-start;
    gap: 50px;
`;

export const mainContainer = css`
    width: 1200px;
    height: 80vh;
    background-color: #dbdbdb;
    border-radius: 20px;
    
    /* [스타일 보강] 내부 컨텐츠 정렬을 위해 flex 속성만 살짝 추가했습니다. */
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 30px;
    box-sizing: border-box;
`;

export const btn = css`
    width: 75px;
    height: 25px;
    padding: 12px 20px;
    border: none;
    border-radius: 12px;
    background-color: #ffe6d5;
    color: #e67025;
    font-weight: 700;
    border: none;
    cursor: pointer;
    font-size: 16px;
    transition: background 0.2s;
    white-space: nowrap;

    &:hover {
        background-color: #e67025;
        color: white; /* 호버 시 글자색 변경 추가 */
    }
`;

// ===========================================
// [추가된 스타일] - MainContainer 내부 UI용
// ===========================================

// 게임/입양 화면 공통 컨테이너 (흰색 카드 역할)
export const innerGameArea = css`
    width: 100%;
    height: 100%;
    background-color: #ffffff;
    border-radius: 16px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: space-between;
    padding: 40px;
    box-sizing: border-box;
    box-shadow: 0 4px 15px rgba(0,0,0,0.05);
`;

// [입양 화면] 리스트 그리드
export const adoptionList = css`
    display: flex;
    gap: 25px;
    justify-content: center;
    flex-wrap: wrap;
    width: 100%;
`;

// [입양 화면] 개별 카드
export const adoptionCard = css`
    width: 240px;
    padding: 30px 20px;
    border: 2px solid #f0f0f0;
    border-radius: 20px;
    text-align: center;
    cursor: pointer;
    background-color: white;
    transition: all 0.3s ease;

    &:hover {
        border-color: #e67025;
        transform: translateY(-5px);
        box-shadow: 0 10px 20px rgba(230, 112, 37, 0.15);
    }
`;

// [게임 화면] 레벨 뱃지
export const levelBadge = css`
    font-size: 16px;
    background-color: #ffe6d5;
    color: #e67025;
    padding: 4px 10px;
    border-radius: 12px;
    margin-left: 10px;
    vertical-align: middle;
`;

// [게임 화면] 말풍선 메시지
export const statusMsg = css`
    margin-top: 15px;
    color: #555;
    background-color: #f8f9fa;
    padding: 10px 20px;
    border-radius: 20px;
    border: 1px solid #eee;
    font-size: 16px;
    display: inline-block;
`;

// [게임 화면] 펫 이미지 영역 (중앙)
export const petImageArea = css`
    flex: 1;
    display: flex;
    width: 100%;
    align-items: center;
    justify-content: center;
    position: relative;
    overflow: visible;
`;

// [게임 화면] 자는 표시 (ZZZ 애니메이션)
export const zzzText = css`
    position: absolute;
    top: 10%;
    right: 35%;
    font-size: 40px;
    font-weight: 800;
    color: #5c6bc0;
    z-index: 10;
    animation: float 2.5s infinite ease-in-out;
    
    @keyframes float {
        0% { transform: translateY(0px) rotate(0deg); opacity: 0.6; }
        50% { transform: translateY(-20px) rotate(10deg); opacity: 1; }
        100% { transform: translateY(-40px) rotate(0deg); opacity: 0; }
    }
`;

// [게임 화면] 하단 컨트롤 패널
export const controlPanel = css`
    width: 80%;
    background-color: #fafafa;
    border-radius: 24px;
    padding: 30px;
    border: 1px solid #f0f0f0;
    display: flex;
    flex-direction: column;
    gap: 25px;
`;

// [게임 화면] 스탯 그리드 (2열)
export const statsGrid = css`
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 15px 50px;
`;

// [게임 화면] 버튼 그룹
export const btnGroup = css`
    display: flex;
    justify-content: center;
    gap: 15px;
`;

// [게임 화면] 일반 액션 버튼
export const gameBtn = css`
    flex: 1;
    height: 60px;
    border: 2px solid #f0f0f0;
    border-radius: 16px;
    background-color: white;
    color: #444;
    font-size: 15px;
    font-weight: 700;
    cursor: pointer;
    transition: all 0.2s;
    
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 2px;

    &:hover {
        border-color: #e67025;
        background-color: #fffaf5;
        color: #e67025;
        transform: translateY(-2px);
    }
    
    &:active {
        transform: scale(0.98);
    }
`;

// [게임 화면] 깨우기 전용 버튼 (강조)
export const wakeBtn = css`
    width: 100%;
    height: 60px;
    background-color: #5c6bc0;
    color: white;
    font-size: 18px;
    font-weight: 700;
    border: none;
    border-radius: 16px;
    cursor: pointer;
    box-shadow: 0 4px 10px rgba(92, 107, 192, 0.3);
    transition: background 0.2s;

    &:hover {
        background-color: #4d59a1;
    }
`;

export const btnArea = css`
    display: flex;
   
    flex-direction: column;
    gap: 10px;         
    margin-top: 20px;
    align-items: center;
`

// ★ 일기장 전용 버튼 스타일
export const diaryBtn = css`
    width: 100%;               
    height: 45px;
    padding: 0 20px;
    border: none;
    border-radius: 12px;
    background-color: #ede7f6; 
    color: #5e35b1;            
    font-weight: 700;
    cursor: pointer;
    font-size: 15px;
    transition: all 0.2s ease;
    
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;                 
    box-shadow: 0 2px 8px rgba(94, 53, 177, 0.1);

    &:hover {
        background-color: #5e35b1; 
        color: white;             
        transform: translateY(-3px);
        box-shadow: 0 5px 15px rgba(94, 53, 177, 0.3);
    }

    &:active {
        transform: scale(0.97);
    }
`;

//--------------일기장 모달---------------------------------------

export const modalOverlay = css`
    position: fixed;
    top: 0; left: 0; right: 0; bottom: 0;
    background-color: rgba(0, 0, 0, 0.6);
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 1000;
`;

// 2. 일기장 박스 (하얀 종이)
export const diaryModalBox = css`
    width: 350px;
    height: 500px;
    background-color: #fff;
    border-radius: 20px;
    padding: 20px;
    display: flex;
    flex-direction: column;
    box-shadow: 0 10px 25px rgba(0,0,0,0.3);
    animation: fadeIn 0.3s ease;

    @keyframes fadeIn {
        from { opacity: 0; transform: translateY(20px); }
        to { opacity: 1; transform: translateY(0); }
    }
`;

// 3. 일기 리스트 영역 (스크롤 가능)
export const diaryListArea = css`
    flex: 1;
    overflow-y: auto;
    margin-top: 10px;
    padding-right: 5px;

    /* 스크롤바 꾸미기 */
    &::-webkit-scrollbar { width: 6px; }
    &::-webkit-scrollbar-thumb { background-color: #ddd; border-radius: 3px; }
`;

// 4. 개별 일기 카드
export const diaryCard = css`
    background-color: #f8f9fa;
    border: 1px solid #eee;
    border-radius: 10px;
    padding: 15px;
    margin-bottom: 15px;

    h3 { margin: 0 0 5px 0; font-size: 16px; color: #333; }
    p { margin: 0; font-size: 14px; color: #555; line-height: 1.5; white-space: pre-wrap; }
    span { font-size: 12px; color: #888; display: block; margin-bottom: 8px;}
`;