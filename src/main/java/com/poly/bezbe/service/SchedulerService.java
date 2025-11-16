package com.poly.bezbe.service;

import com.poly.bezbe.entity.Coupon;
import com.poly.bezbe.entity.Promotion;
import com.poly.bezbe.repository.CouponRepository;
import com.poly.bezbe.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j // Ghi log để bạn theo dõi
public class SchedulerService {

    private final PromotionRepository promotionRepository;
    private final CouponRepository couponRepository;

    /**
     * Tác vụ 1: Kích hoạt (BẬT) Khuyến mãi & Coupon sắp diễn ra.
     * Chạy vào 00:01 (1 phút sáng) mỗi ngày.
     */
    @Scheduled(cron = "0 1 0 * * *")
    @Transactional
    public void activatePendingItems() {
        LocalDate today = LocalDate.now();
        log.info("Bắt đầu tác vụ: Kích hoạt Khuyến mãi & Coupon cho ngày {}", today);

        // Kích hoạt Promotions (Chỉ tìm active=false VÀ startDate=hôm nay)
        List<Promotion> promotionsToActivate = promotionRepository
                .findAllByActiveAndStartDate(false, today);
        if (!promotionsToActivate.isEmpty()) {
            promotionsToActivate.forEach(promo -> promo.setActive(true));
            promotionRepository.saveAll(promotionsToActivate);
            log.info("Đã kích hoạt {} khuyến mãi.", promotionsToActivate.size());
        }

        // Kích hoạt Coupons (Chỉ tìm active=false VÀ startDate=hôm nay)
        List<Coupon> couponsToActivate = couponRepository
                .findAllByActiveAndStartDate(false, today);
        if (!couponsToActivate.isEmpty()) {
            couponsToActivate.forEach(coupon -> coupon.setActive(true));
            couponRepository.saveAll(couponsToActivate);
            log.info("Đã kích hoạt {} coupon.", couponsToActivate.size());
        }

        log.info("Hoàn tất tác vụ kích hoạt.");
    }

    /**
     * Tác vụ 2: Hủy (TẮT) Khuyến mãi & Coupon đã hết hạn.
     * Chạy vào 00:02 (2 phút sáng) mỗi ngày.
     */
    @Scheduled(cron = "0 2 0 * * *")
    @Transactional
    public void deactivateExpiredItems() {
        LocalDate today = LocalDate.now();
        log.info("Bắt đầu tác vụ: Hủy Khuyến mãi & Coupon đã hết hạn trước ngày {}", today);

        // Hủy Promotions (Tìm active=true VÀ endDate < hôm nay)
        List<Promotion> promotionsToDeactivate = promotionRepository
                .findAllByActiveAndEndDateLessThan(true, today);
        if (!promotionsToDeactivate.isEmpty()) {
            promotionsToDeactivate.forEach(promo -> promo.setActive(false));
            promotionRepository.saveAll(promotionsToDeactivate);
            log.info("Đã hủy {} khuyến mãi hết hạn.", promotionsToDeactivate.size());
        }

        // Hủy Coupons (Tìm active=true VÀ endDate < hôm nay)
        List<Coupon> couponsToDeactivate = couponRepository
                .findAllByActiveAndEndDateLessThan(true, today);
        if (!couponsToDeactivate.isEmpty()) {
            couponsToDeactivate.forEach(coupon -> coupon.setActive(false));
            couponRepository.saveAll(couponsToDeactivate);
            log.info("Đã hủy {} coupon hết hạn.", couponsToDeactivate.size());
        }

        log.info("Hoàn tất tác vụ hủy.");
    }
}