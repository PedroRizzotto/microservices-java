package br.edu.atitus.agendaapi.components;

import br.edu.atitus.agendaapi.entities.AvailabilityEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgendaRulesTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 16);
    private static final LocalDate SUNDAY = LocalDate.of(2026, 11, 15);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 15, 12, 0);

    private static AvailabilityEntity window(int dayOfWeek, String start, String end) {
        AvailabilityEntity availability = new AvailabilityEntity();
        availability.setDayOfWeek(dayOfWeek);
        availability.setStartTime(LocalTime.parse(start));
        availability.setEndTime(LocalTime.parse(end));
        return availability;
    }

    private static List<AvailabilityEntity> mondayWindows() {
        return List.of(window(1, "09:00", "12:00"), window(1, "13:00", "18:00"));
    }

    private static LocalDateTime at(LocalDate date, String time) {
        return date.atTime(LocalTime.parse(time));
    }

    @Test
    void overlapsDetectaSobreposicaoParcialETotal() {
        assertTrue(AgendaRules.overlaps(at(MONDAY, "10:00"), at(MONDAY, "11:00"), at(MONDAY, "10:30"), at(MONDAY, "11:30")));
        assertTrue(AgendaRules.overlaps(at(MONDAY, "10:00"), at(MONDAY, "12:00"), at(MONDAY, "10:30"), at(MONDAY, "11:00")));
        assertFalse(AgendaRules.overlaps(at(MONDAY, "10:00"), at(MONDAY, "11:00"), at(MONDAY, "11:00"), at(MONDAY, "12:00")));
    }

    @Test
    void fitsInAvailabilityAceitaDentroDaJanela() {
        assertTrue(AgendaRules.fitsInAvailability(at(MONDAY, "09:00"), at(MONDAY, "10:00"), mondayWindows()));
        assertFalse(AgendaRules.fitsInAvailability(at(MONDAY, "11:30"), at(MONDAY, "12:30"), mondayWindows()));
        assertFalse(AgendaRules.fitsInAvailability(at(MONDAY, "12:00"), at(MONDAY, "13:00"), mondayWindows()));
        assertFalse(AgendaRules.fitsInAvailability(at(MONDAY, "17:30"), at(MONDAY, "18:30"), mondayWindows()));
    }

    @Test
    void fitsInAvailabilityRecusaDiaSemGrade() {
        assertFalse(AgendaRules.fitsInAvailability(at(SUNDAY, "10:00"), at(SUNDAY, "11:00"), mondayWindows()));
    }

    @Test
    void freeSlotsDiaLivre() {
        List<LocalTime> slots = AgendaRules.freeSlots(MONDAY, 60, mondayWindows(), List.of(), NOW);

        assertEquals(14, slots.size());
        assertEquals(LocalTime.parse("09:00"), slots.get(0));
        assertEquals(LocalTime.parse("11:00"), slots.get(4));
        assertEquals(LocalTime.parse("13:00"), slots.get(5));
        assertEquals(LocalTime.parse("17:00"), slots.get(13));
        assertFalse(slots.contains(LocalTime.parse("11:30")));
        assertFalse(slots.contains(LocalTime.parse("17:30")));
    }

    @Test
    void freeSlotsDescontaAgendamento() {
        List<LocalDateTime[]> busy = List.<LocalDateTime[]>of(new LocalDateTime[]{at(MONDAY, "10:00"), at(MONDAY, "11:00")});

        List<LocalTime> slots = AgendaRules.freeSlots(MONDAY, 60, mondayWindows(), busy, NOW);

        assertFalse(slots.contains(LocalTime.parse("09:30")));
        assertFalse(slots.contains(LocalTime.parse("10:00")));
        assertFalse(slots.contains(LocalTime.parse("10:30")));
        assertTrue(slots.contains(LocalTime.parse("09:00")));
        assertTrue(slots.contains(LocalTime.parse("11:00")));
    }

    @Test
    void freeSlotsDiaSemGrade() {
        assertTrue(AgendaRules.freeSlots(SUNDAY, 60, mondayWindows(), List.of(), NOW).isEmpty());
    }

    @Test
    void freeSlotsSemRepetirComJanelasSobrepostas() {
        List<AvailabilityEntity> windows = List.of(window(1, "09:00", "12:00"), window(1, "11:00", "14:00"));

        List<LocalTime> slots = AgendaRules.freeSlots(MONDAY, 60, windows, List.of(), NOW);

        assertEquals(slots.stream().distinct().count(), slots.size());
        assertEquals(LocalTime.parse("13:00"), slots.get(slots.size() - 1));
    }

    @Test
    void freeSlotsIgnoraPassado() {
        List<LocalTime> slots = AgendaRules.freeSlots(MONDAY, 60, mondayWindows(), List.of(), at(MONDAY, "10:15"));

        assertEquals(LocalTime.parse("10:30"), slots.get(0));
    }
}
