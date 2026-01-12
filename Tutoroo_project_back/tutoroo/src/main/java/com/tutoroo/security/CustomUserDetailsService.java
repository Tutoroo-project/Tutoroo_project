package com.tutoroo.security;

import com.tutoroo.entity.UserEntity;
import com.tutoroo.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * [기능: 사용자 인증 데이터 로드 서비스]
 * 설명: Spring Security의 핵심 인터페이스인 UserDetailsService를 구현하여 DB 기반 인증을 수행합니다.
 * 작동원리: MyBatis 매퍼를 주입받아 실제 DB 쿼리를 실행하고 결과를 CustomUserDetails에 담아 반환합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;

    /**
     * [기능: 아이디를 통한 사용자 조회 및 변환]
     * 작동원리:
     * 1. DB에서 username으로 사용자 정보를 검색합니다.
     * 2. 검색 결과가 없을 시 즉각적인 예외 처리를 수행합니다.
     * 3. 검색 결과를 CustomUserDetails record로 감싸서 리턴합니다.
     * @param username 로그인 시도 아이디
     * @return 시큐리티 인증 객체
     * @throws UsernameNotFoundException 해당 사용자가 없을 경우 발생
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("사용자 인증 정보 조회 시작: {}", username);

        UserEntity user = userMapper.findByUsername(username);

        if (user == null) {
            log.warn("사용자 인증 실패 - 존재하지 않는 아이디: {}", username);
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username);
        }

        log.debug("사용자 인증 정보 조회 성공: {}", username);
        return new CustomUserDetails(user);
    }
}