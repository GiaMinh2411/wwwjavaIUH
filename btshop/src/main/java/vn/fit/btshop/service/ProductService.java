package vn.fit.btshop.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import vn.fit.btshop.dto.RepriceReportDTO;
import vn.fit.btshop.model.Product;
import vn.fit.btshop.model.ShoppingCart;
import vn.fit.btshop.repository.ProductRepository;
import vn.fit.btshop.repository.ShoppingCartRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class ProductService {

    @Inject
    private ProductRepository productRepository;

    @Inject
    private ShoppingCartRepository cartRepository;

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProductById(int id) {
        return productRepository.findById(id);
    }

    public boolean createProduct(Product product) {
        return productRepository.save(product);
    }

    public boolean updateProduct(Product product) {
        return productRepository.update(product);
    }

    public boolean deleteProduct(int id) {
        return productRepository.delete(id);
    }

    // YÊU CẦU 3: Dynamic Repricing
    public List<RepriceReportDTO> applyDynamicRepricing() {
        List<Product> products = productRepository.findAll();
        List<ShoppingCart> carts = cartRepository.findAll();

        Map<Integer, Integer> productQtyMap = carts.stream()
                .collect(Collectors.groupingBy(
                        ShoppingCart::getProductId,
                        Collectors.summingInt(ShoppingCart::getQuantity)
                ));

        List<RepriceReportDTO> report = new ArrayList<>();

        for (Product product : products) {
            int totalQty = productQtyMap.getOrDefault(product.getId(), 0);
            double oldPrice = product.getPrice();
            double newPrice = oldPrice;
            String adjustment = "";
            boolean isChanged = false;

            if (totalQty >= 5) {
                newPrice = Math.round(oldPrice * 1.10 * 100.0) / 100.0;
                adjustment = "+10%";
                isChanged = true;
            } else if (totalQty == 0) {
                newPrice = Math.round(oldPrice * 0.95 * 100.0) / 100.0;
                adjustment = "-5%";
                isChanged = true;
            }

            if (isChanged) {
                product.setPrice(newPrice);
                productRepository.update(product);
                report.add(new RepriceReportDTO(
                        product.getId(),
                        product.getName(),
                        totalQty,
                        oldPrice,
                        newPrice,
                        adjustment
                ));
            }
        }
        return report;
    }

    // YÊU CẦU 5: Thống kê Doanh thu dự kiến & Đóng góp % từng danh mục sản phẩm (Revenue Share Analytics)
    public List<vn.fit.btshop.dto.RevenueShareDTO> getRevenueShareReport() {
        List<Product> products = productRepository.findAll();
        List<ShoppingCart> carts = cartRepository.findAll();

        Map<Integer, Integer> productQtyMap = carts.stream()
                .collect(Collectors.groupingBy(
                        ShoppingCart::getProductId,
                        Collectors.summingInt(ShoppingCart::getQuantity)
                ));

        Map<Integer, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        double totalRevenue = 0.0;
        List<vn.fit.btshop.dto.RevenueShareDTO> report = new ArrayList<>();

        for (Map.Entry<Integer, Integer> entry : productQtyMap.entrySet()) {
            int productId = entry.getKey();
            int totalUnits = entry.getValue();
            Product p = productMap.get(productId);
            if (p != null) {
                double revenue = Math.round(p.getPrice() * totalUnits * 100.0) / 100.0;
                totalRevenue += revenue;
                report.add(new vn.fit.btshop.dto.RevenueShareDTO(
                        productId,
                        p.getName(),
                        totalUnits,
                        revenue,
                        ""
                ));
            }
        }

        final double finalTotalRev = totalRevenue;
        for (vn.fit.btshop.dto.RevenueShareDTO item : report) {
            double percent = (finalTotalRev > 0) ? (item.getRevenueContribution() / finalTotalRev) * 100.0 : 0.0;
            item.setPercentage(String.format(java.util.Locale.US, "%.2f%%", percent));
        }

        // Sắp xếp giảm dần theo revenueContribution
        report.sort((a, b) -> Double.compare(b.getRevenueContribution(), a.getRevenueContribution()));

        return report;
    }
}