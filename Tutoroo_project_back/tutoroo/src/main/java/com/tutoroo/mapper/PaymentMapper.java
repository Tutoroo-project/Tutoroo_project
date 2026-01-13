package com.tutoroo.mapper;

import com.tutoroo.entity.PaymentEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentMapper {

    /**
     * [기능: 결제 정보 저장]
     * 매핑: resources/mapper/PaymentMapper.xml -> id="save"
     */
    void save(PaymentEntity payment);

    /**
     * [기능: 포트원 고유번호로 조회]
     * 매핑: resources/mapper/PaymentMapper.xml -> id="findByImpUid"
     */
    PaymentEntity findByImpUid(String impUid);
}