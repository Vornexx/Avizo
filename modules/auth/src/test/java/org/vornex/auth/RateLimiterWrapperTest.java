package org.vornex.auth;


import org.junit.jupiter.api.*;
import org.vornex.auth.util.RateLimiterWrapper;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;

class RateLimiterWrapperTest {

    private RateLimiterWrapper wrapper;

    @BeforeEach
    void init() {
        wrapper = new RateLimiterWrapper();
    }

    @Test
    void resolveBucket_sameKey_returnsSameInstanceAndTryConsumeWorks() {
        String key = "ip:1";
        var b1 = wrapper.resolveBucket(key, 5, 5, Duration.ofMinutes(1));
        var b2 = wrapper.resolveBucket(key, 5, 5, Duration.ofMinutes(1));
        assertThat(b1).isSameAs(b2);

        // tryConsume 5 times ok, 6th — false
        for (int i = 0; i < 5; i++) {
            assertThat(wrapper.tryConsume(key, 5, 5, Duration.ofMinutes(1))).isTrue();
        }
        assertThat(wrapper.tryConsume(key, 5, 5, Duration.ofMinutes(1))).isFalse();
    }
}
