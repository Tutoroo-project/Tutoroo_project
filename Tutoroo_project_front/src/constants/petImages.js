
import foxEgg from "../assets/pets/fox_egg.png"; // 여우 알 상태
import foxEgg1 from "../assets/pets/fox_egg1.png"; // 여우 알 감정1
import foxEgg2 from "../assets/pets/fox_egg2.png"; // 여우 알 감정2
import foxEgg3 from "../assets/pets/fox_egg3.png"; // 여우 알 감정3

//----------------------------------------------------------------------

import rabitEgg1 from "../assets/pets/rabit_egg1.png"; //토끼 알 상태 (기본, 사랑, 사랑부족, 놀기)

//-------------------------------------------------------------------------

import tigerEgg1 from "../assets/pets/tiger_egg1.png"; //호랑이 알 상태 (기본, 사랑, 사랑부족, 놀기)

//------------------------------------------------------------------------

import turtleEgg1 from "../assets/pets/turtle_egg1.png"; //거북이 알 상태 (기본, 사랑, 사랑부족, 놀기)

//--------------------------------------------------------------------------

import quokkaEgg1 from "../assets/pets/quokka_egg1.png"; //쿼카 알 상태 (기본, 사랑, 사랑부족, 놀기)

import foxBaby from "../assets/pets/fox_baby.png"; // 여우 아기 상태
// import foxIdle from "../assets/pets/fox_Idle.png"; // 여우 아이 상태

//------------------------------------여우 아기 상태--------------------------
import foxPart1 from "../assets/pets/fox_baby1.png"; //자기, 깨우기, 가출, 레벨업
import foxPart2 from "../assets/pets/fox_baby2.png"; // 기본, 사랑, 사랑부족, 식사
import foxPart3 from "../assets/pets/fox_baby3.png"; // 배고파요, 놀기, 심심해요, 피곤해요


//----------------------------------여우 학생 상태----------------------------------------
// import foxStudent from "../assets/pets/fox_studnet.png"; // 여우 학생 상태
import foxStudent1 from "../assets/pets/fox_student1.png"; // 여우 학생 상태 (기본, 사랑, 사랑부족, 놀기)
import foxStudent2 from "../assets/pets/fox_student2.png"; // 여우 학생 상태 (먹기_배고파요_심심해요_자기)
import foxStudent3 from "../assets/pets/fox_student3.png"; // 여우 학생 상태 (피곤해요_깨우기(일어나기)_가출_레벨업)
import foxStudent4 from "../assets/pets/fox_student4.png"; // 여우 학생 상태 (일기)


//---------------------------------여우 어른 상태------------------------------------------------
// import foxAdult from "../assets/pets/fox_baby.png"; // 여우 어른 상태
import foxAdult1 from "../assets/pets/fox_adult1.png"; // 여우 어른 상태 (기본, 사랑, 사랑부족, 놀기)
import foxAdult2 from "../assets/pets/fox_adult2.png"; // 여우 어른 상태 (먹기_배고파요_심심해요_자기)
import foxAdult3 from "../assets/pets/fox_adult3.png"; // 여우 어른 상태 (피곤해요_깨우기(일어나기)_가출_레벨업)
import foxAdult4 from "../assets/pets/fox_adult4.png"; // 여우 어른 상태 (일기)




export const PET_IMAGES = { //전체적으로 아기 상태로 설정

    FoxEgg: {
        BASIC: foxEgg,
        PART1: foxEgg1,
        PART2: foxEgg2,
        PART3: foxEgg3,

    },

    RabitEgg: {
        BASIC: rabitEgg1,
    },

    TigerEgg: {
        BASIC: tigerEgg1,
    },

    TurtleEgg: {
        BASIC: turtleEgg1,
    },

    quokkaEgg: {
        BASIC: quokkaEgg1,
    },
   
    Fox: {
        BASIC: foxBaby,
        PART1: foxPart1,
        PART2: foxPart2,
        PART3: foxPart3
    },

    FoxIdle: {

    },

    FoxStudent: {
        PART1: foxStudent1,
        PART2: foxStudent2,
        PART3: foxStudent3,
        PART4: foxStudent4,
    },
    
    FoxAdult: {
        PART1: foxAdult1,
        PART2: foxAdult2,
        PART3: foxAdult3,
        PART4: foxAdult4,
    }
}