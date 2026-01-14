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
            <div css={s.infoPageWrapper}>
                <Sidebar />
                <main css={s.infoPageMainContainer}>
                    <div css={s.scrollableCard}>

                        <div css={s.profileSection}>
                            <div css={s.sectionTitle}>
                                <div className="icon-person">
                                    <BsPersonCircle size={50}/>
                                </div>
                                프로필 이미지 수정
                            </div>

                            <div css={s.imageUploadBox}>
                                <div css={s.uploadPlaceholder}>
                                    <FaCamera size={40} color="#333"/>
                                    <p>이미지를 드래그하거나 선택해서 업로드</p>
                                    <span>최대 1개 파일 5MB 이해</span>
                                    <button type="button">파일 선택</button>
                                </div>
                            </div>
                        </div>

                        <div css={s.formSection}>
                            <div css={s.infoInputGroup}>
                                <label>이름</label>
                                <input type="text" value={profile.name} />
                            </div>
                            <div css={s.infoInputGroup}>
                                <label>전화번호</label>
                                <input type="text" value={profile.phone} />
                            </div>
                            <div css={s.infoInputGroup}>
                                <label>나이</label>
                                <input type="text" value={profile.age} />
                            </div>
                            <div css={s.infoInputGroup}>
                                <label>이메일</label>
                                <input type="email" value={profile.email} />
                            </div>
                        </div>

                        <button css={s.subBtn}>변경하기</button>


                    </div>
                </main>
            </div>
        </>
           
        

    );
 }
 
 export default ChangeInfoPage;