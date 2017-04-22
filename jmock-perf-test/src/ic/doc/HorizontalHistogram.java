package ic.doc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HorizontalHistogram {
    private static final int N_MIN = 3;
    private static final int N_MAX = 10;

    private final List<Double> binEdges;
    private final List<Integer> binCounts;

    public HorizontalHistogram(List<Double> numbers) {
        double max = Collections.max(numbers);
        double min = Collections.min(numbers);
        double minCost = Double.MAX_VALUE;

        List<Double> binEdges = null;
        List<Integer> binCounts = null;

        for (int N = N_MIN; N < N_MAX; N++) {
            double delta = (max - min) / N;
            List<Double> edges = linspace(min, max, N + 1);
            List<Integer> bins = bin(numbers, edges);
            double mean = bins.stream().mapToInt(v -> v).average().orElse(0.0);
            double var = bins.stream().mapToDouble(x -> Math.pow((x - mean), 2)).sum() / N;
            double cost = (2 * mean - var) / (delta * delta);
            if (cost <= minCost) {
                minCost = cost;
                binEdges = edges;
                binCounts = bins;
            }
        }

        this.binEdges = binEdges;
        this.binCounts = binCounts;
    }

    public void print() {
        for (int i = 0; i < binEdges.size() - 1; i++) {
            System.out.println(String.format("%05.2f", binEdges.get(i))
                + "-"
                + String.format("%05.2f", binEdges.get(i + 1))
                + " "
                + String.join("", Collections.nCopies(binCounts.get(i), "*"))
            );
        }
    }

    private List<Double> linspace(double min, double max, int num) {
        double diff = max - min;
        double step = diff / (num - 1);
        List<Double> ret = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            ret.add(min + step * i);
        }
        return ret;
    }

    private List<Integer> bin(List<Double> numbers, List<Double> edges) {
        int size = edges.size();
        List<Integer> ret = new ArrayList<>(Collections.nCopies(size, 0));
        for (Double d : numbers) {
            for (int i = 0; i < (size - 1); i++) {
                Double lower = edges.get(i);
                Double upper = edges.get(i + 1);
                if (d >= lower && d <= upper) {
                    ret.set(i, ret.get(i) + 1);
                }
            }
        }
        return ret;
    }
}