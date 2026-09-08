package com.parut.user.seller.infrastructure;

import com.parut.user.seller.domain.Seller;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SellerRepository extends JpaRepository<Seller, UUID> {
    Page<Seller> findByCompanyNameContainingOrManagerNameContaining(String companyName, String managerName, Pageable pageable);

    // 입점 신청(회원가입) 시 아이디 중복 체크용
    boolean existsByLoginId(String loginId);

    // 판매자 로그인 시 정보 조회용
    Optional<Seller> findByLoginId(String loginId);
}