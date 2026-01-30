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
    UserEntity findByUsername(@Param("username") String username);

    // [핵심] 통합 업데이트 메서드 (이거 하나로 프로필 수정, 탈퇴, 포인트 변경 다 처리)
    void update(UserEntity user);

    // --- [인증 & 계정 찾기] ---
    UserEntity findByNameAndEmailAndPhone(@Param("name") String name, @Param("email") String email, @Param("phone") String phone);
    UserEntity findByUsernameAndEmail(@Param("username") String username, @Param("email") String email);
    void updatePassword(@Param("id") Long id, @Param("password") String password);
    void updateUserContact(@Param("id") Long id, @Param("phone") String phone);
    void updateSocialUser(UserEntity user);

    // --- [포인트 관리] ---
    void earnPoints(@Param("userId") Long userId, @Param("amount") int amount);
    void spendPoints(@Param("userId") Long userId, @Param("amount") int amount);
    void updateUserPointByPlan(@Param("planId") Long planId, @Param("point") int point);
    void resetRankingPoints();

    // --- [랭킹 & 라이벌] ---
    List<UserEntity> getRankingList(@Param("gender") String gender, @Param("ageGroup") Integer ageGroup);
    List<UserEntity> findAllByOrderByTotalPointDesc();
    UserEntity findPotentialRival(@Param("myId") Long myId, @Param("myPoint") int myPoint);

    // --- [관리/스케줄러] ---
    List<UserEntity> findUsersForWeeklyReport();
    List<UserEntity> findWithdrawnUsersForPurge();
    void deleteUserPermanently(Long id);
}