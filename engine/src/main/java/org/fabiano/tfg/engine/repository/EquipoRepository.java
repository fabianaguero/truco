package org.fabiano.tfg.engine.repository;


import org.fabiano.tfg.engine.model.team.Equipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EquipoRepository extends JpaRepository<Equipo, UUID> {
}