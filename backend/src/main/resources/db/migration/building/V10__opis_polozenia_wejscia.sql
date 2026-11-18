-- Dostępny Kampus
-- Author: Vladyslav Yevkov
-- Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko

-- Wejście bez nazwy w danych nazywa się „Wejście”, a typ main „Wejście główne” (BR-27).
-- Położenie to osobny opis, także przy opisie dojścia: najbliższy inny budynek do 40 m
-- i strona świata od środka konturu, bez numerów.
DROP VIEW entrance_place_names;

CREATE VIEW entrance_places AS
WITH unnamed AS (
    SELECT entrance.id, entrance.building_symbol, entrance.location, entrance.type,
        CAST(floor(COALESCE(degrees(ST_Azimuth(
            ST_Centroid(building.outline)::geography, entrance.location::geography
        )), 0)) AS INTEGER) AS azimuth
    FROM entrances AS entrance
    JOIN buildings AS building ON building.symbol = entrance.building_symbol
    WHERE entrance.pwr_name IS NULL AND entrance.name IS NULL
        AND COALESCE(entrance.type, '') NOT IN ('emergency', 'exit', 'garage')
)
SELECT unnamed.id, unnamed.building_symbol,
    CASE WHEN unnamed.type = 'main' THEN 'Wejście główne' ELSE 'Wejście' END AS place_name,
    COALESCE('Od strony ' || near.symbol || ', od ', 'Od ')
        || (ARRAY['północy', 'wschodu', 'południa', 'zachodu'])
            [(unnamed.azimuth + 45) % 360 / 90 + 1]
        || '.' AS place_description
FROM unnamed
LEFT JOIN LATERAL (
    SELECT other.symbol
    FROM buildings AS other
    WHERE other.symbol <> unnamed.building_symbol
        AND ST_DWithin(other.outline::geography, unnamed.location::geography, 40)
    ORDER BY ST_Distance(other.outline::geography, unnamed.location::geography),
        other.symbol
    LIMIT 1
) AS near ON TRUE;

COMMENT ON VIEW entrance_places IS 'Nazwa „Wejście” i opis położenia wejścia bez nazwy w danych (BR-27, BR-25)';
