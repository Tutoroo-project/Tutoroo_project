/** @jsxImportSource @emotion/react */
import { useState, useEffect, useCallback } from "react";
import Header from "../../components/layouts/Header";
import * as s from "./styles";
import { adoptPet, getAdoptablePets, getPetStatus, interactWithPet } from "../../apis/pet/petApi";

import { ANIMATIONS } from "./petAnimations";
import { PET_IMAGES } from "../../constants/petImages";
import SpriteChar from "./SpriteChar";


function Pet() {

  const [loading, setLoading] = useState(true);
  const [petStatus, setPetStatus] = useState(null);
  const [isNoPet, setIsNoPet] = useState(false);
  const [adoptableList, setAdoptableList] = useState([]);
  const [actionStatus, setActionStatus ] = useState(null);

  const [ frameIndex, setFrameIndex ]  = useState(0); //프레임 번호

  const getRenderInfo = () => {
    if (!petStatus || petStatus.stage <= 1) {
      return { src: PET_IMAGES.Egg.DEFAULT, sequence: ANIMATIONS.ROW1 };
    }
    

    const type = petStatus.petType || "Fox";
    const images = PET_IMAGES[type] || PET_IMAGES.Fox;

    if (actionStatus === "EATING") {
        return { src: images.PART2, sequence: ANIMATIONS.ROW1 , isEgg: true};
    }

    // if (actionStatus === "PLAYING") {
    //     return { src: images.PART2, sequence: ANIMATIONS.ROW2 }; // 주석상 ROW2가 '사랑'이라면 여기로 연결
    // }

    //  씻는 중 
    if (actionStatus === "CLEANING") {
        return { src: images.PART2, sequence: ANIMATIONS.ROW2 };
    }

    //  자는 중 
    if (petStatus.isSleeping) {
        return { src: images.PART1, sequence: ANIMATIONS.ROW1 };
    }

    // 배고픔(슬픔) 
    if (petStatus.fullness < 30) {
        return { src: images.PART2, sequence: ANIMATIONS.ROW3 };
    }

    // 기분 좋음
    if (petStatus.intimacy >= 80) {
        return { src: images.PART2, sequence: ANIMATIONS.ROW2 };
    }
    
    // [기본] 평상시
    return { src: images.PART2, sequence: ANIMATIONS.ROW1 };
  };

  // 위 함수를 실행해서 현재 보여줄 정보를 뽑아냅니다.
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
      if (status) {
        setPetStatus(status);
        setIsNoPet(false);
      } else {
        setIsNoPet("ADOPT"); // [수정] "ADOPT" 문자열로 통일
        setPetStatus(null);
        const listResponse = await getAdoptablePets();
        setAdoptableList(listResponse.availablePets || []);
      }
    } catch (error) {
      console.error("데이터 로딩 실패: ", error);
      // alert("데이터를 불러오는 중 문제가 발생했습니다."); // 귀찮으면 주석
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData])

  const handleAdopt = async (petType) => { 
    if (!window.confirm("이 친구로 입양하시겠습니까?")) return;
    try {
      await adoptPet(petType);
      alert("입양 성공! 새로운 친구가 생겼어요.");
      fetchData();
    } catch (error) {
      console.error(error);
      alert("입양 중 오류가 발생했습니다.")
    }
  };

  const handleHatch = async (petType) => {
    if (!window.confirm("이 알을 부화시키시겠습니까?")) return;
    try {
        await hatchEgg(petType);
        alert("알이 부화했습니다! 🐣 새로운 여정을 시작하세요.");
        fetchData(); // 상태 갱신 -> PET 모드로 변경됨
    } catch (error) {
        alert("부화에 실패했습니다.");
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

      if (error.response && error.response.data && error.response.data.data.message) {
        alert(error.response.data.message);
      } else {
        alert ("적용 실패!!")
      }
    }
  };

  // [New] 배경 이미지 결정
  const getBackgroundImage = () => {
    // 나중에 레벨이나 펫 종류에 따라 배경을 바꿀 수 있음
    return "url('/assets/backgrounds/room_default.png')";
  };


 return (
    <>
      <Header />
      <div css={s.wrapper}>
        <div css={s.contentBox}>
          <div css={s.mainContainer}>
            {loading && <div>로딩 중...</div>}

            {/* [유지] 입양 화면 */}
            {!loading && isNoPet === "ADOPT" && (
              <div css={s.innerGameArea}>
                <div style={{ textAlign: "center", marginBottom: "30px" }}>
                  <h2 style={{ fontSize: "28px", color: "#333", marginBottom: "10px" }}>
                    새로운 파트너를 선택해주세요 🐾
                  </h2>
                </div>

                <div css={s.adoptionList}>
                  {adoptableList.map((pet) => (
                    <div key={pet.type} css={s.adoptionCard} onClick={() => handleAdopt(pet.type)}>
                      
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
                  
                  {/* SpriteChar 연결: getRenderInfo에서 받은 src, sequence 사용 */}
                  <SpriteChar 
                    src={src} 
                    index={sequence[frameIndex]} 
                    size={280} 
                  />
                </div>

                {/* [유지] 컨트롤 패널 */}
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
                        <button css={s.gameBtn} onClick={() => handleInteract("FEED")}>
                          🍖 밥주기
                        </button>
                        <button css={s.gameBtn} onClick={() => handleInteract("PLAY")}>
                          ⚽ 놀아주기
                        </button>
                        <button css={s.gameBtn} onClick={() => handleInteract("CLEAN")}>
                          ✨ 씻겨주기
                        </button>
                        <button css={s.gameBtn} onClick={() => handleInteract("SLEEP")}>
                          💤 재우기
                        </button>
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

// [유지] StatBar 컴포넌트
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