package ph.darch.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ph.darch.api.entity.MediaType;
import ph.darch.api.entity.ProductMedia;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductMediaRepository extends JpaRepository<ProductMedia, Long> {

    List<ProductMedia> findByProductIdOrderByPositionAsc(Long productId);

    List<ProductMedia> findByProductIdInOrderByProductIdAscPositionAsc(Collection<Long> productIds);

    Optional<ProductMedia> findByProductIdAndMediaType(Long productId, MediaType mediaType);

    boolean existsByMediaAssetId(Long mediaAssetId);
}
