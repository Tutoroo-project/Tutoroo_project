import axios from "axios";

// 백엔드 API 주소 (본인 환경에 맞게 수정)
const BASE_URL = "http://localhost:8080/api/pet";

/**
 * 응답 데이터에서 알맹이만 추출하는 헬퍼 함수
 */
const extractData = (response) => {
  console.log("📦 백엔드 원본 응답:", response); // F12 콘솔에서 확인용
  // 백엔드가 { code: 1, data: { ... } } 형태로 줄 경우를 대비
  if (response.data && response.data.data) {
    return response.data.data;
  }
  // 그냥 { ... } 형태로 줄 경우
  return response.data;
};

/**
 * 1. 현재 내 펫 상태 조회
 */
export const getPetStatus = async () => {
  try {
    const response = await axios.get(`${BASE_URL}/status`);
    const data = extractData(response);
    console.log("✅ 펫 상태 데이터 추출:", data);
    return data;
  } catch (error) {
    // 404는 펫이 없는 경우이므로 null 반환
    if (error.response && error.response.status === 404) {
      console.log("ℹ️ 펫이 없음 (404)");
      return null;
    }
    console.error("❌ 펫 상태 조회 실패:", error);
    throw error;
  }
};

/**
 * 2. 입양 가능한 펫 목록 조회
 */
export const getAdoptablePets = async () => {
  const response = await axios.get(`${BASE_URL}/adoptable`);
  return extractData(response);
};

/**
 * 3. 펫 입양하기
 */
export const adoptPet = async (petType) => {
  const response = await axios.post(`${BASE_URL}/adopt`, { petType });
  return extractData(response);
};

/**
 * 4. 상호작용 (밥주기, 놀기 등)
 */
export const interactWithPet = async (actionType) => {
  const response = await axios.post(`${BASE_URL}/interact`, { actionType });
  return extractData(response);
};

/**
 * 5. 졸업 후 알 목록 조회
 */
export const getGraduationEggs = async () => {
  const response = await axios.get(`${BASE_URL}/eggs`);
  return extractData(response);
};

/**
 * 6. 알 부화시키기
 */
export const hatchEgg = async (selectedPetType) => {
  const response = await axios.post(`${BASE_URL}/hatch`, { selectedPetType });
  return extractData(response);
};

/**
 * 7. 커스텀 펫 생성
 */
export const createCustomPet = async (name, description) => {
  const response = await axios.post(`${BASE_URL}/create-custom`, {
    name,
    description,
  });
  return extractData(response);
};
