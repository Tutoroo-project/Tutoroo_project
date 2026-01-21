 /** @jsxImportSource @emotion/react */
import Header from "../../components/layouts/Header";
import Sidebar from "./Sidebar";
import * as s  from "./styles";
import { FaCamera } from "react-icons/fa";
import { BsPersonCircle } from "react-icons/bs";
import { useEffect, useRef, useState } from "react";
import { userApi } from "../../apis/users/usersApi";
import Swal from "sweetalert2";

 function ChangeInfoPage() {
    const BASE_URL = import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, "") || "http://localhost:8080";

    const [ profile, setProfile ] = useState({
        name: "",
        phone: "",
        age: "",
        email: ""

    });
    
    const [ imageFile, setImageFile] = useState(null);
    const [ previewUrl, setPreviewUrl ] = useState(""); 

    
    const fileInputRef = useRef(null);

    const getFullImageUrl = (path) => {
        if (!path) return "";
        
        if (path.startsWith("http") || path.startsWith("blob:")) return path;
        const replacePath = path.startsWith("/") ? path : `${path}`;
        return `${BASE_URL}${replacePath}`
    }

    useEffect(() => {
        const fetchUserData = async() => {
            try {

                const data = await userApi.getProfile();
                
                if (!data) return;
                console.log("서버에서 온 데이터:", data)

                setProfile({
                    name: data.name || "",
                    phone: data.phone || "",
                    age: data.age || "",
                    email: data.email || "",
                });

                if (data.profileImage) {
                    setPreviewUrl(getFullImageUrl(data.profileImage));
                } else {
                    setPreviewUrl("");
                }
            } catch (error) {
                console.error("프로필 로딩 실패", error);
            }
        }
        fetchUserData();
    }, []);

    const handleImageChange = (e) => {
        const file = e.target.files[0];
        if (file) {
            setImageFile(file);
            const reader = new FileReader();
            reader.onloadend = () => {
                setPreviewUrl(reader.result);
            };
            reader.readAsDataURL(file);
        } 
    }

    const handleSubmit = async () => {
        try {
           const { value: currentPassword } = await Swal.fire({
                title: '본인 확인',
                input: 'password',
                inputLabel: '정보를 수정하려면 현재 비밀번호를 입력하세요',
                inputPlaceholder: '비밀번호 입력',
                showCancelButton: true
            });
                if (!currentPassword) return;
                    
            const updateData = {
                currentPassword: "currentPassword",
                name: profile.name || "",
                age: profile.age === "" ? null : parseInt(profile.age),
                email: profile.email || "",
                phone: profile.phone || "" 
            };
            
            console.log("전송 데이터:", updateData);
            console.log("전송 이미지:", imageFile);

            
            const resp = await userApi.updateProfile({ data: updateData, profileImage: imageFile });
            
            console.log("서버 응답:", resp);

            
            Swal.fire("성공", resp.message || "회원 정보가 수정되었습니다.", "success")
            .then(() => {
               
                window.location.reload(); 
            });
           
            if(resp.after) {
                 setProfile({
                    name: resp.after.name,
                    phone: resp.after.phone,
                    age: resp.after.age,
                    email: resp.after.email
                });
                if (resp.after.profileImage) {
                    const newUrl = getFullImageUrl(resp.after.profileImage);
                    setPreviewUrl(`${newUrl}?t=${new Date().getTime()}`);
                    
                }
            }

        } catch (error) {
            console.error(error)
            Swal.fire ("실패", "정보 수정이 실패했습니다", "error");
        }
    }

   return (
        <>
            <Header />
            <div css={s.wrapper}> 
                <Sidebar />
                <main css={s.mainContainer}> 
                    <div css={s.commonCard}>
                        <h1 css={s.cardTitle}>회원 정보 변경</h1>
                        <div css={s.cardContent}>
                            <div css={s.profileSection}>
                                <div style={{display:'flex', alignItems:'center', gap:'10px', fontSize:'18px', fontWeight:'600' , marginBottom: '10px' }}>
                                    {previewUrl ? (
                                        <img 
                                            src={previewUrl} 
                                            alt="미니 프로필" 
                                            style={{
                                                width: '40px', 
                                                height: '40px', 
                                                borderRadius: '50%', 
                                                objectFit: 'cover',
                                                border: '1px solid #ddd'
                                            }} 
                                        />
                                    ) : (
                                        <BsPersonCircle size={40} color="#ccc"/>
                                    )}
                                    프로필 이미지 수정
                                </div>
                                {/* 이미지가 있거나 미리보기가 있으면 그것을 보여줌 (코드상 미리보기 영역이 생략되어 있어 추가 필요 가능성 있음) */}
                                <div css={s.imageUploadBox} onClick={() => fileInputRef.current.click()}>
                                    {previewUrl ? (
                                        <img 
                                        src={previewUrl} 
                                        alt="프로필 미리보기" 
                                        style={{width: '100%', height: '100%', objectFit: 'cover', borderRadius: '50%', objectPosition: 'center center'}} />
                                    ) : (
                                        <div css={s.uploadPlaceholder}>
                                            <FaCamera size={30} />
                                            <p style={{fontSize:'16px'}}>이미지 드래그 또는 선택</p>
                                        </div>
                                    )}
                                    <input type="file" ref={fileInputRef} style={{display: 'none'}} accept="image/*" onChange={handleImageChange}/>
                                </div>
                            </div>

                            <div css={s.commonInputGroup}>
                                <label>이름</label>
                                <input type="text" value={profile.name || ""} onChange={(e)=>setProfile({...profile, name:e.target.value})} />
                            </div>
                            <div css={s.commonInputGroup}>
                                <label>전화번호</label>
                                <input type="text" value={profile.phone || ""} onChange={(e)=>setProfile({...profile, phone:e.target.value})} />
                            </div>
                            <div css={s.commonInputGroup}>
                                <label>나이</label>
                                <input type="text" value={profile.age || ""} onChange={(e)=>setProfile({...profile, age:e.target.value})} />
                            </div>
                            <div css={s.commonInputGroup}>
                                <label>이메일</label>
                                <input type="email" value={profile.email || ""} onChange={(e)=>setProfile({...profile, email:e.target.value})} />
                            </div>
                        </div>

                        <button css={s.actionBtn} onClick={handleSubmit}>변경하기</button>
                    </div>

                </main>
            </div>
        </>
    );
}
 
export default ChangeInfoPage;