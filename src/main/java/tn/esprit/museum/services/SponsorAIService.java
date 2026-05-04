package tn.esprit.museum.services;

import tn.esprit.museum.entities.Event;
import tn.esprit.museum.entities.Sponsor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SponsorAIService {

    public static double calculateMatchPercentage(String sponsorDesc, String eventText) {
        if (sponsorDesc == null || sponsorDesc.isBlank() || eventText == null || eventText.isBlank()) {
            return 0.0;
        }

        sponsorDesc = sponsorDesc.toLowerCase();
        eventText = eventText.toLowerCase();

        Set<String> sponsorWords = new HashSet<>(Arrays.asList(sponsorDesc.split("\\W+")));
        Set<String> eventWords = new HashSet<>(Arrays.asList(eventText.split("\\W+")));

        sponsorWords.remove("");
        eventWords.remove("");

        Set<String> intersection = new HashSet<>(sponsorWords);
        intersection.retainAll(eventWords);

        Set<String> union = new HashSet<>(sponsorWords);
        union.addAll(eventWords);

        int minSize = Math.min(sponsorWords.size(), eventWords.size());
        if (minSize == 0) {
            return 0.0;
        }

        return (double) intersection.size() / minSize * 100.0;
    }

    public static double calculateMatchPercentage(Sponsor sponsor, Event event) {
        if (sponsor == null || sponsor.getDescription() == null || sponsor.getDescription().isBlank()) {
            return 0.0;
        }
        if (event == null) {
            return 0.0;
        }

        StringBuilder eventText = new StringBuilder();
        if (event.getTitle() != null) eventText.append(event.getTitle()).append(" ");
        if (event.getDescription() != null) eventText.append(event.getDescription()).append(" ");
        if (event.getCategory() != null) eventText.append(event.getCategory()).append(" ");

        return calculateMatchPercentage(sponsor.getDescription(), eventText.toString());
    }
}

