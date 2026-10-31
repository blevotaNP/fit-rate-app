package com.outfitrating.repo;

import com.outfitrating.domain.Brand;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    Optional<Brand> findBySlug(String slug);

    Optional<Brand> findByNameIgnoreCase(String name);

    List<Brand> findAllByOrderByNameAsc();
}
