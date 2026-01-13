package com.tutoroo.mapper;

import com.tutoroo.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface UserMapper {

    UserEntity findById(Long id);
    UserEntity findByUsername(String username);
    void save(UserEntity user);
    void update(UserEntity user); // 전체 업데이트

    int countAllUsers();

    UserEntity findByNameAndEmailAndPhone(@Param("name") String name, @Param("email") String email, @Param("phone") String phone);
    UserEntity findByUsernameAndEmail(@Param("username") String username, @Param("email") String email);

    void updatePassword(@Param("id") Long id, @Param("password") String password);
    void updateUserContact(@Param("id") Long id, @Param("phone") String phone);
    void updateSocialUser(UserEntity user);

    void updateUserPointByPlan(@Param("planId") Long planId, @Param("point") int point);
    void resetAllUserPoints();
    List<UserEntity> getRankingList(@Param("gender") String gender, @Param("ageGroup") Integer ageGroup);
    List<UserEntity> findAllByOrderByTotalPointDesc();

    // [신규] 라이벌 찾기
    UserEntity findPotentialRival(@Param("myId") Long myId, @Param("myPoint") int myPoint);

    // [신규] 주간 리포트 대상 조회
    List<UserEntity> findUsersForWeeklyReport();
}