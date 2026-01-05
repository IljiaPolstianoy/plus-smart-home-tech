package ru.yandex.practicum;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.yandex.practicum.model.Pageable;
import ru.yandex.practicum.model.error.ProductNotFoundException;
import ru.yandex.practicum.model.product.ProductCategory;
import ru.yandex.practicum.model.product.ProductDto;
import ru.yandex.practicum.model.product.ProductState;
import ru.yandex.practicum.model.quantity.SetProductQuantityStateRequest;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShoppingStoreServiceImpl implements ShoppingStoreService {
    private final ShoppingStoreRepository shoppingStoreRepository;

    @Override
    public Page<ProductDto> getProducts(final Pageable pageable, final ProductCategory category) {
        return shoppingStoreRepository.findAllByProductCategory(pageableConverter(pageable), category);
    }

    @Override
    public ProductDto createProduct(final ProductDto productDto) {
        return shoppingStoreRepository.save(productDto);
    }

    @Override
    public ProductDto updateProduct(final ProductDto productDto) {
        return shoppingStoreRepository.save(productDto);
    }

    @Override
    public boolean deleteProduct(final String productId) {
        final ProductDto productDto = shoppingStoreRepository.getProductDtoByProductId(productId).orElseThrow(() -> new ProductNotFoundException(
                "Товар с ID '" + productId + "' не найден",
                "Искомый товар отсутствует в системе. Проверьте ID.",
                "404 NOT_FOUND"
        ));
        productDto.setProductState(ProductState.DEACTIVATE);
        shoppingStoreRepository.save(productDto);
        return true;
    }

    @Override
    public boolean updateQuantityStateProduct(final SetProductQuantityStateRequest setProductQuantityStateRequest) {
        final ProductDto productDto = shoppingStoreRepository.getProductDtoByProductId(setProductQuantityStateRequest.getProductId())
                .orElseThrow(() -> new ProductNotFoundException(
                        "Товар с ID '" + setProductQuantityStateRequest.getProductId() + "' не найден",
                        "Искомый товар отсутствует в системе. Проверьте ID.",
                        "404 NOT_FOUND"
                ));
        productDto.setQuantityState(setProductQuantityStateRequest.getQuantityState());
        shoppingStoreRepository.save(productDto);
        return true;
    }

    @Override
    public ProductDto getProduct(final String productId) {
        return shoppingStoreRepository.getProductDtoByProductId(productId).orElseThrow(() -> new ProductNotFoundException(
                "Товар с ID '" + productId + "' не найден",
                "Искомый товар отсутствует в системе. Проверьте ID.",
                "404 NOT_FOUND"
        ));
    }

    private org.springframework.data.domain.Pageable pageableConverter(final Pageable customPageable) {
        if (customPageable == null) {
            return PageRequest.of(0, 20); // Значения по умолчанию
        }

        // Валидация page и size
        final int page = customPageable.getPage();
        final int size = customPageable.getSize();

        // Создание объекта сортировки
        final Sort sort = createSort(customPageable.getSort());

        return PageRequest.of(page, size, sort);
    }

    private Sort createSort(final List<String> sortStrings) {
        if (sortStrings == null || sortStrings.isEmpty()) {
            return Sort.unsorted();
        }

        final List<Sort.Order> orders = new ArrayList<>();

        for (String sortString : sortStrings) {
            if (!StringUtils.hasText(sortString)) {
                continue;
            }

           final Sort.Order order = parseSortOrder(sortString.trim());
            if (order != null) {
                orders.add(order);
            }
        }
        return orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
    }

    /**
     * Парсит строку сортировки в объект Order
     */
    private Sort.Order parseSortOrder(final String sortString) {
        final String[] parts = sortString.split(",");

        if (parts.length == 0 || !StringUtils.hasText(parts[0])) {
            return null;
        }

        final String field = parts[0].trim();

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
