package rsa;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Objects;

public final class RSAKeyGenerator {
    private static final BigInteger ONE = BigInteger.ONE;

    private final SecureRandom random;

    public RSAKeyGenerator() {
        this(new SecureRandom());
    }

    public RSAKeyGenerator(SecureRandom random) {
        this.random = Objects.requireNonNull(random, "random");
    }

    public RSAKeyPair generateKeyPair(int bitLength) {
        if (bitLength < 512) {
            throw new IllegalArgumentException("Do dai khoa RSA nen tu 512 bit tro len.");
        }

        int primeBits = bitLength / 2;
        BigInteger p;
        BigInteger q;
        BigInteger n;

        do {
            p = BigInteger.probablePrime(primeBits, random);
            q = BigInteger.probablePrime(bitLength - primeBits, random);
            n = p.multiply(q);
        } while (p.equals(q) || n.bitLength() != bitLength);

        BigInteger phi = p.subtract(ONE).multiply(q.subtract(ONE));
        BigInteger e = choosePublicExponent(phi);
        BigInteger d = e.modInverse(phi);

        return new RSAKeyPair(new PublicKey(e, n), new PrivateKey(d, n), p, q, phi);
    }

    private static BigInteger choosePublicExponent(BigInteger phi) {
        BigInteger e = BigInteger.valueOf(65537);
        if (phi.gcd(e).equals(ONE)) {
            return e;
        }

        e = BigInteger.valueOf(3);
        while (!phi.gcd(e).equals(ONE)) {
            e = e.add(BigInteger.TWO);
        }
        return e;
    }

    public static final class RSAKeyPair {
        private final PublicKey publicKey;
        private final PrivateKey privateKey;
        private final BigInteger p;
        private final BigInteger q;
        private final BigInteger phi;

        private RSAKeyPair(PublicKey publicKey, PrivateKey privateKey,
                           BigInteger p, BigInteger q, BigInteger phi) {
            this.publicKey = publicKey;
            this.privateKey = privateKey;
            this.p = p;
            this.q = q;
            this.phi = phi;
        }

        public PublicKey getPublicKey() {
            return publicKey;
        }

        public PrivateKey getPrivateKey() {
            return privateKey;
        }

        public BigInteger getP() {
            return p;
        }

        public BigInteger getQ() {
            return q;
        }

        public BigInteger getPhi() {
            return phi;
        }
    }

    public static final class PublicKey {
        private final BigInteger e;
        private final BigInteger n;

        private PublicKey(BigInteger e, BigInteger n) {
            this.e = e;
            this.n = n;
        }

        public BigInteger getE() {
            return e;
        }

        public BigInteger getN() {
            return n;
        }
    }

    public static final class PrivateKey {
        private final BigInteger d;
        private final BigInteger n;

        private PrivateKey(BigInteger d, BigInteger n) {
            this.d = d;
            this.n = n;
        }

        public BigInteger getD() {
            return d;
        }

        public BigInteger getN() {
            return n;
        }
    }
}