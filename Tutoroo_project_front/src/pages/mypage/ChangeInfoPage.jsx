 /** @jsxImportSource @emotion/react */
import Header from "../../components/layouts/Header";
import Sidebar from "./Sidebar";
import * as s  from "./styles";
import { FaCamera } from "react-icons/fa";
import { BsPersonCircle } from "react-icons/bs";
import { useEffect, useRef, useState } from "react";
import axios from "axios";

 function ChangeInfoPage() {
    const [ profile, setProfile ] = useState({
        name: "",
        phone: "",
        age: "",
        email: ""

    });
    
    const [ imageFile, setImageFile] = useState(null);
    const [ previewUrl, setpriviewUrl ] = useState("");

    const fileInputRef = useRef(null);

    useEffect(() => {
        const fetchUseData = async() => {
            const token = localStorage.getItem("accessToken");

            if (!token) return;
            
        }
    })

    const handleImageChange = (e) => {
        const file = e.target.file[0];
        if (file) {
            setImageFile(File);
            const reader = new FileReader();
            reader.onloadend = () => {
                setpriviewUrl(reader.result);
            };
            reader.readAsDataURL(file);
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
                                    <BsPersonCircle size={40} color="#ccc"/>
                                    프로필 이미지 수정
                                </div>
                                <div css={s.imageUploadBox}>
                                    <div css={s.uploadPlaceholder}>
                                        <FaCamera size={30} />
                                        <p style={{fontSize:'16px'}}>이미지 드래그 또는 선택</p>
                                        <button type="button">파일 선택</button>
                                    </div>
                                </div>
                            </div>

                            <div css={s.commonInputGroup}>
                                <label>이름</label>
                                <input type="text" value={profile.name} onChange={(e)=>setProfile({...profile, name:e.target.value})} />
                            </div>
                            <div css={s.commonInputGroup}>
                                <label>전화번호</label>
                                <input type="text" value={profile.phone} onChange={(e)=>setProfile({...profile, phone:e.target.value})} />
                            </div>
                            <div css={s.commonInputGroup}>
                                <label>나이</label>
                                <input type="text" value={profile.age} onChange={(e)=>setProfile({...profile, age:e.target.value})} />
                            </div>
                            <div css={s.commonInputGroup}>
                                <label>이메일</label>
                                <input type="email" value={profile.email} onChange={(e)=>setProfile({...profile, email:e.target.value})} />
                            </div>
                        </div>

                        <button css={s.actionBtn}>변경하기</button>
                    </div>

                </main>
            </div>
        </>
    );
}
 
export default ChangeInfoPage;