package com.ownding.headscale.utils;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Headscale时间格式处理工具类
 */
public class HeadscaleTimeUtils {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_ZONED_DATE_TIME;
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 解析Headscale API返回的时间字符串为LocalDateTime
     *
     * @param timeStr 时间字符串，如 "2025-04-22T08:27:18.802455714Z"
     * @return LocalDateTime对象，解析失败返回null
     */
    public static LocalDateTime parseHeadscaleTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return null;
        }

        try {
            // 解析ISO 8601格式的时间字符串
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(timeStr, ISO_FORMATTER);
            return zonedDateTime.toLocalDateTime();
        } catch (DateTimeParseException e) {
            // 如果解析失败，返回null
            return null;
        }
    }

    /**
     * 格式化时间为显示字符串
     *
     * @param timeStr Headscale时间字符串
     * @return 格式化后的时间字符串，如 "2025-04-22 08:27:18"
     */
    public static String formatHeadscaleTime(String timeStr) {
        LocalDateTime dateTime = parseHeadscaleTime(timeStr);
        if (dateTime == null) {
            return timeStr; // 返回原字符串
        }

        return dateTime.format(DISPLAY_FORMATTER);
    }

    /**
     * 检查时间字符串是否有效
     *
     * @param timeStr 时间字符串
     * @return 是否为有效的时间格式
     */
    public static boolean isValidHeadscaleTime(String timeStr) {
        return parseHeadscaleTime(timeStr) != null;
    }
}
