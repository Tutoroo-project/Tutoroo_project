/** @jsxImportSource @emotion/react */
import { useState, useEffect, useCallback } from "react";
import Header from "../../components/layouts/Header"; // 경로 확인 필요
import * as s from "./styles";
import {
  adoptPet,
  getAdoptablePets,
  getPetStatus,
  interactWithPet,
} from "../../apis/pet/petApi";

function Pet() {
  const [loading, setLoading] = useState(true);
  const [petStatus, setPetStatus] = useState(null);
  const [isNoPet, setIsNoPet] = useState(false);
  const [adoptableList, setAdoptableList] = useState([]);

  // 1. 데이터 가져오기
  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const status = await getPetStatus();

      // 데이터가 있고, 필수 필드(petId 또는 petName)가 있는지 확인
      if (status && (status.petId || status.petName)) {
        console.log("동물 상태 적용:", status);
        setPetStatus(status);
        setIsNoPet(false);
      } else {
        console.log("동물 없음 상태로 전환");
        setIsNoPet(true);
        setPetStatus(null);

        // 입양 리스트 가져오기
        const listData = await getAdoptablePets();
        // 배열인지 확인 후 설정 (배열이 아니면 빈 배열)
        const list = Array.isArray(listData)
          ? listData
          : listData.availablePets || [];
        setAdoptableList(list);
      }
    } catch (error) {
      console.error("데이터 로딩 중 에러:", error);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  // 2. 입양 핸들러
  const handleAdopt = async (petType) => {
    if (!window.confirm("이 친구로 입양하시겠습니까?")) return;
    try {
      await adoptPet(petType);
      alert("입양 성공! 🎉");
      fetchData();
    } catch (error) {
      alert(
        "입양 실패: " + (error.response?.data?.message || "알 수 없는 오류"),
      );
    }
  };

  // 3. 상호작용 핸들러
  const handleInteract = async (actionType) => {
    try {
      const updateStatus = await interactWithPet(actionType);
      if (updateStatus) {
        setPetStatus(updateStatus);
      }
    } catch (error) {
      if (
        error.response &&
        error.response.data &&
        error.response.data.message
      ) {
        alert(error.response.data.message);
      } else {
        alert("행동 실패!");
      }
    }
  };

  // 4. 이미지 경로 생성 (대문자 파일명 매칭)
  const getPetImage = (pet) => {
    if (!pet) return "";
    if (pet.customImageUrl) return pet.customImageUrl;

    // petType을 대문자로 변환 (Tiger -> TIGER)
    const type = pet.petType ? pet.petType.toUpperCase() : "TIGER";
    // 상태: 자는 중(SLEEP) vs 깨어있음(IDLE)
    const state = pet.isSleeping ? "SLEEP" : "IDLE";

    // 경로: /assets/pets/TIGER_1_IDLE.png
    return `/assets/pets/${type}_${pet.stage}_${state}.png`;
  };

  // 5. 배경 이미지 (없으면 회색 배경)
  const getBackgroundImage = () => {
    return "url('/assets/backgrounds/room_default.png')";
  };

  return (
    <>
      <Header />
      <div css={s.wrapper}>
        <div css={s.contentBox}>
          <div css={s.mainContainer}>
            {loading && <div>데이터를 불러오는 중입니다...</div>}

            {/* CASE A: 펫 없음 (입양 화면) */}
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
                      key={pet.type || pet.petType}
                      css={s.adoptionCard}
                      onClick={() => handleAdopt(pet.type || pet.petType)}
                    >
                      <div style={{ fontSize: "50px", marginBottom: "10px" }}>
                        🥚
                      </div>
                      <h3 style={{ margin: "0 0 10px 0", color: "#e67025" }}>
                        {pet.name}
                      </h3>
                      <p style={{ fontSize: "13px", color: "#666" }}>
                        {pet.description}
                      </p>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* CASE B: 펫 있음 (육성 화면) */}
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

                {/* 펫 이미지 */}
                <div css={s.petImageArea}>
                  {petStatus.isSleeping && <div css={s.zzzText}>ZZZ...</div>}
                  <img
                    src={getPetImage(petStatus)}
                    alt="pet"
                    css={s.petImage(petStatus.isSleeping)}
                    onError={(e) => {
                      e.target.onerror = null;
                      // 이미지 없을 때 임시 이미지
                      e.target.src = `https://via.placeholder.com/300?text=${petStatus.petType}`;
                    }}
                  />
                </div>

                {/* 하단 컨트롤 */}
                <div
                  css={s.controlPanel}
                  style={{ backgroundColor: "rgba(255, 255, 255, 0.95)" }}
                >
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
                          🍖 밥주기
                        </button>
                        <button
                          css={s.gameBtn}
                          onClick={() => handleInteract("PLAY")}
                        >
                          ⚽ 놀아주기
                        </button>
                        <button
                          css={s.gameBtn}
                          onClick={() => handleInteract("CLEAN")}
                        >
                          ✨ 씻겨주기
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
          width: `${Math.min(100, Math.max(0, value))}%`,
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
