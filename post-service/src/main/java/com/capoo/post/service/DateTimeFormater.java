package com.capoo.post.service;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class DateTimeFormater {
    Map<Long, Function<Instant, String>> strageyMap= new LinkedHashMap<>();

    public DateTimeFormater() {
        strageyMap.put(60L, this::formatInSecond);
        strageyMap.put(3600L, this::formatInMinute);
        strageyMap.put(86400L, this::formatInHour);
        strageyMap.put(Long.MAX_VALUE, this::formatInDay);

    }

    private String formatInSecond(Instant instant) {
        long elapsedSeconds = Instant.now().getEpochSecond() - instant.getEpochSecond();
        return elapsedSeconds + " seconds ago";
    }
    private String formatInMinute(Instant instant) {
        long elapsedSeconds = Instant.now().getEpochSecond() - instant.getEpochSecond();
        long minutes = elapsedSeconds / 60;
        return minutes + " minutes ago";
    }
    private String formatInHour(Instant instant) {
        long elapsedSeconds = Instant.now().getEpochSecond() - instant.getEpochSecond();
        long hours = elapsedSeconds / 3600;
        return hours + " hours ago";
    }
    private String formatInDay(Instant instant) {
        LocalDateTime localDateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
        DateTimeFormatter dateTimeFormater= DateTimeFormatter.ISO_DATE_TIME;

        return  localDateTime.format(dateTimeFormater);
    }
    public String format(Instant instant){
        return strageyMap.entrySet().stream()
                .filter(entry -> Instant.now().getEpochSecond() - instant.getEpochSecond() < entry.getKey())
                .findFirst()
                .map(entry -> entry.getValue().apply(instant))
                .orElseThrow(() -> new IllegalStateException("Unexpected time difference"));
    }
}
