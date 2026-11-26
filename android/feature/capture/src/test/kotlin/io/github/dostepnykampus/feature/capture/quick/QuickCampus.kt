/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.quick

/**
 * Katalog testów V4: budynek A-1 z konturem i jego wejście `node/6` „G” w rogu konturu oraz uproszczone kontury
 * budynków D-2, A-4 i C-13 w ich prawdziwych miejscach.
 */
internal const val QUICK_CAMPUS =
    """{"type":"FeatureCollection","attribution":"© OpenStreetMap","features":[""" +
        """{"type":"Feature","properties":{"kind":"building","symbols":["A-1"],"name":"A-1",""" +
        """"osm_id":"way/A-1"},"geometry":{"type":"Polygon","coordinates":[[[17.06,51.11],""" +
        """[17.061,51.11],[17.061,51.111],[17.06,51.111],[17.06,51.11]]]}},""" +
        """{"type":"Feature","properties":{"kind":"entrance","symbols":["A-1"],"osm_id":"node/6",""" +
        """"entrance":"yes","ref":"G"},"geometry":{"type":"Point","coordinates":[17.06,51.11]}}""" +
        """,{"type":"Feature","properties":{"kind":"building","symbols":["D-2"],"name":"D-2",""" +
        """"osm_id":"way/D-2"},"geometry":{"type":"Polygon","coordinates":[[[17.0564,51.1097],""" +
        """[17.0572,51.1097],[17.0572,51.1100],[17.0564,51.1100],[17.0564,51.1097]]]}}""" +
        """,{"type":"Feature","properties":{"kind":"building","symbols":["A-4"],"name":"A-4",""" +
        """"osm_id":"way/101125324"},"geometry":{"type":"Polygon","coordinates":[[[17.06275,51.10800],""" +
        """[17.06295,51.10800],[17.06295,51.10810],[17.06275,51.10810],[17.06275,51.10800]]]}}""" +
        """,{"type":"Feature","properties":{"kind":"building","symbols":["C-13"],"name":"C-13",""" +
        """"osm_id":"way/C-13"},"geometry":{"type":"Polygon","coordinates":[[[17.0590,51.1074],""" +
        """[17.0604,51.1074],[17.0604,51.1079],[17.0590,51.1079],[17.0590,51.1074]]]}}""" +
        """]}"""
