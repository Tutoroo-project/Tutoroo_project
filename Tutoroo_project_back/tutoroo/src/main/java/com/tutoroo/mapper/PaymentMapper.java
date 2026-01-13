package com.tutoroo.mapper;

import com.tutoroo.entity.PaymentEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentMapper {
    // 결제 내역 저장
    void save(PaymentEntity payment);

    // 포트원 고유번호로 결제 내역 조회 (중복 검증용)
    PaymentEntity findByImpUid(String impUid);
}