/** @jsxImportSource @emotion/react */
import { useState, useEffect, useCallback } from "react";
import Header from "../../components/layouts/Header";
import * as s from "./styles";
import { adoptPet, getAdoptablePets, getPetStatus, interactWithPet, getGraduationEggs, hatchEgg } from "../../apis/pet/petApi";

import { ANIMATIONS } from "./petAnimations";
import { PET_IMAGES } from "../../constants/petImages";
import SpriteChar from "./SpriteChar";

function Pet() {

  const [loading, setLoading] = useState(true);
  const [petStatus, setPetStatus] = useState(null);
  const [isNoPet, setIsNoPet] = useState(false);
  
  // [수정됨 1] 변수명을 eggList로 통일 (기존 adoptableList 대체)
  const [eggList, setEggList] = useState([]); 
  
  const [actionStatus, setActionStatus ] = useState(null);
  const [frameIndex, setFrameIndex ]  = useState(0); 

  const getRenderInfo = () => {
    if (!petStatus || petStatus.stage <= 1) {
      return { src: PET_IMAGES.Egg.DEFAULT, sequence: ANIMATIONS.ROW1 };
    }

    const type = petStatus.petType || "Fox";
    const images = PET_IMAGES[type] || PET_IMAGES.Fox;

    if (actionStatus === "EATING") return { src: images.PART2, sequence: ANIMATIONS.ROW1 , isEgg: true};
    if (actionStatus === "CLEANING") return { src: images.PART2, sequence: ANIMATIONS.ROW2 };
    if (petStatus.isSleeping) return { src: images.PART1, sequence: ANIMATIONS.ROW1 };
    if (petStatus.fullness < 30) return { src: images.PART2, sequence: ANIMATIONS.ROW3 };
    if (petStatus.intimacy >= 80) return { src: images.PART2, sequence: ANIMATIONS.ROW2 };
    
    return { src: images.PART2, sequence: ANIMATIONS.ROW1 };
  };

  const { src, sequence } = getRenderInfo();

  useEffect(() => {
      const timer = setInterval(() => {
          setFrameIndex((prev) => (prev + 1)  % sequence.length);
      }, 500);
      return () => clearInterval(timer);
  }, [sequence]);
  
 
 const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const status = await getPetStatus();
      
      if (status && status.petId) { 
        console.log("내 펫 정보 발견:", status); // 콘솔에서 확인용
        setPetStatus(status);
        setIsNoPet(false);
      } else {
        // 펫 정보가 없거나 이상하면 없는 것으로 간주
        setPetStatus(null);
        
        // 1. 졸업 후 알 후보(Eggs)가 있는지 먼저 확인
        try {
            const eggResponse = await getGraduationEggs();
            // 커스텀 알 제외
            const pureEggs = eggResponse.candidates.filter(egg => egg.type !== "CUSTOM_EGG");
            
            if (pureEggs.length > 0) {
                setIsNoPet("SELECT_EGG_GRADUATED"); 
                setEggList(pureEggs); // [수정됨] 이제 에러 안 남
                setLoading(false);
                return;
            }
        } catch (e) {
            // 졸업 알 없으면 패스
        }

        // 2. 초기 유저용 알 리스트
        const initResponse = await getAdoptablePets();
        setIsNoPet("SELECT_EGG_NEW");
        setEggList(initResponse.availablePets || []); // [수정됨] 이제 에러 안 남
      }
    } catch (error) {
      console.error("데이터 로딩 실패: ", error);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);


  // [중요] 알 선택 통합 핸들러
  const handleEggSelect = async (pet) => { 
   const inputName = window.prompt(`"${pet.name}"의 이름을 지어주세요!`, pet.name);

    // 취소 버튼을 눌렀으면 아무 일도 안 하고 종료
    if (inputName === null) return;

    // 이름이 비어있으면 경고
    if (inputName.trim() === "") {
        alert("이름을 한 글자 이상 입력해주세요!");
        return;
    }

    try {
      if (isNoPet === "SELECT_EGG_GRADUATED") {
        await hatchEgg(pet.type, inputName); 
      } else {
        await adoptPet(pet.type, inputName); 
      }
      
      alert("알을 따뜻하게 품기 시작했습니다! 🥚");
      fetchData(); 
    } catch (error) {
      console.error(error);
      alert("알 선택 중 문제가 발생했습니다.");
    }
  };

  const handleInteract = async (actionType) => {
    try {
      const updateStatus = await interactWithPet(actionType);
      setPetStatus(updateStatus);

     if (actionType === "FEED") {
          setActionStatus("EATING"); 
          setTimeout(() => setActionStatus(null), 2000); 
      } else if (actionType === "CLEAN") {
          setActionStatus("CLEANING"); 
          setTimeout(() => setActionStatus(null), 2000);
      }
    } catch (error) {
      console.log(error);
      if (error.response?.data?.message) {
        alert(error.response.data.message);
      } else {
        alert ("적용 실패!!");
      }
    }
  };

  const getBackgroundImage = () => {
    return "url('/assets/backgrounds/room_default.png')";
  };


 return (
    <>
      <Header />
      <div css={s.wrapper}>
        <div css={s.contentBox}>
          <div css={s.mainContainer}>
            {loading && <div>로딩 중...</div>}

            {/* [수정됨 2] 조건문을 isNoPet 상태에 맞게 변경 */}
            {!loading && (isNoPet === "SELECT_EGG_NEW" || isNoPet === "SELECT_EGG_GRADUATED") && (
              <div css={s.innerGameArea}>
                <div style={{ textAlign: "center", marginBottom: "30px" }}>
                  {/* 문구도 알 선택에 맞게 변경 */}
                  <h2 style={{ fontSize: "28px", color: "#333", marginBottom: "10px" }}>
                    운명의 알을 선택해주세요 🥚
                  </h2>
                  <p style={{color: "#666"}}>당신의 사랑으로 태어날 친구입니다.</p>
                </div>

                <div css={s.adoptionList}>
                  {/* [수정됨] eggList 사용 */}
                  {eggList.map((pet) => (
                    <div 
                        key={pet.type} 
                        css={s.adoptionCard} 
                        // [수정됨 3] 핸들러를 handleEggSelect로 교체
                        onClick={() => handleEggSelect(pet)}
                    >
                      <img
                        src={PET_IMAGES.Egg.DEFAULT} 
                        alt={pet.name}
                        style={{ width: "100px", height: "100px", objectFit: "contain", marginBottom: "15px" }}
                      />
                      <h3 style={{ margin: "0 0 10px 0", color: "#e67025" }}>{pet.name}</h3>
                      <p style={{ fontSize: "13px", color: "#666" }}>{pet.description}</p>
                    </div>
                  ))}
                </div>
              </div>
            )}

            
            {!loading && !isNoPet && petStatus && (
              <div
                css={s.innerGameArea}
                style={{ backgroundImage: getBackgroundImage(), backgroundSize: "cover" }}
              >
                <div style={{ textAlign: "center", zIndex: 2 }}>
                  <h2 style={{ margin: 0, fontSize: "28px", color: "#333" }}>
                    {petStatus.petName} <span css={s.levelBadge}>Lv.{petStatus.stage}</span>
                  </h2>
                  <div css={s.statusMsg}>"{petStatus.statusMessage}"</div>
                </div>

                <div css={s.petImageArea}>
                  {petStatus.isSleeping && <div css={s.zzzText}>ZZZ...</div>}
                  <SpriteChar 
                    src={src} 
                    index={sequence[frameIndex]} 
                    size={280} 
                  />
                </div>

                <div css={s.controlPanel} style={{ backgroundColor: "rgba(255, 255, 255, 0.9)" }}>
                  <div css={s.statsGrid}>
                    <StatBar label="배고픔" value={petStatus.fullness} color="#FF9800" />
                    <StatBar label="친밀도" value={petStatus.intimacy} color="#E91E63" />
                    <StatBar label="청결도" value={petStatus.cleanliness} color="#2196F3" />
                    <StatBar label="에너지" value={petStatus.energy} color="#4CAF50" />
                  </div>
                  <div css={s.btnGroup}>
                    {petStatus.isSleeping ? (
                      <button css={s.wakeBtn} onClick={() => handleInteract("WAKE_UP")}>
                        ⏰ 흔들어 깨우기
                      </button>
                    ) : (
                      <>
                        <button css={s.gameBtn} onClick={() => handleInteract("FEED")}>🍖 밥주기</button>
                        <button css={s.gameBtn} onClick={() => handleInteract("PLAY")}>⚽ 놀아주기</button>
                        <button css={s.gameBtn} onClick={() => handleInteract("CLEAN")}>✨ 씻겨주기</button>
                        <button css={s.gameBtn} onClick={() => handleInteract("SLEEP")}>💤 재우기</button>
                      </>
                    )}
                  </div>
                </div>
              </div>
            )}
          </div>
          <button css={s.btn}>👜 상점</button>
        </div>
      </div>
    </>
  );
}

const StatBar = ({ label, value, color }) => (
  <div style={{ display: "flex", alignItems: "center", gap: "10px", fontSize: "14px", fontWeight: "bold", color: "#555" }}>
    <span style={{ width: "50px" }}>{label}</span>
    <div style={{ flex: 1, height: "10px", background: "#eee", borderRadius: "5px", overflow: "hidden" }}>
      <div style={{ width: `${Math.min(100, value)}%`, height: "100%", background: color, transition: "width 0.5s" }} />
    </div>
    <span style={{ width: "30px", textAlign: "right" }}>{value}</span>
  </div>
);

export default Pet;