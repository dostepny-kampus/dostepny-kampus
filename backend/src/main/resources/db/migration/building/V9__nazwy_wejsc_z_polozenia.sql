-- Dostępny Kampus
-- Author: Vladyslav Yevkov
-- Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko

-- Nazwa z położenia wejścia bez nazwy i opisu dojścia w danych (BR-27). Typ main daje
-- „Wejście główne”; inaczej najbliższy inny budynek do 40 m i strona świata od środka
-- konturu. Ta sama nazwa w kilku miejscach budynku (drzwi do 12 m to jedno miejsce)
-- dostaje numer w kolejności azymutu od północnego zachodu.
CREATE VIEW entrance_place_names AS
WITH unnamed AS (
    SELECT entrance.id, entrance.building_symbol, entrance.location, entrance.type,
        CAST(floor(COALESCE(degrees(ST_Azimuth(
            ST_Centroid(building.outline)::geography, entrance.location::geography
        )), 0)) AS INTEGER) AS azimuth
    FROM entrances AS entrance
    JOIN buildings AS building ON building.symbol = entrance.building_symbol
    WHERE entrance.pwr_name IS NULL AND entrance.name IS NULL AND entrance.approach IS NULL
        AND COALESCE(entrance.type, '') NOT IN ('emergency', 'exit', 'garage')
), based AS (
    SELECT unnamed.id, unnamed.building_symbol, unnamed.location,
        (unnamed.azimuth + 45) % 360 AS along,
        CASE WHEN unnamed.type = 'main' THEN 'Wejście główne'
            ELSE 'Wejście ' || COALESCE('od strony ' || near.symbol || ', ', '')
                || (ARRAY['od północy', 'od wschodu', 'od południa', 'od zachodu'])
                    [(unnamed.azimuth + 45) % 360 / 90 + 1]
        END AS base
    FROM unnamed
    LEFT JOIN LATERAL (
        SELECT other.symbol
        FROM buildings AS other
        WHERE other.symbol <> unnamed.building_symbol
            AND ST_DWithin(other.outline::geography, unnamed.location::geography, 40)
        ORDER BY ST_Distance(other.outline::geography, unnamed.location::geography),
            other.symbol
        LIMIT 1
    ) AS near ON TRUE
), spots AS (
    SELECT based.*, ST_ClusterDBSCAN(ST_Transform(based.location, 2180), 12, 1)
        OVER (PARTITION BY based.building_symbol, based.base) AS spot
    FROM based
), ordered AS (
    SELECT spots.*,
        MIN(spots.along) OVER (PARTITION BY spots.building_symbol, spots.base, spots.spot)
            AS first_along
    FROM spots
), numbered AS (
    SELECT ordered.*, DENSE_RANK() OVER (
        PARTITION BY ordered.building_symbol, ordered.base
        ORDER BY ordered.first_along, ordered.spot
    ) AS place
    FROM ordered
)
SELECT numbered.id, numbered.building_symbol,
    numbered.base || CASE
        WHEN MAX(numbered.place) OVER (PARTITION BY numbered.building_symbol, numbered.base) > 1
            THEN ' (' || numbered.place || ')'
        ELSE '' END AS place_name
FROM numbered;

COMMENT ON VIEW entrance_place_names IS 'Nazwa z położenia wejścia bez nazwy i opisu dojścia w danych (BR-27, BR-25)';
