package com.mycomplex.properties.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mycomplex.properties.application.port.out.LoadAvailableResidentialUnitsPort;
import com.mycomplex.properties.domain.model.AvailableResidentialUnit;
import com.mycomplex.properties.domain.model.PropertyStructureType;
import com.mycomplex.properties.domain.model.ResidentialUnitType;
import com.mycomplex.properties.domain.model.ResidentialUnitStatus;
import com.mycomplex.properties.domain.model.ResidentialUnitSummary;

@Component
public class ResidentialUnitQueryPersistenceAdapter
        implements LoadAvailableResidentialUnitsPort {

    private final JdbcTemplate jdbcTemplate;

    public ResidentialUnitQueryPersistenceAdapter(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailableResidentialUnit> loadByPropertyId(
            UUID propertyId
    ) {
        return jdbcTemplate.query(
                """
                SELECT unidad.id,
                       estructura.id,
                       estructura.nombre,
                       estructura.tipo,
                       unidad.numero,
                       unidad.tipo
                FROM unidades_residenciales unidad
                LEFT JOIN estructuras_copropiedad estructura
                  ON estructura.id = unidad.estructura_id
                 AND estructura.copropiedad_id = unidad.copropiedad_id
                 AND estructura.estado = 'ACTIVA'
                 AND estructura.deleted_at IS NULL
                WHERE unidad.copropiedad_id = ?
                  AND unidad.estado = 'ACTIVA'
                  AND unidad.deleted_at IS NULL
                  AND (
                      unidad.estructura_id IS NULL
                      OR estructura.id IS NOT NULL
                  )
                ORDER BY estructura.nombre, unidad.numero
                """,
                (resultSet, rowNumber) ->
                        new AvailableResidentialUnit(
                                resultSet.getObject(1, UUID.class),
                                resultSet.getObject(2, UUID.class),
                                resultSet.getString(3),
                                mapStructureType(
                                        resultSet.getString(4)
                                ),
                                resultSet.getString(5),
                                mapUnitType(resultSet.getString(6))
                        ),
                propertyId
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResidentialUnitSummary> loadByPropertyIdAndUnitIds(
            UUID propertyId,
            Set<UUID> unitIds
    ) {
        String placeholders = String.join(
                ", ",
                Collections.nCopies(unitIds.size(), "?")
        );
        List<Object> parameters = new ArrayList<>();
        parameters.add(propertyId);
        parameters.addAll(unitIds);

        return jdbcTemplate.query(
                """
                SELECT unidad.id,
                       unidad.copropiedad_id,
                       estructura.id,
                       estructura.nombre,
                       estructura.tipo,
                       unidad.numero,
                       unidad.tipo
                FROM unidades_residenciales unidad
                LEFT JOIN estructuras_copropiedad estructura
                  ON estructura.id = unidad.estructura_id
                 AND estructura.copropiedad_id = unidad.copropiedad_id
                 AND estructura.estado = 'ACTIVA'
                 AND estructura.deleted_at IS NULL
                WHERE unidad.copropiedad_id = ?
                  AND unidad.id IN (%s)
                  AND unidad.estado = 'ACTIVA'
                  AND unidad.deleted_at IS NULL
                  AND (
                      unidad.estructura_id IS NULL
                      OR estructura.id IS NOT NULL
                  )
                """.formatted(placeholders),
                (resultSet, rowNumber) -> new ResidentialUnitSummary(
                        resultSet.getObject(1, UUID.class),
                        resultSet.getObject(2, UUID.class),
                        resultSet.getObject(3, UUID.class),
                        resultSet.getString(4),
                        mapStructureType(resultSet.getString(5)),
                        resultSet.getString(6),
                        mapUnitType(resultSet.getString(7)),
                        ResidentialUnitStatus.ACTIVE
                ),
                parameters.toArray()
        );
    }

    private PropertyStructureType mapStructureType(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "TORRE" -> PropertyStructureType.TOWER;
            case "BLOQUE" -> PropertyStructureType.BLOCK;
            case "SECTOR" -> PropertyStructureType.SECTOR;
            case "OTRO" -> PropertyStructureType.OTHER;
            default -> throw new IllegalArgumentException(
                    "Unsupported structure type: " + value
            );
        };
    }

    private ResidentialUnitType mapUnitType(String value) {
        return switch (value) {
            case "APARTAMENTO" -> ResidentialUnitType.APARTMENT;
            case "CASA" -> ResidentialUnitType.HOUSE;
            case "LOCAL" -> ResidentialUnitType.COMMERCIAL_SPACE;
            case "OFICINA" -> ResidentialUnitType.OFFICE;
            case "OTRO" -> ResidentialUnitType.OTHER;
            default -> throw new IllegalArgumentException(
                    "Unsupported unit type: " + value
            );
        };
    }
}
