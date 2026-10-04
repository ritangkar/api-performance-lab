import java.util.*;
import java.util.concurrent.*;

public class CacheBench {
    // Simulates an expensive lookup (e.g., a DB join + computation) with real CPU work, not Thread.sleep.
    static double expensiveProductScore(long productId) {
        double acc = 0;
        for (int i = 0; i < 2_000_000; i++) {
            acc += Math.sqrt(productId + i) * Math.sin(i);
        }
        return acc;
    }

    public static void main(String[] args) {
        int uniqueProducts = 30;
        int totalRequests = 300; // simulate repeated lookups of the same hot products, as real traffic does
        Random rng = new Random(3);
        List<Long> requestSequence = new ArrayList<>();
        for (int i = 0; i < totalRequests; i++) requestSequence.add((long) rng.nextInt(uniqueProducts));

        // WITHOUT cache: recompute every time
        long t0 = System.nanoTime();
        double sinkA = 0;
        for (long id : requestSequence) sinkA += expensiveProductScore(id);
        long t1 = System.nanoTime();

        // WITH cache: simple ConcurrentHashMap memoization, same as Spring's @Cacheable would do
        Map<Long, Double> cache = new ConcurrentHashMap<>();
        long t2 = System.nanoTime();
        double sinkB = 0;
        for (long id : requestSequence) sinkB += cache.computeIfAbsent(id, CacheBench::expensiveProductScore);
        long t3 = System.nanoTime();

        System.out.println("Unique products: " + uniqueProducts + ", total requests: " + totalRequests);
        System.out.printf("Without cache: %.1f ms total, %.2f ms/request avg (sink=%.1f)%n",
                (t1 - t0) / 1e6, (t1 - t0) / 1e6 / totalRequests, sinkA);
        System.out.printf("With cache:    %.1f ms total, %.2f ms/request avg (sink=%.1f)%n",
                (t3 - t2) / 1e6, (t3 - t2) / 1e6 / totalRequests, sinkB);
        System.out.printf("Speedup: %.1fx%n", (double) (t1 - t0) / (t3 - t2));
        System.out.println("Cache size after run: " + cache.size() + " (== unique products, as expected)");
    }
}
