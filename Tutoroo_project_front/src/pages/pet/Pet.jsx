/** @jsxImportSource @emotion/react */
import { useState, useEffect, useCallback } from "react";
import Header from "../../components/layouts/Header";
import * as s from "./styles";
import { api } from "../../apis/configs/axiosConfig";
import { adoptPet, getAdoptablePets, getPetStatus, interactWithPet } from "../../apis/pet/petApi";

function Pet() {
  // ... (기존 State, API 로직 그대로 유지) ...
  // ... (handleAdopt, handleInteract 등 기존 로직 그대로 유지) ...

  const [loading, setLoading] = useState(true);
  const [errorMsg, setErrorMsg] = useState(null);
  const [petStatus, setPetStatus] = useState(null);
  const [isNoPet, setIsNoPet] = useState(false);
  const [adoptableList, setAdoptableList] = useState([]);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const status = await getPetStatus();

      if (status) {
        setPetStatus(status);
        setIsNoPet(false);
      } else {
        setIsNoPet(true);
        setPetStatus(null);

        const listResponse = await getAdoptablePets();
        setAdoptableList(listResponse.availablePets || []);
      }
    } catch (error) {
      console.error("데이터 로딩 실패: ", error) ;
      alert("데이터를 불러오는 중 문제가 발생했습니다.");
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

  const handleInteract = async (actionType) => {
    try {
      const updateStatus = await interactWithPet(actionType);
      setPetStatus(updateStatus);
    } catch (error) {
      console.log(error);

      if (error.response && error.response.data && error.response.data.data.message) {
        alert(error.response.data.message);
      } else {
        alert ("적용 실패!!")
      }
    }
  };

  

  // (fetchStatus, handleAdopt, handleInteract 등 위쪽 코드는 기존과 동일하므로 생략)
  // 아래 헬퍼 함수부터 수정합니다.

  // ----------------------------------------------------------------
  // [수정] 3. 이미지 경로 생성 헬퍼 (Naming Rule 적용)
  // ----------------------------------------------------------------
  const getPetImage = (pet) => {
    if (!pet) return "";

    // 1. 커스텀 펫이면 URL 그대로 사용
    if (pet.customImageUrl) return pet.customImageUrl;

    // 2. 상태 결정 (자는 중 우선 > 기분 좋음(예시) > 기본)
    // 백엔드에 'isHappy' 같은 필드가 없으므로, 현재는 SLEEP과 IDLE만 구분
    // 나중에 intimacy가 높으면 HAPPY 이미지를 쓰도록 로직 추가 가능
    let state = "IDLE";
    if (pet.isSleeping) {
      state = "SLEEP";
    } else if (pet.intimacy >= 80) {
      // 친밀도 80 이상이면 HAPPY 이미지 사용 (이미지 있으면)
      state = "IDLE"; // 일단은 IDLE로 통일 (이미지 준비되면 HAPPY로 변경)
    }

    // 3. 경로 반환 (public/assets/pets/{TYPE}_{STAGE}_{STATE}.png)
    // 예: /assets/pets/TIGER_1_IDLE.png
    return `/assets/pets/${pet.petType}_${pet.stage}_${state}.png`;
  };

  // [New] 배경 이미지 결정
  const getBackgroundImage = () => {
    // 나중에 레벨이나 펫 종류에 따라 배경을 바꿀 수 있음
    return "url('/assets/backgrounds/room_default.png')";
  };

  // ----------------------------------------------------------------
  // 4. 화면 렌더링
  // ----------------------------------------------------------------
  return (
    <>
      <Header />
      <div css={s.wrapper}>
        <div css={s.contentBox}>
          <div css={s.mainContainer}>
            {loading && <div>로딩 중...</div>}

            {/* Case A: 펫 없음 (입양) */}
            {!loading && isNoPet && (
              <div css={s.innerGameArea}>
                <div style={{ textAlign: "center", marginBottom: "30px" }}>
                  <h2
                    style={{
                      fontSize: "28px",
                      color: "#333",
                      marginBottom: "10px",
                    }}
                  >
                    새로운 파트너를 선택해주세요 🐾
                  </h2>
                  <p style={{ color: "#888" }}>
                    함께 공부하며 성장할 친구입니다.
                  </p>
                </div>

                <div css={s.adoptionList}>
                  {adoptableList.map((pet) => (
                    <div
                      key={pet.type}
                      css={s.adoptionCard}
                      onClick={() => handleAdopt(pet.type)}
                    >
                      {/* 입양 리스트의 대표 이미지 (1단계 기본) */}
                      <img
                        src={`/assets/pets/${pet.type}_1_IDLE.png`}
                        alt={pet.name}
                        onError={(e) => {
                          e.target.onerror = null;
                          e.target.src = `https://via.placeholder.com/150?text=${pet.type}`; // 이미지 없을 때 대비
                        }}
                        style={{
                          width: "120px",
                          height: "120px",
                          objectFit: "contain",
                          marginBottom: "15px",
                        }}
                      />
                      <h3 style={{ margin: "0 0 10px 0", color: "#e67025" }}>
                        {pet.name}
                      </h3>
                      <p
                        style={{
                          fontSize: "13px",
                          color: "#666",
                          lineHeight: "1.4",
                        }}
                      >
                        {pet.description}
                      </p>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Case B: 펫 있음 (육성) */}
            {!loading && !isNoPet && petStatus && (
              <div
                css={s.innerGameArea}
                style={{
                  backgroundImage: getBackgroundImage(),
                  backgroundSize: "cover",
                }}
              >
                {/* 상단 정보 */}
                <div style={{ textAlign: "center", zIndex: 2 }}>
                  <h2
                    style={{
                      margin: 0,
                      fontSize: "28px",
                      color: "#333",
                      textShadow: "2px 2px 0px #fff",
                    }}
                  >
                    {petStatus.petName}
                    <span css={s.levelBadge}>Lv.{petStatus.stage}</span>
                  </h2>
                  <div css={s.statusMsg}>"{petStatus.statusMessage}"</div>
                </div>

                {/* 펫 이미지 영역 */}
                <div css={s.petImageArea}>
                  {petStatus.isSleeping && <div css={s.zzzText}>ZZZ...</div>}
                  <img
                    src={getPetImage(petStatus)}
                    alt="pet"
                    onError={(e) => {
                      e.target.onerror = null;
                      e.target.src = `https://via.placeholder.com/300?text=${petStatus.petType}_${petStatus.stage}`;
                    }}
                    style={{
                      height: "280px",
                      objectFit: "contain",
                      filter: petStatus.isSleeping ? "brightness(0.8)" : "none",
                      transition: "all 0.5s ease",
                      dropShadow: "0 10px 10px rgba(0,0,0,0.2)", // 그림자 효과 추가
                    }}
                  />
                </div>

                {/* 하단 컨트롤 패널 */}
                <div
                  css={s.controlPanel}
                  style={{ backgroundColor: "rgba(255, 255, 255, 0.9)" }}
                >
                  {" "}
                  {/* 배경 투명도 추가 */}
                  <div css={s.statsGrid}>
                    <StatBar
                      label="배고픔"
                      value={petStatus.fullness}
                      color="#FF9800"
                    />
                    <StatBar
                      label="친밀도"
                      value={petStatus.intimacy}
                      color="#E91E63"
                    />
                    <StatBar
                      label="청결도"
                      value={petStatus.cleanliness}
                      color="#2196F3"
                    />
                    <StatBar
                      label="에너지"
                      value={petStatus.energy}
                      color="#4CAF50"
                    />
                  </div>
                  <div css={s.btnGroup}>
                    {petStatus.isSleeping ? (
                      <button
                        css={s.wakeBtn}
                        onClick={() => handleInteract("WAKE_UP")}
                      >
                        ⏰ 흔들어 깨우기
                      </button>
                    ) : (
                      <>
                        <button
                          css={s.gameBtn}
                          onClick={() => handleInteract("FEED")}
                        >
                          🍖 밥주기{" "}
                          <span style={{ fontSize: "10px", display: "block" }}>
                            -50P
                          </span>
                        </button>
                        <button
                          css={s.gameBtn}
                          onClick={() => handleInteract("PLAY")}
                        >
                          ⚽ 놀아주기{" "}
                          <span style={{ fontSize: "10px", display: "block" }}>
                            -30P
                          </span>
                        </button>
                        <button
                          css={s.gameBtn}
                          onClick={() => handleInteract("CLEAN")}
                        >
                          ✨ 씻겨주기{" "}
                          <span style={{ fontSize: "10px", display: "block" }}>
                            -20P
                          </span>
                        </button>
                        <button
                          css={s.gameBtn}
                          onClick={() => handleInteract("SLEEP")}
                        >
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

// ... StatBar 컴포넌트와 export는 그대로 유지 ...
const StatBar = ({ label, value, color }) => (
  <div
    style={{
      display: "flex",
      alignItems: "center",
      gap: "10px",
      fontSize: "14px",
      fontWeight: "bold",
      color: "#555",
    }}
  >
    <span style={{ width: "50px" }}>{label}</span>
    <div
      style={{
        flex: 1,
        height: "10px",
        background: "#eee",
        borderRadius: "5px",
        overflow: "hidden",
      }}
    >
      <div
        style={{
          width: `${Math.min(100, value)}%`,
          height: "100%",
          background: color,
          transition: "width 0.5s",
        }}
      />
    </div>
    <span style={{ width: "30px", textAlign: "right" }}>{value}</span>
  </div>
);

export default Pet;
