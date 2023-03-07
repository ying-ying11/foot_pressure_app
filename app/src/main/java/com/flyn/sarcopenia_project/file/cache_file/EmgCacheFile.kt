package com.flyn.sarcopenia_project.file.cache_file

class EmgCacheFile(private val timeLabel: Long, private val emgData: ShortArray, private val average: Double, private val isFatigue: Boolean): CacheFile {

    override fun toCsv(): String {
        return "$timeLabel, ${emgData[0]}, ${emgData[1]}, ${emgData[2]}, ${emgData[3]}, ${emgData[4]}, ${emgData[5]}, $average, $isFatigue\n"
    }

}