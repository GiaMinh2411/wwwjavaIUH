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

public class ShopServiceTest {

    @Test
    public void testDatabaseAndRepositories() {
        ProductRepositoryImpl productRepo = new ProductRepositoryImpl();
        ShoppingCartRepositoryImpl cartRepo = new ShoppingCartRepositoryImpl();

        List<Product> products = productRepo.findAll();
        Assertions.assertFalse(products.isEmpty(), "Product list should not be empty");

        Product p1 = productRepo.findById(1);
        Assertions.assertNotNull(p1, "Product 1 should exist");

        List<ShoppingCart> carts = cartRepo.findAll();
        Assertions.assertFalse(carts.isEmpty(), "Cart list should not be empty");
    }

    @Test
    public void testCalculateCustomerBill() throws Exception {
        ShoppingCartService cartService = new ShoppingCartService();
        ProductRepositoryImpl productRepo = new ProductRepositoryImpl();
        ShoppingCartRepositoryImpl cartRepo = new ShoppingCartRepositoryImpl();

        injectField(cartService, "productRepository", productRepo);
        injectField(cartService, "cartRepository", cartRepo);

        CustomerBillDTO bill = cartService.calculateCustomerBill("Nguyen Van A");
        Assertions.assertNotNull(bill);
        Assertions.assertEquals("Nguyen Van A", bill.getCustomerName());
        Assertions.assertFalse(bill.getItems().isEmpty());
    }

    @Test
    public void testAddCartWithCapConstraint() throws Exception {
        ShoppingCartService cartService = new ShoppingCartService();
        ProductRepositoryImpl productRepo = new ProductRepositoryImpl();
        ShoppingCartRepositoryImpl cartRepo = new ShoppingCartRepositoryImpl();

        injectField(cartService, "productRepository", productRepo);
        injectField(cartService, "cartRepository", cartRepo);

        // Test over cap (> $5,000)
        ShoppingCart overCapCart = new ShoppingCart();
        overCapCart.setProductId(1); // iPhone 15 Pro ($999)
        overCapCart.setCustomerName("Test User");
        overCapCart.setQuantity(10); // 10 * 999 = $9,990 > $5,000

        Assertions.assertThrows(IllegalStateException.class, () -> {
            cartService.addItemToCart(overCapCart);
        });
    }

    @Test
    public void testDynamicRepricing() throws Exception {
        ProductService productService = new ProductService();
        ProductRepositoryImpl productRepo = new ProductRepositoryImpl();
        ShoppingCartRepositoryImpl cartRepo = new ShoppingCartRepositoryImpl();

        injectField(productService, "productRepository", productRepo);
        injectField(productService, "cartRepository", cartRepo);

        List<RepriceReportDTO> report = productService.applyDynamicRepricing();
        Assertions.assertNotNull(report);
    }

    private void injectField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
