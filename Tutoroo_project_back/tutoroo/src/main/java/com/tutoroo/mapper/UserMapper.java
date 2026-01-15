package com.tutoroo.mapper;

import com.tutoroo.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {

    // --- [기본 CRUD] ---
    void save(UserEntity user);
    int countAllUsers();
    UserEntity findById(Long id);
    UserEntity findByUsername(String username);
    void update(UserEntity user); // 회원정보 전체 업데이트

    // --- [인증 & 계정 찾기] ---
    UserEntity findByNameAndEmailAndPhone(@Param("name") String name, @Param("email") String email, @Param("phone") String phone);
    UserEntity findByUsernameAndEmail(@Param("username") String username, @Param("email") String email);
    void updatePassword(@Param("id") Long id, @Param("password") String password);
    void updateUserContact(@Param("id") Long id, @Param("phone") String phone);
    void updateSocialUser(UserEntity user);

    // --- [학습 & 포인트 & 펫 (여기가 중요)] ---

    // 1. [다마고치용] 포인트 직접 변경 (PetService 오류 해결용)
    void updateUserPoint(@Param("userId") Long userId, @Param("point") int point);

    // 2. 학습 플랜 완료 보상
    void updateUserPointByPlan(@Param("planId") Long planId, @Param("point") int point);

    // 3. 포인트 리셋
    void resetAllUserPoints();


    // --- [랭킹 & 라이벌] ---
    List<UserEntity> getRankingList(@Param("gender") String gender, @Param("ageGroup") Integer ageGroup);
    List<UserEntity> findAllByOrderByTotalPointDesc();
    UserEntity findPotentialRival(@Param("myId") Long myId, @Param("myPoint") int myPoint);


    // --- [스케줄러 & 리포트] ---
    List<UserEntity> findUsersForWeeklyReport();
    List<UserEntity> findWithdrawnUsersForPurge();
    void deleteUserPermanently(Long id);
}