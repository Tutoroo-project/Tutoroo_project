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

    // --- [포인트 관리 (분리됨)] ---
    // 포인트 획득 (랭킹+지갑)
    void earnPoints(@Param("userId") Long userId, @Param("amount") int amount);
    // 포인트 사용 (지갑만)
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