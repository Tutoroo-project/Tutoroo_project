import axios from 'axios';

// 백엔드 API 기본 URL 설정 (환경 변수 또는 하드코딩)
const BASE_URL = 'http://localhost:8080/api/pet';

/**
 * 1. 현재 내 펫 상태 조회
 * @returns {Promise<Object>} PetDTO.PetStatusResponse
 */
export const getPetStatus = async () => {
    try {
        const response = await axios.get(`${BASE_URL}/status`);
        return response.data;
    } catch (error) {
        // 404는 펫이 없는 경우이므로 에러가 아닌 null 처리를 위해 throw
        throw error;
    }
};

/**
 * 2. 입양 가능한 펫 목록 조회 (초기 생성용)
 * @returns {Promise<Object>} PetDTO.AdoptableListResponse
 */
export const getAdoptablePets = async () => {
    const response = await axios.get(`${BASE_URL}/adoptable`);
    return response.data;
};

/**
 * 3. 펫 입양하기
 * @param {string} petType - 'TIGER', 'RABBIT' 등
 * @returns {Promise<string>} 성공 메시지
 */
export const adoptPet = async (petType) => {
    const response = await axios.post(`${BASE_URL}/adopt`, { petType });
    return response.data;
};

/**
 * 4. 상호작용 (밥주기, 놀기 등)
 * @param {string} actionType - 'FEED', 'PLAY', 'CLEAN', 'SLEEP', 'WAKE_UP'
 * @returns {Promise<Object>} 갱신된 PetDTO.PetStatusResponse
 */
export const interactWithPet = async (actionType) => {
    const response = await axios.post(`${BASE_URL}/interact`, { actionType });
    return response.data;
};

/**
 * 5. 졸업 후 알 목록 조회
 * @returns {Promise<Object>} PetDTO.RandomEggResponse
 */
export const getGraduationEggs = async () => {
    const response = await axios.get(`${BASE_URL}/eggs`);
    return response.data;
};

/**
 * 6. 알 부화시키기
 * @param {string} selectedPetType 
 * @returns {Promise<string>} 성공 메시지
 */
export const hatchEgg = async (selectedPetType) => {
    const response = await axios.post(`${BASE_URL}/hatch`, { selectedPetType });
    return response.data;
};

/**
 * 7. 커스텀 펫 생성 (Step 20)
 * @param {string} name 
 * @param {string} description 
 * @returns {Promise<string>} 성공 메시지
 */
export const createCustomPet = async (name, description) => {
    const response = await axios.post(`${BASE_URL}/create-custom`, { 
        name, 
        description 
    });
    return response.data;
};