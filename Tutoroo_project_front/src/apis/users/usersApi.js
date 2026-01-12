import { api } from "../config/axiosConfig";

export const requestMe = () => {
    return api.get("/api/users/me");
}