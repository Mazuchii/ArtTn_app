package tn.esprit.museum.services;

import tn.esprit.museum.entities.Event;
import tn.esprit.museum.entities.Reservation;
import tn.esprit.museum.entities.User;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class RecommendationService {

    private final EventService eventService;
    private final ReservationService reservationService;

    public RecommendationService() {
        this.eventService = new EventService();
        this.reservationService = new ReservationService();
    }

    public List<Event> getRecommendationsForUser(User user, int limit) throws SQLException {
        List<Reservation> userReservations = reservationService.getByClientEmail(user.getEmail())
                .stream()
                .filter(r -> "CONFIRMEE".equals(r.getStatus()) || "EN_ATTENTE".equals(r.getStatus()))
                .toList();

        if (userReservations.isEmpty()) {
            return getPopularEvents(limit);
        }

        List<Event> likedEvents = new ArrayList<>();
        for (Reservation res : userReservations) {
            Event e = eventService.getById(res.getEventId());
            if (e != null) likedEvents.add(e);
        }

        if (likedEvents.isEmpty()) return getPopularEvents(limit);

        Set<Integer> reservedIds = userReservations.stream().map(Reservation::getEventId).collect(Collectors.toSet());
        List<Event> candidates = eventService.getAll().stream()
                .filter(e -> !reservedIds.contains(e.getId())
                        && ("A_VENIR".equals(e.getStatus()) || "EN_COURS".equals(e.getStatus())))
                .toList();

        Map<Event, Double> scores = new HashMap<>();
        for (Event candidate : candidates) {
            double totalSimilarity = 0.0;
            for (Event liked : likedEvents) {
                totalSimilarity += computeSimilarity(liked, candidate);
            }
            scores.put(candidate, totalSimilarity / likedEvents.size());
        }

        return scores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public List<Event> getSimilarEvents(Event event, int limit) throws SQLException {
        List<Event> allEvents = eventService.getAll().stream()
                .filter(e -> e.getId() != event.getId()
                        && ("A_VENIR".equals(e.getStatus()) || "EN_COURS".equals(e.getStatus())))
                .toList();

        Map<Event, Double> scores = new HashMap<>();
        for (Event other : allEvents) {
            scores.put(other, computeSimilarity(event, other));
        }

        return scores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private double computeSimilarity(Event a, Event b) {
        double score = 0.0;
        if (a.getCategory().equals(b.getCategory())) {
            score += 0.4;
        } else if (isCategoryClose(a.getCategory(), b.getCategory())) {
            score += 0.2;
        }

        double locSim = a.getLocation().equalsIgnoreCase(b.getLocation()) ? 0.2 : 0.0;
        if (locSim == 0.0 && (a.getLocation().toLowerCase().contains(b.getLocation().toLowerCase())
                || b.getLocation().toLowerCase().contains(a.getLocation().toLowerCase()))) {
            locSim = 0.1;
        }
        score += locSim;

        double priceDiff = Math.abs(a.getPrice().doubleValue() - b.getPrice().doubleValue());
        double priceSim = Math.exp(-priceDiff / 20.0);
        score += priceSim * 0.2;

        long daysDiff = Math.abs(a.getEventDate().toEpochDay() - b.getEventDate().toEpochDay());
        double dateSim = Math.exp(-daysDiff / 60.0);
        score += dateSim * 0.2;

        return score;
    }

    private boolean isCategoryClose(String cat1, String cat2) {
        return (cat1.equals("SPECTACLE") && cat2.equals("CONFERENCE")) ||
                (cat2.equals("SPECTACLE") && cat1.equals("CONFERENCE"));
    }

    private List<Event> getPopularEvents(int limit) throws SQLException {
        List<Reservation> allReservations = reservationService.getAll().stream()
                .filter(r -> "CONFIRMEE".equals(r.getStatus()))
                .toList();
        Map<Integer, Long> reservationCounts = allReservations.stream()
                .collect(Collectors.groupingBy(Reservation::getEventId, Collectors.counting()));

        return eventService.getAll().stream()
                .filter(e -> "A_VENIR".equals(e.getStatus()) || "EN_COURS".equals(e.getStatus()))
                .sorted((a, b) -> Long.compare(
                        reservationCounts.getOrDefault(b.getId(), 0L),
                        reservationCounts.getOrDefault(a.getId(), 0L)
                ))
                .limit(limit)
                .collect(Collectors.toList());
    }
}