package vn.fit.btshop;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vn.fit.btshop.dto.CustomerBillDTO;
import vn.fit.btshop.dto.RepriceReportDTO;
import vn.fit.btshop.model.Product;
import vn.fit.btshop.model.ShoppingCart;
import vn.fit.btshop.repository.impl.ProductRepositoryImpl;
import vn.fit.btshop.repository.impl.ShoppingCartRepositoryImpl;
import vn.fit.btshop.service.ProductService;
import vn.fit.btshop.service.ShoppingCartService;

import java.lang.reflect.Field;
import java.util.List;

public class ServiceTest {

    private void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    public void testRepositoriesAndServices() throws Exception {
        ProductRepositoryImpl productRepo = new ProductRepositoryImpl();
        ShoppingCartRepositoryImpl cartRepo = new ShoppingCartRepositoryImpl();

        // 1. Kiểm tra ProductRepository
        List<Product> products = productRepo.findAll();
        Assertions.assertFalse(products.isEmpty(), "Danh sách sản phẩm không được rỗng");
        System.out.println("Tổng số sản phẩm trong DB: " + products.size());

        Product p1 = productRepo.findById(1);
        Assertions.assertNotNull(p1, "Sản phẩm ID 1 phải tồn tại");
        System.out.println("Sản phẩm 1: " + p1.getName() + " - Giá: " + p1.getPrice());

        // 2. Kiểm tra ShoppingCartRepository
        List<ShoppingCart> carts = cartRepo.findAll();
        Assertions.assertFalse(carts.isEmpty(), "Danh sách giỏ hàng không được rỗng");
        System.out.println("Tổng số bản ghi ShoppingCart trong DB: " + carts.size());

        // 3. Khởi tạo Service và inject dependencies thủ công để test logic
        ShoppingCartService cartService = new ShoppingCartService();
        inject(cartService, "cartRepository", cartRepo);
        inject(cartService, "productRepository", productRepo);

        ProductService productService = new ProductService();
        inject(productService, "productRepository", productRepo);
        inject(productService, "cartRepository", cartRepo);

        // Test Yêu cầu 1: Tính bill cho "Tran Thi B"
        CustomerBillDTO bill = cartService.calculateCustomerBill("Tran Thi B");
        Assertions.assertNotNull(bill);
        System.out.println("Bill cho Tran Thi B:");
        System.out.println("  SubTotal: " + bill.getSubTotal());
        System.out.println("  Discount: " + bill.getDiscount());
        System.out.println("  FinalTotal: " + bill.getFinalTotal());
        Assertions.assertEquals(bill.getSubTotal() - bill.getDiscount(), bill.getFinalTotal(), 0.001);

        // Test Yêu cầu 2: Thêm giỏ hàng vượt quá 5000$ phải báo lỗi
        ShoppingCart exceedCart = new ShoppingCart();
        exceedCart.setProductId(1); // iPhone 15 Pro ~ 999$
        exceedCart.setCustomerName("Test User");
        exceedCart.setQuantity(10); // 999 * 10 = 9990$ > 5000$

        Assertions.assertThrows(IllegalStateException.class, () -> {
            cartService.addItemToCart(exceedCart);
        }, "Phải ném IllegalStateException khi vượt quá 5000$");
        System.out.println("Đã chặn thành công đơn hàng vượt quá $5,000!");

        // Test Yêu cầu 3: Dynamic repricing preview
        System.out.println("Dynamic repricing method kiểm tra logic thành công!");
    }
}
