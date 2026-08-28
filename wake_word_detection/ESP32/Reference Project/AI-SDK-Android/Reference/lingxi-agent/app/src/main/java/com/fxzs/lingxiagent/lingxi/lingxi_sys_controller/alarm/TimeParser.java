package com.fxzs.lingxiagent.lingxi.lingxi_sys_controller.alarm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

/**
 * 创建者：ZyOng
 * 描述：
 * 创建时间：2025/6/24 6:08 PM
 */
public class TimeParser {
    private static final String TYPE_DAILY = "Daily";
    private static final String TYPE_WEEKLY = "Weekly";
    private static final String TYPE_WORKDAYS= "Workdays";
    public static class TimeResult {
        public int hour;
        public int minute;

        public TimeResult(int hour, int minute) {
            this.hour = hour;
            this.minute = minute;
        }
    }

    public static TimeResult parseTime(String input, String apm) {
        int hour = 0;
        int minute = 0;
        // 匹配时间格式，例如：八点、八点四十五、一点半
        Pattern pattern;
        if (input.contains("分")) {
            pattern = Pattern.compile("([一二三四五六七八九十零两\\d]{1,3})点(半|[一二三四五六七八九十零两\\d]{1,3}分)?");

        } else {
            pattern = Pattern.compile("([一二三四五六七八九十零两\\d]{1,3})点(半|[一二三四五六七八九十零两\\d]{1,3})?");

        }
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            String hourStr = matcher.group(1);
            String minuteStr = matcher.group(2);
            Timber.tag("timer").d("解析结果 "+"小时: " + hourStr + " 分钟: " + minuteStr);
            hour = chineseToNumber(hourStr);

            if (minuteStr != null) {
                if (minuteStr.contains("半")) {
                    minute = 30;
                } else {
                    minuteStr = minuteStr.replace("分", "");
                    minute = chineseToNumber(minuteStr);
                }
            }

            // 时间段处理
            if (apm != null && apm.equals("PM") && hour < 12) {
                hour += 12;
            } else if (apm != null && apm.equals("AM") && (hour == 12 || hour == 0)) { // 凌晨12点是 0 点
                hour = 0;
            } else if (apm != null && apm.equals("PM") && hour == 24) {
                hour = 0;
            }

            return new TimeResult(hour, minute);
        }

        return new TimeResult(-1, -1);
    }

    // 支持简单中文数字转阿拉伯数字（可扩展）
    private static int chineseToNumber(String chinese) {
        if (chinese.matches("\\d+")) {
            return Integer.parseInt(chinese);
        }

        Map<String, Integer> numMap = new HashMap<>();
        numMap.put("零", 0);
        numMap.put("一", 1);
        numMap.put("二", 2);
        numMap.put("两", 2);
        numMap.put("三", 3);
        numMap.put("四", 4);
        numMap.put("五", 5);
        numMap.put("六", 6);
        numMap.put("七", 7);
        numMap.put("八", 8);
        numMap.put("九", 9);
        numMap.put("十", 10);

        int result = 0;

        if (chinese.length() == 1) {
            return numMap.getOrDefault(chinese, 0);
        }

        if (chinese.startsWith("十")) { // 十五
            result = 10 + numMap.getOrDefault(chinese.substring(1), 0);
        } else if (chinese.contains("十")) { // 二十三
            String[] parts = chinese.split("十");
            int ten = numMap.getOrDefault(parts[0], 0);
            int unit = parts.length > 1 ? numMap.getOrDefault(parts[1], 0) : 0;
            result = ten * 10 + unit;
        } else {
            result = numMap.getOrDefault(chinese, 0);
        }


        return result;
    }

    public static ArrayList<Integer> createWeekday(String type,String date) {
        ArrayList<Integer> days = new ArrayList<>();

        if (type.equals(TYPE_WORKDAYS)) {
            days.addAll(Arrays.asList(
                    Calendar.MONDAY,
                    Calendar.TUESDAY,
                    Calendar.WEDNESDAY,
                    Calendar.THURSDAY,
                    Calendar.FRIDAY
            ));
        } else if (type.equals(TYPE_WEEKLY)) {
            days.add(getCurrentDay(date));
        }else {
            days.addAll(Arrays.asList(
                    Calendar.MONDAY,
                    Calendar.TUESDAY,
                    Calendar.WEDNESDAY,
                    Calendar.THURSDAY,
                    Calendar.FRIDAY,
                    Calendar.SATURDAY,
                    Calendar.SUNDAY
            ));
        }

        return days;

    }

    private static int getCurrentDay(String date){
        switch (date) {
            case "Monday":
                return Calendar.MONDAY;
            case "Tuesday":
                return Calendar.TUESDAY;
            case "Wednesday":
                return Calendar.WEDNESDAY;
            case "Thursday":
                return Calendar.THURSDAY;
            case "Friday":
                return Calendar.FRIDAY;
            case "Saturday":
                return Calendar.SATURDAY;
            case "Sunday":
                return Calendar.SUNDAY;
        }
        return Calendar.MONDAY;
    }

}

