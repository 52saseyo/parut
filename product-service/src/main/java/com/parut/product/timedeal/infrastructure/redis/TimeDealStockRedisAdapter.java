package com.parut.product.timedeal.infrastructure.redis;

import com.parut.product.timedeal.application.port.out.timedealstock.TimeDealStockRedisPort;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TimeDealStockRedisAdapter implements TimeDealStockRedisPort {

    private final RedissonClient redissonClient;

    @Override
    public void setAvailableQuantity(UUID timeDealId, UUID stockId, int availableQuantity) {
        RBucket<Integer> stockBucket = redissonClient.getBucket(
                TimeDealRedisKeys.stock(timeDealId, stockId));
        stockBucket.set(availableQuantity);
    }

    @Override
    public void delete(UUID timeDealId, UUID stockId) {
        RBucket<Integer> stockBucket = redissonClient.getBucket(
                TimeDealRedisKeys.stock(timeDealId, stockId));
        stockBucket.delete();
    }
}
