-- Dostępny Kampus
-- Author: Vladyslav Yevkov
-- Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko

-- Budynek może nie mieć nazwy. Nazwy będące samymi symbolami budynków
-- (np. C-13, C-3/C-4) znikają przy ponownym imporcie konturów (OsmBuildings).
ALTER TABLE buildings ALTER COLUMN name DROP NOT NULL;
