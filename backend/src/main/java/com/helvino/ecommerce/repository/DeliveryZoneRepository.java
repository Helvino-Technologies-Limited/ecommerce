package com.helvino.ecommerce.repository;

import com.helvino.ecommerce.entity.DeliveryZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryZoneRepository extends JpaRepository<DeliveryZone, UUID> {

    List<DeliveryZone> findByCountyIgnoreCaseAndActiveTrue(String county);

    List<DeliveryZone> findAllByActiveTrue();

    Optional<DeliveryZone> findByCountyIgnoreCaseAndTownIgnoreCase(String county, String town);
}
