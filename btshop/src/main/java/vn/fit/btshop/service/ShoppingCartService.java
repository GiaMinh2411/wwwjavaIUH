package vn.fit.btshop.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import vn.fit.btshop.dto.AddCartResponseDTO;
import vn.fit.btshop.dto.CartItemDTO;
import vn.fit.btshop.dto.CustomerBillDTO;
import vn.fit.btshop.model.Product;
import vn.fit.btshop.model.ShoppingCart;
import vn.fit.btshop.repository.ProductRepository;
import vn.fit.btshop.repository.ShoppingCartRepository;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class ShoppingCartService {

    @Inject
    private ShoppingCartRepository cartRepository;

    @Inject
    private ProductRepository productRepository;

    public List<ShoppingCart> getAllCarts() {
        return cartRepository.findAll();
    }

    public ShoppingCart getCartById(int id) {
        return cartRepository.findById(id);
    }

    public boolean createCart(ShoppingCart cart) {
        return cartRepository.save(cart);
    }

    public boolean updateCart(ShoppingCart cart) {
        return cartRepository.update(cart);
    }

    public boolean deleteCart(int id) {
        return cartRepository.delete(id);
    }

    // YÊU CẦU 1: Tính tổng hóa đơn
    public CustomerBillDTO calculateCustomerBill(String customerName) {
        List<ShoppingCart> carts = cartRepository.findByCustomerName(customerName);
        List<CartItemDTO> itemDTOs = new ArrayList<>();
        double subTotal = 0.0;

        for (ShoppingCart cart : carts) {
            Product product = productRepository.findById(cart.getProductId());
            if (product != null) {
                double itemTotal = Math.round(product.getPrice() * cart.getQuantity() * 100.0) / 100.0;
                subTotal += itemTotal;
                itemDTOs.add(new CartItemDTO(
                        cart.getId(),
                        product.getName(),
                        product.getPrice(),
                        cart.getQuantity(),
                        itemTotal
                ));
            }
        }

        subTotal = Math.round(subTotal * 100.0) / 100.0;
        double discount = 0.0;
        if (subTotal > 2000.0) {
            discount = Math.round(subTotal * 0.1 * 100.0) / 100.0;
        }
        double finalTotal = Math.round((subTotal - discount) * 100.0) / 100.0;

        CustomerBillDTO bill = new CustomerBillDTO();
        bill.setCustomerName(customerName);
        bill.setItems(itemDTOs);
        bill.setSubTotal(subTotal);
        bill.setDiscount(discount);
        bill.setFinalTotal(finalTotal);

        return bill;
    }

    // YÊU CẦU 2: Thêm giỏ hàng có kiểm tra V alue Cap
    public AddCartResponseDTO addItemToCart(ShoppingCart cart) {
        Product product = productRepository.findById(cart.getProductId());
        if (product == null) {
            throw new IllegalArgumentException("Sản phẩm không tồn tại!");
        }

        double estimatedTotal = Math.round(product.getPrice() * cart.getQuantity() * 100.0) / 100.0;
        if (estimatedTotal > 5000.0) {
            throw new IllegalStateException("Giá trị đơn hàng vượt quá hạn mức cho phép ($5,000)!");
        }

        boolean saved = cartRepository.save(cart);
        if (!saved) {
            throw new RuntimeException("Lưu sản phẩm vào giỏ hàng thất bại!");
        }

        return new AddCartResponseDTO(
                cart.getId(),
                product.getName(),
                cart.getQuantity(),
                estimatedTotal
        );
    }

    // YÊU CẦU 4: Chuyển đổi giỏ hàng thành đơn đặt mua và Xóa giỏ (Checkout & Clear Cart)
    public vn.fit.btshop.dto.CheckoutSummaryDTO checkout(String customerName) {
        List<ShoppingCart> carts = cartRepository.findByCustomerName(customerName);
        if (carts == null || carts.isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng của khách hàng " + customerName + " đang trống!");
        }

        int totalItems = carts.size();
        int totalQuantity = 0;
        double totalPaid = 0.0;

        for (ShoppingCart cart : carts) {
            totalQuantity += cart.getQuantity();
            Product p = productRepository.findById(cart.getProductId());
            if (p != null) {
                totalPaid += p.getPrice() * cart.getQuantity();
            }
        }

        totalPaid = Math.round(totalPaid * 100.0) / 100.0;

        // Xóa sạch các bản ghi giỏ hàng của khách hàng này
        cartRepository.deleteByCustomerName(customerName);

        return new vn.fit.btshop.dto.CheckoutSummaryDTO(
                customerName,
                totalItems,
                totalQuantity,
                totalPaid,
                "CHECKOUT_COMPLETED_AND_CART_CLEARED"
        );
    }
}