package ph.darch.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ph.darch.api.entity.MediaAsset;

import java.util.Optional;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, Long> {

    Optional<MediaAsset> findByPublicUrl(String publicUrl);
}
