// [중요] axiosConfig가 있는 경로로 맞춰주세요. 
// 보통 같은 apis 폴더 안에 있다면 "../axiosConfig" 입니다.
import { api } from "../configs/axiosConfig";

// axiosConfig에 이미 도메인(VITE_API_BASE_URL)이 설정되어 있으므로
// 여기서는 뒷부분 경로만 적어주면 됩니다.
const PET_URL = "/api/pet";

/**
 * 1. 현재 내 펫 상태 조회
 * @returns {Promise<Object>} PetDTO.PetStatusResponse
 */
export const getPetStatus = async () => {
    try {
        // api.get을 쓰면 토큰이 자동으로 들어갑니다.
        const response = await api.get(`${PET_URL}/status`);
        return response.data;
    } catch (error) {
        // 404는 펫이 없는 경우이므로 에러가 아닌 null 처리를 위해 throw 방지
        if (error.response && error.response.status === 404) {
            return null;
        }
        throw error;
    }
};

/**
 * 2. 입양 가능한 펫 목록 조회 (초기 생성용)
 * @returns {Promise<Object>} PetDTO.AdoptableListResponse
 */
export const getAdoptablePets = async () => {
    const response = await api.get(`${PET_URL}/adoptable`);
    return response.data;
};

/**
 * 3. 펫 입양하기 (신규 유저용)
 * @param {string} petType - 'FOX', 'RABBIT' 등
 * @param {string} petName
 * @returns {Promise<string>} 성공 메시지
 */
export const adoptPet = async (petType, petName) => {
    const response = await api.post(`${PET_URL}/adopt`, { petType, petName });
    return response.data;
};

/**
 * 4. 상호작용 (밥주기, 놀기 등)
 * @param {string} actionType - 'FEED', 'PLAY', 'CLEAN', 'SLEEP', 'WAKE_UP'
 * @returns {Promise<Object>} 갱신된 PetDTO.PetStatusResponse
 */
export const interactWithPet = async (actionType) => {
    const response = await api.post(`${PET_URL}/interact`, { actionType });
    return response.data;
};

/**
 * 5. 졸업 후 알 목록 조회
 * @returns {Promise<Object>} PetDTO.RandomEggResponse
 */
export const getGraduationEggs = async () => {
    const response = await api.get(`${PET_URL}/eggs`);
    return response.data;
};

/**
 * 6. 알 부화시키기 (졸업 유저용)
 * @param {string} selectedPetType 
 * @param {string} petName
 * @returns {Promise<string>} 성공 메시지
 */
export const hatchEgg = async (selectedPetType, petName) => {
    const response = await api.post(`${PET_URL}/hatch`, { selectedPetType, petName });
    return response.data;
};

export const getMyDiaries = async () => {
    const response = await api.get(`${PET_URL}/diaries`);
    return response.data;
};


export const testWriteDiary = async () => {
    // api.get을 쓰면 토큰이 자동으로 같이 갑니다!
    const response = await api.get(`${PET_URL}/test/diary`);
    return response.data;
};

// 7. 커스텀 펫 생성은 사용 안 하므로 제외 (필요 시 동일하게 api.post 사용)