/** @jsxImportSource @emotion/react */
import { useState, useEffect, useCallback } from "react";
import Header from "../../components/layouts/Header";
import * as s from "./styles";
import { adoptPet, 
  getAdoptablePets, 
  getPetStatus, 
  interactWithPet, 
  getGraduationEggs, 
  hatchEgg, 
  getMyDiaries,
  testWriteDiary } from "../../apis/pet/petApi";

import { ANIMATIONS } from "./petAnimations";
import { PET_IMAGES } from "../../constants/petImages";
import SpriteChar from "./SpriteChar";

function Pet() {

  const [diaries, setDiaries] = useState([]);      
  const [isDiaryOpen, setIsDiaryOpen] = useState(false);

  const [loading, setLoading] = useState(true);
  const [petStatus, setPetStatus] = useState(null);
  const [isNoPet, setIsNoPet] = useState(false);
  
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
      
      if (status && status.petId && status.status !== "RUNAWAY") { 
        console.log("내 펫 정보 발견:", status);
        setPetStatus(status);
        setIsNoPet(false);
      } else {
        setPetStatus(null);
        try {
            const eggResponse = await getGraduationEggs();
            const pureEggs = eggResponse.candidates.filter(egg => egg.type !== "CUSTOM_EGG");
            
            if (pureEggs.length > 0) {
                setIsNoPet("SELECT_EGG_GRADUATED"); 
                setEggList(pureEggs);
                setLoading(false);
                return;
            }
        } catch (e) {
        }
        const initResponse = await getAdoptablePets();
        setIsNoPet("SELECT_EGG_NEW");
        setEggList(initResponse.availablePets || []);
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


  const handleEggSelect = async (pet) => { 
   const inputName = window.prompt(`"${pet.name}"의 이름을 지어주세요!`, pet.name);

    if (inputName === null) return;
    if (inputName.trim() === "") {
        alert("이름을 한 글자 이상 입력해주세요!");
        return;
    }

    try {
      if (isNoPet === "SELECT_EGG_GRADUATED") {
        // [중요] 여기에 petName이 꼭 들어가야 합니다!
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


  const handleOpenDiary = async () => {
        try {
            const data = await getMyDiaries();
            setDiaries(data);
            setIsDiaryOpen(true);
        } catch (error) {
            alert("일기장을 불러오지 못했어요 ㅠㅠ");
        }
    };


 return (
    <>
      <Header />
      <div css={s.wrapper}>
        <div css={s.contentBox}>
          
          {/* 1. 게임 화면 영역 */}
          <div css={s.mainContainer}>
            {loading && <div>로딩 중...</div>}

            {!loading && (isNoPet === "SELECT_EGG_NEW" || isNoPet === "SELECT_EGG_GRADUATED") && (
              <div css={s.innerGameArea}>
                <div style={{ textAlign: "center", marginBottom: "30px" }}>
                  <h2 style={{ fontSize: "28px", color: "#333", marginBottom: "10px" }}>
                    운명의 알을 선택해주세요 🥚
                  </h2>
                  <p style={{color: "#666"}}>당신의 사랑으로 태어날 친구입니다.</p>
                </div>

                <div css={s.adoptionList}>
                  {eggList.map((pet) => (
                    <div 
                        key={pet.type} 
                        css={s.adoptionCard} 
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

          {/* 2. 버튼 영역 (게임화면 아래에 위치) */}
          <div css={s.btnArea}>            
            {/* 상점 버튼 */}
            <button css={s.btn} style={{width: '200px', height: '50px', display:'flex', alignItems:'center', justifyContent:'center', gap:'8px'}}>
                👜 상점 가기
            </button>
            
            {/* 일기장 버튼 (위아래 배치) */}
            <button css={s.diaryBtn} onClick={handleOpenDiary} style={{width: '200px', height: '50px', border: 'none'}}>
              📖 비밀 일기장
            </button>
          </div>

   
          {isDiaryOpen && (
            <div css={s.modalOverlay} onClick={() => setIsDiaryOpen(false)}>
                <div css={s.diaryModalBox} onClick={(e) => e.stopPropagation()}>
                    <div style={{display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:'10px'}}>
                        <h2 style={{margin:0, fontSize:'20px'}}>📖 펫의 비밀일기</h2>
                        <button onClick={() => setIsDiaryOpen(false)} style={{background:'none', border:'none', fontSize:'20px', cursor:'pointer'}}>❌</button>
                    </div>
                    
                    <div css={s.diaryListArea}>
                        {diaries.length === 0 ? (
                            <p style={{textAlign:'center', color:'#999', marginTop:'50px'}}>아직 쓰여진 일기가 없어...<br/>오늘 밤을 기다려봐! 🌙</p>
                        ) : (
                            diaries.map((diary, index) => (
                                <div key={diary.diaryId || index} css={s.diaryCard}>
                                    <span>📅 {diary.date} | 기분: {diary.mood === 'HAPPY' ? '🥰' : '😢'}</span>
                                    <p>{diary.content}</p>
                                </div>
                            ))
                        )}
                    </div>
                </div>
            </div>
          )}

        </div> {/* contentBox 끝 */}
      </div> {/* wrapper 끝 */}
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