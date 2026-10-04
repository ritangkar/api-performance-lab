import java.util.*;
import java.util.stream.*;

public class ParallelBench {
    record Order(long id, double subtotal, double discountRate, double taxRate) {}

    // Deliberately non-trivial CPU work per order, similar to real tax/discount recalculation
    static double recalcTotal(Order o) {
        double total = o.subtotal();
        // simulate tiered discount + tax computation with some iteration (not sleep - real CPU work)
        for (int i = 0; i < 400; i++) {
            total = total * (1 - o.discountRate() / 400) + Math.sin(i) * 0.0001;
        }
        total = total * (1 + o.taxRate());
        return total;
    }

    public static void main(String[] args) {
        int n = 60_000;
        Random rng = new Random(7);
        List<Order> orders = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            orders.add(new Order(i, 200 + rng.nextDouble() * 8000, 0.05 + rng.nextDouble() * 0.15, 0.12));
        }

        // warm up JIT
        orders.stream().limit(2000).map(ParallelBench::recalcTotal).count();
        orders.parallelStream().limit(2000).map(ParallelBench::recalcTotal).count();

        long t0 = System.nanoTime();
        double sumSeq = orders.stream().mapToDouble(ParallelBench::recalcTotal).sum();
        long t1 = System.nanoTime();
        double sumPar = orders.parallelStream().mapToDouble(ParallelBench::recalcTotal).sum();
        long t2 = System.nanoTime();

        double seqMs = (t1 - t0) / 1_000_000.0;
        double parMs = (t2 - t1) / 1_000_000.0;

        System.out.println("Available processors: " + Runtime.getRuntime().availableProcessors());
        System.out.printf("Orders: %d%n", n);
        System.out.printf("Sequential: %.1f ms (sum=%.2f)%n", seqMs, sumSeq);
        System.out.printf("Parallel:   %.1f ms (sum=%.2f)%n", parMs, sumPar);
        System.out.printf("Speedup: %.2fx%n", seqMs / parMs);
    }
}
