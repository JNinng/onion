package org.ninng.businesssvc.model;

import org.babyfish.jimmer.sql.meta.UserIdGenerator;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class TenantIdGenerator implements UserIdGenerator<String> {

    private static final char[] ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final byte[] DECODE_TABLE = buildDecodeTable();

    private static final long CUSTOM_EPOCH = LocalDateTime.parse("2024-01-11 00:00:00",
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            .toEpochSecond(ZoneOffset.UTC);
    private static final int FEISTEL_ROUNDS = 6;
    private static final long[] ROUND_KEYS = new long[FEISTEL_ROUNDS];
    private static int machineId = 1;
    private static long seed = 0xDEADBEEFCAFEBABEL;
    private final AtomicInteger sequence = new AtomicInteger(0);

    private static byte[] buildDecodeTable() {
        byte[] table = new byte[128];
        Arrays.fill(table, (byte) -1);
        for (int i = 0; i < ALPHABET.length; i++) {
            table[ALPHABET[i]] = (byte) i;
        }
        return table;
    }

    public static void init(int machineId, long seed) {
        TenantIdGenerator.machineId = machineId & 0x3FF;
        TenantIdGenerator.seed = seed;
        deriveRoundKeys();
    }

    /**
     * Derive Feistel round keys from the seed using a split-mix style sequence.
     */
    private static void deriveRoundKeys() {
        long key = seed;
        for (int i = 0; i < FEISTEL_ROUNDS; i++) {
            key ^= key << 13;
            key ^= key >>> 7;
            key ^= key << 17;
            ROUND_KEYS[i] = key & 0xFFFFFFFFFFL;
        }
    }

    /**
     * Feistel round function: 40-bit half + 40-bit round key → 40-bit output.
     * Uses golden-ratio multiply and self-inverse XOR-shifts for bit diffusion.
     */
    private static long F(long half, long roundKey) {
        long x = (half ^ roundKey) & 0xFFFFFFFFFFL;
        x = (x * 0x9E3779B97F4A7C15L) & 0xFFFFFFFFFFL;
        x ^= x >>> 20;
        x ^= x << 13;
        return x & 0xFFFFFFFFFFL;
    }

    /**
     * Forward Feistel permutation on two 40-bit halves (4 rounds).
     * Bijective: input → output is a 1:1 mapping.
     */
    private static long[] feistelPermute(long upper, long lower) {
        long L = upper & 0xFFFFFFFFFFL;
        long R = lower & 0xFFFFFFFFFFL;

        for (int i = 0; i < FEISTEL_ROUNDS; i++) {
            long newL = R;
            long newR = L ^ F(R, ROUND_KEYS[i]);
            L = newL;
            R = newR;
        }

        return new long[]{L, R};
    }

    /**
     * Inverse Feistel permutation.  Reverses {@link #feistelPermute}.
     */
    private static long[] feistelInverse(long upper, long lower) {
        long L = upper & 0xFFFFFFFFFFL;
        long R = lower & 0xFFFFFFFFFFL;

        for (int i = FEISTEL_ROUNDS - 1; i >= 0; i--) {
            long newR = L;
            long newL = R ^ F(L, ROUND_KEYS[i]);
            R = newR;
            L = newL;
        }

        return new long[]{L, R};
    }

    private static String base32Encode(byte[] data) {
        StringBuilder sb = new StringBuilder(16);
        int buffer = 0;
        int bitsInBuffer = 0;

        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsInBuffer += 8;

            while (bitsInBuffer >= 5) {
                bitsInBuffer -= 5;
                int idx = (buffer >>> bitsInBuffer) & 0x1F;
                sb.append(ALPHABET[idx]);
            }
        }

        if (bitsInBuffer > 0) {
            int idx = (buffer << (5 - bitsInBuffer)) & 0x1F;
            sb.append(ALPHABET[idx]);
        }

        return sb.toString();
    }

    private static byte[] base32Decode(String encoded) {
        int buffer = 0;
        int bitsInBuffer = 0;
        byte[] result = new byte[10];
        int pos = 0;

        for (int i = 0; i < encoded.length(); i++) {
            char c = encoded.charAt(i);
            int val = c < 128 ? DECODE_TABLE[c] : -1;
            if (val < 0) {
                throw new IllegalArgumentException("Invalid character in tenant ID: " + c);
            }

            buffer = (buffer << 5) | val;
            bitsInBuffer += 5;

            if (bitsInBuffer >= 8) {
                bitsInBuffer -= 8;
                result[pos++] = (byte) (buffer >>> bitsInBuffer);
            }
        }

        return result;
    }

    // -- Decode API -----------------------------------------------------------

    /**
     * Decode a tenant ID back into its raw components.
     *
     * @return array of 4 longs: [unixEpochSeconds, machineId, sequence, random]
     */
    public static long[] decodeTenantId(String tenantId) {
        byte[] bytes = base32Decode(tenantId);

        long upper = 0;
        long lower = 0;
        for (int i = 0; i < 5; i++) {
            upper = (upper << 8) | (bytes[i] & 0xFF);
        }
        for (int i = 5; i < 10; i++) {
            lower = (lower << 8) | (bytes[i] & 0xFF);
        }

        long[] original = feistelInverse(upper, lower);

        long rawSeconds = original[0] & 0xFFFFFFFFFFL;
        long payload = original[1] & 0xFFFFFFFFFFL;

        return new long[]{
                rawSeconds + CUSTOM_EPOCH,
                (payload >>> 30) & 0x3FF,
                (payload >>> 18) & 0xFFF,
                payload & 0x3FFFF
        };
    }

    // -- Generate API ---------------------------------------------------------

    @Override
    public String generate(Class<?> entityType) {
        return generate();
    }

    public String generate() {
        long seconds = (System.currentTimeMillis() / 1000) - CUSTOM_EPOCH;
        int seq = sequence.getAndIncrement() & 0xFFF;
        int random = ThreadLocalRandom.current()
                .nextInt(1 << 18);

        // Raw 80-bit payload
        long upper = seconds & 0xFFFFFFFFFFL;
        long lower = ((long) machineId << 30) | ((long) seq << 18) | random;

        // Feistel permutation (replaces former XOR timeMask)
        long[] permuted = feistelPermute(upper, lower);
        upper = permuted[0];
        lower = permuted[1];

        // Pack into 10 bytes
        byte[] bytes = new byte[10];
        bytes[0] = (byte) (upper >>> 32);
        bytes[1] = (byte) (upper >>> 24);
        bytes[2] = (byte) (upper >>> 16);
        bytes[3] = (byte) (upper >>> 8);
        bytes[4] = (byte) (upper);
        bytes[5] = (byte) (lower >>> 32);
        bytes[6] = (byte) (lower >>> 24);
        bytes[7] = (byte) (lower >>> 16);
        bytes[8] = (byte) (lower >>> 8);
        bytes[9] = (byte) (lower);

        return base32Encode(bytes);
    }
}
