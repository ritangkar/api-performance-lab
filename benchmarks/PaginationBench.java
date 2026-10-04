import java.util.*;

public class PaginationBench {
    record Product(long id, String name, String category, double price, int stock, String description) {}

    static String toJson(Product p) {
        return "{\"id\":" + p.id() + ",\"name\":\"" + p.name() + "\",\"category\":\"" + p.category()
                + "\",\"price\":" + p.price() + ",\"stock\":" + p.stock() + ",\"description\":\"" + p.description() + "\"}";
    }

    public static void main(String[] args) {
        int n = 50_000;
        Random rng = new Random(11);
        String[] categories = {"Footwear", "Apparel", "Accessories", "Outdoor Gear", "Electronics"};
        List<Product> products = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            products.add(new Product(i, "Product " + i, categories[rng.nextInt(categories.length)],
                    round2(200 + rng.nextDouble() * 8000), rng.nextInt(200),
                    "A synthetic catalog item used for pagination benchmarking purposes only."));
        }

        // "Return everything" endpoint
        long t0 = System.nanoTime();
        StringBuilder allJson = new StringBuilder("[");
        for (int i = 0; i < products.size(); i++) {
            allJson.append(toJson(products.get(i)));
            if (i < products.size() - 1) allJson.append(",");
        }
        allJson.append("]");
        long t1 = System.nanoTime();

        // Paginated endpoint: page=0, size=20
        int pageSize = 20;
        long t2 = System.nanoTime();
        StringBuilder pageJson = new StringBuilder("[");
        for (int i = 0; i < pageSize; i++) {
            pageJson.append(toJson(products.get(i)));
            if (i < pageSize - 1) pageJson.append(",");
        }
        pageJson.append("]");
        long t3 = System.nanoTime();

        System.out.println("Catalog size: " + n + " products");
        System.out.printf("Return-everything: %.2f ms to serialize, %,d bytes payload%n",
                (t1 - t0) / 1_000_000.0, allJson.length());
        System.out.printf("Paginated (size=20): %.3f ms to serialize, %,d bytes payload%n",
                (t3 - t2) / 1_000_000.0, pageJson.length());
        System.out.printf("Payload size reduction: %.0fx smaller%n", (double) allJson.length() / pageJson.length());
    }

    static double round2(double v) { return Math.round(v * 100) / 100.0; }
}
