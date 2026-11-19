/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.quick

/** Katalog testów V4: budynek A-1 z konturem i jego wejście `node/6` „G” w rogu konturu. */
internal const val QUICK_CAMPUS =
    """{"type":"FeatureCollection","attribution":"© OpenStreetMap","features":[""" +
        """{"type":"Feature","properties":{"kind":"building","symbols":["A-1"],"name":"A-1",""" +
        """"osm_id":"way/A-1"},"geometry":{"type":"Polygon","coordinates":[[[17.06,51.11],""" +
        """[17.061,51.11],[17.061,51.111],[17.06,51.111],[17.06,51.11]]]}},""" +
        """{"type":"Feature","properties":{"kind":"entrance","symbols":["A-1"],"osm_id":"node/6",""" +
        """"entrance":"yes","ref":"G"},"geometry":{"type":"Point","coordinates":[17.06,51.11]}}]}"""
