/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

internal const val BUILDING_OUTLINE_JSON =
    """{"type":"Polygon","coordinates":[[[17,51],[18,51],[18,52],[17,52],[17,51]]]}"""

internal const val BUILDING_LIST_JSON =
    """{"source":"© OpenStreetMap contributors","buildings":[""" +
        """{"symbol":"C-13","name":"Budynek C-13","detailLevel":"DETAILED","outline":$BUILDING_OUTLINE_JSON}]}"""

internal const val BUILDING_DETAILS_JSON =
    """{"symbol":"C-13","name":"Budynek C-13","detailLevel":"BASIC","outline":$BUILDING_OUTLINE_JSON,""" +
        """"entrances":[{"id":"c13-main","buildingSymbol":"C-13","name":"Wejście główne","main":true,""" +
        """"location":{"type":"Point","coordinates":[17,51]},"marking":"ACCESSIBLE",""" +
        """"markingDate":"2026-10-01","markingSource":"IMPORT"}]}"""
