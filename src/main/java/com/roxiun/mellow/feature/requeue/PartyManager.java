package com.roxiun.mellow.feature.requeue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PartyManager {

    public static PartyManager instance;

    private final List<String> party = new ArrayList<>();

    public PartyManager() {
        instance = this;
    }

    public void clearParty() {
        party.clear();
    }

    public void registerPlayer(String player) {
        if (player == null || player.trim().isEmpty()) {
            return;
        }
        String normalized = player.trim();
        if (!partyContains(normalized)) {
            party.add(normalized);
        }
    }

    public boolean removePlayer(String player) {
        if (player == null) {
            return false;
        }
        String normalized = player.trim();
        for (Iterator<String> iterator = party.iterator(); iterator.hasNext();) {
            String current = iterator.next();
            if (current.equalsIgnoreCase(normalized)) {
                iterator.remove();
                return true;
            }
        }
        return false;
    }

    public boolean partyContains(String player) {
        if (player == null) return false;
        for (String current : party) {
            if (current.equalsIgnoreCase(player)) {
                return true;
            }
        }
        return false;
    }

    public List<String> getParty() {
        return new ArrayList<>(party);
    }
}
