package com.humangc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.humangc.dto.ServerStatusResponse;
import com.humangc.entity.Donation;
import com.humangc.entity.User;
import com.humangc.mapper.DonationMapper;
import com.humangc.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class ServerStatusService {

    @Autowired
    private DonationMapper donationMapper;

    @Autowired
    private UserMapper userMapper;

    @Value("${humangc.server-cost-daily:3}")
    private BigDecimal dailyCost;

    @Value("${humangc.server-initial-balance:0}")
    private BigDecimal initialBalance;

    private static final BigDecimal TARGET_DAYS = BigDecimal.valueOf(30);

    /**
     * Get server status based on: initial balance + donations vs daily cost.
     * daysRemaining = (initialBalance + donatedAmount) / dailyCost
     * progressPercent = totalFunds / (dailyCost * 30) * 100 (30-day target)
     */
    public ServerStatusResponse getStatus() {
        BigDecimal donatedAmount = getTotalDonated();
        BigDecimal totalFunds = initialBalance.add(donatedAmount);
        BigDecimal targetCost = dailyCost.multiply(TARGET_DAYS); // 30-day cost target

        // proportion of 30-day target covered
        BigDecimal proportion = totalFunds.divide(targetCost, 4, RoundingMode.HALF_UP);
        BigDecimal progressPercent = proportion.multiply(BigDecimal.valueOf(100))
                .min(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);

        // Actual days the funds can cover
        long daysRemaining = totalFunds.divide(dailyCost, 0, RoundingMode.DOWN).longValue();
        BigDecimal remainder = totalFunds.subtract(dailyCost.multiply(BigDecimal.valueOf(daysRemaining)));
        long hoursRemaining = remainder.divide(dailyCost, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(24))
                .setScale(0, RoundingMode.DOWN)
                .longValue();

        boolean running = true;

        return new ServerStatusResponse(
                running,
                daysRemaining,
                hoursRemaining,
                progressPercent,
                targetCost,
                donatedAmount
        );
    }

    /**
     * Process a donation.
     */
    @Transactional
    public ServerStatusResponse donate(BigDecimal amount, String anonymousId) {
        log.info("Processing donation: amount={}, anonymousId={}", amount, anonymousId);

        User user = findOrCreateUser(anonymousId);

        Donation donation = new Donation();
        donation.setUserId(user.getId());
        donation.setAmount(amount);
        donation.setExtendedHours(0);
        donation.setCreatedAt(LocalDateTime.now());
        donationMapper.insert(donation);

        log.info("Donation saved: userId={}, amount={}", user.getId(), amount);

        return getStatus();
    }

    private BigDecimal getTotalDonated() {
        List<Donation> donations = donationMapper.selectList(null);
        return donations.stream()
                .map(Donation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private User findOrCreateUser(String anonymousId) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getAnonymousId, anonymousId);
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            user = new User();
            user.setAnonymousId(anonymousId);
            user.setCreatedAt(LocalDateTime.now());
            userMapper.insert(user);
        }

        return user;
    }
}
