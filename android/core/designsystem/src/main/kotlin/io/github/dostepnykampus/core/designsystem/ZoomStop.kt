/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

/**
 * Próg wymiaru mapy z pliku tokenów: przy powiększeniu [zoom] wymiar ma [dp]; między progami styl mapy go interpoluje.
 *
 * @property zoom powiększenie mapy (`powiekszenie`)
 * @property dp wymiar w dp przy tym powiększeniu
 */
public data class ZoomStop(val zoom: Double, val dp: Double)
