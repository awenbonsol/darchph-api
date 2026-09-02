package ph.darch.api.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ph.darch.api.entity.Product;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Page<Product> findByIsActiveTrue(Pageable pageable);

    Page<Product> findByIsActiveTrueAndFeaturedTrue(Pageable pageable);

    Optional<Product> findBySlugAndIsActiveTrue(String slug);

    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("""
            SELECT p FROM Product p
            WHERE p.isActive = true
              AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :term, '%'))
                   OR LOWER(p.description) LIKE LOWER(CONCAT('%', :term, '%')))
            """)
    Page<Product> searchActiveByTerm(@Param("term") String term, Pageable pageable);

    @Query("""
            SELECT p FROM Product p
            WHERE p.isActive = true
              AND p.featured = true
              AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :term, '%'))
                   OR LOWER(p.description) LIKE LOWER(CONCAT('%', :term, '%')))
            """)
    Page<Product> searchActiveFeaturedByTerm(@Param("term") String term, Pageable pageable);
}
