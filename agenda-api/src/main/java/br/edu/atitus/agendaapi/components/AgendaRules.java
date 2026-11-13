package br.edu.atitus.agendaapi.components;

import br.edu.atitus.agendaapi.entities.AvailabilityEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class AgendaRules {

    public static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    public static final int SLOT_MINUTES = 30;

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

    public static boolean overlaps(LocalDateTime startA, LocalDateTime endA, LocalDateTime startB, LocalDateTime endB) {
        return startA.isBefore(endB) && endA.isAfter(startB);
    }

    public static boolean fitsInAvailability(LocalDateTime start, LocalDateTime end, List<AvailabilityEntity> windows) {
        if (!start.toLocalDate().equals(end.toLocalDate()))
            return false;
        int dayOfWeek = start.getDayOfWeek().getValue();
        for (AvailabilityEntity window : windows) {
            if (window.getDayOfWeek() == dayOfWeek
                    && !start.toLocalTime().isBefore(window.getStartTime())
                    && !end.toLocalTime().isAfter(window.getEndTime()))
                return true;
        }
        return false;
    }

    public static List<LocalTime> freeSlots(LocalDate date, int durationMinutes, List<AvailabilityEntity> windows,
                                            List<LocalDateTime[]> busy, LocalDateTime now) {
        List<LocalTime> slots = new ArrayList<>();
        int dayOfWeek = date.getDayOfWeek().getValue();
        for (AvailabilityEntity window : windows) {
            if (window.getDayOfWeek() != dayOfWeek)
                continue;
            LocalDateTime start = date.atTime(window.getStartTime());
            LocalDateTime windowEnd = date.atTime(window.getEndTime());
            while (!start.plusMinutes(durationMinutes).isAfter(windowEnd)) {
                LocalDateTime end = start.plusMinutes(durationMinutes);
                if (!start.isBefore(now) && isFree(start, end, busy))
                    slots.add(start.toLocalTime());
                start = start.plusMinutes(SLOT_MINUTES);
            }
        }
        return slots.stream().distinct().sorted().toList();
    }

    private static boolean isFree(LocalDateTime start, LocalDateTime end, List<LocalDateTime[]> busy) {
        for (LocalDateTime[] appointment : busy) {
            if (overlaps(start, end, appointment[0], appointment[1]))
                return false;
        }
        return true;
    }
}
