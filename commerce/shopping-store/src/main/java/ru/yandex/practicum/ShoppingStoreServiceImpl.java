package ru.yandex.practicum;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShoppingStoreServiceImpl implements ShoppingStoreService {
    private final ShoppingStoreRepository shoppingStoreRepository;
    private List<String> sortStrings;

    @Override
    public Page<ProductDto> getProducts(Pageable pageable, ProductCategory category) {
        return shoppingStoreRepository.findAllByProductCategory(pageableConverter(pageable), category);
    }

    @Override
    public ProductDto createProduct(ProductDto productDto) {
        return shoppingStoreRepository.save(productDto);
    }

    @Override
    public ProductDto updateProduct(ProductDto productDto) {
        return shoppingStoreRepository.save(productDto);
    }

    @Override
    public boolean deleteProduct(String productId) {
        return shoppingStoreRepository.deleteByProductId(productId);
    }

    @Override
    public boolean updateQuantityStateProduct(SetProductQuantityStateRequest setProductQuantityStateRequest) {
        final ProductDto productDto = shoppingStoreRepository.getProductDtoByProductId(setProductQuantityStateRequest.getProductId());
        productDto.setQuantityState(setProductQuantityStateRequest.getQuantityState());
        shoppingStoreRepository.save(productDto);
        return true;
    }

    @Override
    public ProductDto getProduct(String productId) {
        return shoppingStoreRepository.getProductDtoByProductId(productId);
    }

    private org.springframework.data.domain.Pageable pageableConverter(Pageable customPageable) {
        if (customPageable == null) {
            return PageRequest.of(0, 20); // Значения по умолчанию
        }

        // Валидация page и size
        int page = customPageable.getPage();
        int size = customPageable.getSize();

        // Создание объекта сортировки
        Sort sort = createSort(customPageable.getSort());

        return PageRequest.of(page, size, sort);
    }

    private Sort createSort(List<String> sortStrings) {
        if (sortStrings == null || sortStrings.isEmpty()) {
            return Sort.unsorted();
        }

        List<Sort.Order> orders = new ArrayList<>();

        for (String sortString : sortStrings) {
            if (!StringUtils.hasText(sortString)) {
                continue;
            }

            Sort.Order order = parseSortOrder(sortString.trim());
            if (order != null) {
                orders.add(order);
            }
        }
        return orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
    }

    /**
     * Парсит строку сортировки в объект Order
     */
    private Sort.Order parseSortOrder(String sortString) {
        String[] parts = sortString.split(",");

        if (parts.length == 0 || !StringUtils.hasText(parts[0])) {
            return null;
        }

        String field = parts[0].trim();

        // По умолчанию сортировка по возрастанию
        Sort.Direction direction = Sort.Direction.ASC;

        if (parts.length >= 2 && StringUtils.hasText(parts[1])) {
            String dir = parts[1].trim().toUpperCase();
            if ("DESC".equals(dir)) {
                direction = Sort.Direction.DESC;
            } else if (!"ASC".equals(dir)) {
                throw new IllegalArgumentException(
                        String.format("Invalid sort direction: %s. Use 'asc' or 'desc'", parts[1])
                );
            }
        }

        return new Sort.Order(direction, field);
    }
}
