package com.revshop.dao;

import com.revshop.entity.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryDAO {

    Category save(Category category);

    Optional<Category> findById(Long id);

    Optional<Category> findBySlug(String slug);

    Optional<Category> findAnyByName(String name);

    List<Category> findAllActive();

    List<Category> findAllActiveWithParent();

    boolean existsByName(String name);

    long countActiveChildren(Long parentId);
}
