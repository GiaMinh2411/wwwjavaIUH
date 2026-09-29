package vn.fit.btshop.repository;

import vn.fit.btshop.model.ShoppingCart;
import java.util.List;

public interface ShoppingCartRepository {
    List<ShoppingCart> findAll();
    ShoppingCart findById(int id);
    List<ShoppingCart> findByCustomerName(String customerName);
    boolean save(ShoppingCart cart);
    boolean update(ShoppingCart cart);
    boolean delete(int id);
    boolean deleteByCustomerName(String customerName);
}