package com.mubarak.mbcompass.utils

import androidx.annotation.StringRes
import com.mubarak.mbcompass.R

object CardinalDirection {

    fun getDirectionFromAzimuth(azimuth: Float) = when (azimuth) {
        in 0.0 ..< 22.5, in 337.5..360.0 -> DIRECTION.NORTH
        in 22.5..< 45.0 -> DIRECTION.NORTH_NORTHEAST
        in 45.0..< 67.5 -> DIRECTION.NORTHEAST
        in 67.5..< 90.0 -> DIRECTION.EAST_NORTHEAST
        in 90.0..< 112.5 -> DIRECTION.EAST
        in 112.5..< 135.0 -> DIRECTION.EAST_SOUTHEAST
        in 135.0..< 157.5 -> DIRECTION.SOUTHEAST
        in 157.5..< 180.0 -> DIRECTION.SOUTH_SOUTHEAST
        in 180.0..< 202.5 -> DIRECTION.SOUTH
        in 202.5..< 225.0 -> DIRECTION.SOUTH_SOUTHWEST
        in 225.0..< 247.5 -> DIRECTION.SOUTHWEST
        in 247.5..< 270.0 -> DIRECTION.WEST_SOUTHWEST
        in 270.0..< 292.5 -> DIRECTION.WEST
        in 292.5..< 315.0 -> DIRECTION.WEST_NORTHWEST
        in 315.0..< 337.5 -> DIRECTION.NORTHWEST
        else -> DIRECTION.NA // In case the azimuth doesn't fall in any expected range
    }

}

enum class DIRECTION(@StringRes val dirName: Int) { // true north-based azimuths
    NORTH(R.string.north),
    NORTH_NORTHEAST(R.string.north_northeast),
    NORTHEAST(R.string.northeast),
    EAST_NORTHEAST(R.string.east_northeast),
    EAST(R.string.east),
    EAST_SOUTHEAST(R.string.east_southeast),
    SOUTHEAST(R.string.southeast),
    SOUTH_SOUTHEAST(R.string.south_southeast),
    SOUTH(R.string.south),
    SOUTH_SOUTHWEST(R.string.south_southwest),
    SOUTHWEST(R.string.southwest),
    WEST_SOUTHWEST(R.string.west_southwest),
    WEST(R.string.west),
    WEST_NORTHWEST(R.string.west_northwest),
    NORTHWEST(R.string.northwest),
    NA(R.string.no_dir),
}