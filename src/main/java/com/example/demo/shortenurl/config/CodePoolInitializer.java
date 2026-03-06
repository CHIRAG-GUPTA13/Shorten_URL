package com.example.demo.shortenurl.config;

import com.example.demo.shortenurl.entity.CodePool;
import com.example.demo.shortenurl.repository.CodePoolRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Component that initializes the code pool on application startup.
 * Ensures there are at least 1000 unused codes available in the pool.
 */
@Component
public class CodePoolInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(CodePoolInitializer.class);
    private static final int TARGET_POOL_SIZE = 1000;
    private static final int CODE_LENGTH = 8;
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private final CodePoolRepository codePoolRepository;

    public CodePoolInitializer(CodePoolRepository codePoolRepository) {
        this.codePoolRepository = codePoolRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        logger.info("Starting Code Pool Initialization...");
        
        try {
            long unusedCount = codePoolRepository.countByIsUsedFalse();
            logger.info("Current unused codes in pool: {}", unusedCount);

            if (unusedCount < TARGET_POOL_SIZE) {
                int codesToGenerate = TARGET_POOL_SIZE - (int) unusedCount;
                logger.info("Generating {} new codes to reach target pool size of {}", codesToGenerate, TARGET_POOL_SIZE);
                
                generateCodes(codesToGenerate);
                
                long newCount = codePoolRepository.countByIsUsedFalse();
                logger.info("Code Pool Initialization complete. Total unused codes: {}", newCount);
            } else {
                logger.info("Code pool already has sufficient unused codes ({}). No initialization needed.", unusedCount);
            }
        } catch (Exception e) {
            logger.error("Failed to initialize code pool", e);
            throw e;
        }
    }

    /**
     * Generate new unique codes and save them to the pool.
     * @param count number of codes to generate
     */
    private void generateCodes(int count) {
        Random random = new Random();
        AtomicInteger generated = new AtomicInteger(0);
        AtomicInteger attempts = new AtomicInteger(0);
        int maxAttempts = count * 10; // Prevent infinite loop
        
        while (generated.get() < count && attempts.get() < maxAttempts) {
            attempts.incrementAndGet();
            
            String code = generateRandomCode(random);
            
            // Check if code already exists in pool
            if (!codePoolRepository.existsByCode(code)) {
                CodePool codePool = new CodePool(code);
                codePoolRepository.save(codePool);
                generated.incrementAndGet();
                
                if (generated.get() % 100 == 0) {
                    logger.debug("Generated {} codes so far...", generated.get());
                }
            }
        }
        
        if (generated.get() < count) {
            logger.warn("Only generated {} codes out of {} requested after {} attempts", 
                generated.get(), count, attempts.get());
        }
        
        logger.info("Successfully generated {} new codes", generated.get());
    }

    /**
     * Generate a random alphanumeric code.
     * @param random Random instance
     * @return random code string
     */
    private String generateRandomCode(Random random) {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return code.toString();
    }
}
