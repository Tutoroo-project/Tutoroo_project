package com.tutoroo.mapper;

import com.tutoroo.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface UserMapper {

    // --- [기존 메서드 유지] ---
    UserEntity findByUsername(String username);
    void save(UserEntity user);

    UserEntity findByNameAndEmailAndPhone(@Param("name") String name, @Param("email") String email, @Param("phone") String phone);
    UserEntity findByUsernameAndEmail(@Param("username") String username, @Param("email") String email);

    void updatePassword(@Param("id") Long id, @Param("password") String password);
    void updateUserContact(@Param("id") Long id, @Param("phone") String phone);

    /** [추가] 소셜 회원가입 완료 정보 업데이트 */
    void updateSocialUser(UserEntity user);

    void updateUserPointByPlan(@Param("planId") Long planId, @Param("point") int point);
    void resetAllUserPoints();
    List<UserEntity> getRankingList(@Param("gender") String gender, @Param("ageGroup") Integer ageGroup);

    // --- [★ 오류 해결을 위해 추가/통합된 메서드] ---

    /** * [기능: ID로 유저 단건 조회]
     * 사용처: StudyService (등급 조회), PaymentService (포인트 조회)
     */
    UserEntity findById(Long id);


    // 전체 유저 수 카운트
    int countAllUsers();
    /**
     * [기능: 유저 전체 정보 업데이트]
     * 사용처: 포인트, 경험치, 레벨, 등급 변경 시 사용
     */
    void update(UserEntity user);

    List<UserEntity> findAllByOrderByTotalPointDesc();
    void updateDailyRank(@Param("id") Long id, @Param("rank") int rank);
}