package org.jmock.internal.perfmodel.distribution;

public class ExponentialDistribution implements Distribution {
    private static final long[] FACTORIALS = new long[]{
        1L, 1L, 2L,
        6L, 24L, 120L,
        720L, 5040L, 40320L,
        362880L, 3628800L, 39916800L,
        479001600L, 6227020800L, 87178291200L,
        1307674368000L, 20922789888000L, 355687428096000L,
        6402373705728000L, 121645100408832000L, 2432902008176640000L
    };

    private static final double[] EXPONENTIAL_SA_QI = new double[20];

    private final double lambda;

    static {
        final double LN2 = Math.log(2);
        double qi = 0;
        int i = 1;

        while (qi < 1) {
            qi += Math.pow(LN2, i) / FACTORIALS[i];
            EXPONENTIAL_SA_QI[i - 1] = qi;
            ++i;
        }
    }

    public ExponentialDistribution(double lambda) {
        this.lambda = lambda;
    }

    public double sample() {
        double a = 0;
        double u = Math.random();

        while (u < 0.5) {
            a += EXPONENTIAL_SA_QI[0];
            u *= 2;
        }

        u += u - 1;

        if (u <= EXPONENTIAL_SA_QI[0]) {
            return lambda * (a + u);
        }

        int i = 0;
        double u2 = Math.random();
        double umin = u2;

        do {
            ++i;
            u2 = Math.random();

            if (u2 < umin) {
                umin = u2;
            }
        } while (u > EXPONENTIAL_SA_QI[i]);

        return lambda * (a + umin * EXPONENTIAL_SA_QI[0]);
    }
}