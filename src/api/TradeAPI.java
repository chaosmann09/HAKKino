package api;

import mysql.DataAccess;

import javax.swing.*;
import java.awt.Color;
import java.util.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class TradeAPI {

    public void message(String text, TYPE_MESSAGE type_message) {
        switch (type_message) {
            case ERROR:
                JOptionPane.showMessageDialog(null, text, "HakKino-System", JOptionPane.ERROR_MESSAGE);
                break;
            case DIALOG:
                JOptionPane.showConfirmDialog(null, text, "HakKino-System - Sicherstellung", JOptionPane.YES_NO_OPTION);
                System.out.println("Wurde noch nicht programmiert");
                break;
            case INFORMATION_MESSAGE:
                JOptionPane.showMessageDialog(null, text, "HakKino-System", JOptionPane.INFORMATION_MESSAGE);
                break;
            case WARNING:
                JOptionPane.showMessageDialog(null, text, "HakKino-System", JOptionPane.WARNING_MESSAGE);
                break;
            default:
                System.out.println("Keine verfügbarer type_message");
                break;
        }
    }

    public int addTickets(JTextField ticketField) {
        int value = Integer.parseInt(ticketField.getText());
        value++;
        ticketField.setText(String.valueOf(value));
        return value;
    }

    public int removeTickets(JTextField ticketField) {
        int value = Integer.parseInt(ticketField.getText());
        if (value > 0) {
            ticketField.setText(String.valueOf(value - 1));
            value--;
        } else {
            message("Du kannst keine Minus Tickets kaufen.", TYPE_MESSAGE.ERROR);
        }
        return value;
    }

    public double getPrice(DataAccess db, String saal, String typ) {
        double preis = 0.0;


        String spalte = switch (typ.toLowerCase()) {
            case "erwachsen" -> "preis_erwachsen";
            case "kind" -> "preis_kind";
            default -> null;
        };

        if (spalte == null) {
            System.out.println("Ungültiger Typ: " + typ);
            return 0.0;
        }

        String sql = "SELECT " + spalte + " FROM saal WHERE name = ?";

        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, saal);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                preis = rs.getDouble(spalte);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return preis;
    }
    public void getTicketPrice(JTextField ticketField) {

    }


    // ============ Buchungssystem =========
    private final Map<String, Set<JButton>> selectedSeatsPerRoom = new HashMap<>();
    private final Map<String, Integer> allowedSeatsPerRoom = new HashMap<>();



    public void setAllowedSeats(String roomName, int adults, int kids) {
        allowedSeatsPerRoom.put(roomName, adults + kids);
        System.out.println("[" + roomName + "] Allowed seats: " + (adults + kids));
    }



    public boolean trySelectSeat(String roomName, JButton seatButton) {
        int allowed = allowedSeatsPerRoom.getOrDefault(roomName, 0);
        Set<JButton> selectedSeats = selectedSeatsPerRoom.computeIfAbsent(roomName, k -> new HashSet<>());


        int seatCount = 1;
        Object prop = seatButton.getClientProperty("seatCount");
        if (prop instanceof Integer) seatCount = (Integer) prop;


        if (seatButton.getBackground() == Color.YELLOW) {
            seatButton.setBackground(null);
            removeSeatMultiple(selectedSeats, seatButton, seatCount);
            return true;
        }


        int currentCount = countTotalSelectedSeats(roomName);
        if (currentCount + seatCount > allowed) {
            message("Sie können in " + roomName + " nur " + allowed + " Sitzplätze auswählen!", TYPE_MESSAGE.WARNING);
            return false;
        }


        seatButton.setBackground(Color.YELLOW);
        addSeatMultiple(selectedSeats, seatButton, seatCount);
        return true;
    }


    public boolean confirmBooking(String roomName) {
        int allowed = allowedSeatsPerRoom.getOrDefault(roomName, 0);
        Set<JButton> selectedSeats = selectedSeatsPerRoom.getOrDefault(roomName, new HashSet<>());
        int totalSelected = countTotalSelectedSeats(roomName);

        if (allowed == 0) {
            message("Sie haben keine Tickets ausgewählt!", TYPE_MESSAGE.ERROR);
            return false;
        }

        if (totalSelected == 0) {
            message("Sie haben keine Sitzplätze ausgewählt!", TYPE_MESSAGE.ERROR);
            return false;
        }

        if (totalSelected != allowed) {
            message("Bitte wählen Sie genau " + allowed + " Sitzplätze aus.\n"
                    + "Aktuell ausgewählt: " + totalSelected, TYPE_MESSAGE.WARNING);
            return false;
        }

        return true;
    }

    public double calculateTotalPrice(DataAccess db, String saal, String sitze) {
        double preisErw = getPrice(db, saal, "erwachsen");
        int seatCount = 0;
        for (String s : sitze.split(",")) {
            if (!s.trim().isEmpty()) seatCount++;
        }
        return seatCount * preisErw;
    }

    // ==== Hilfsmethoden ====
    public void resetSelection() {
        for (Set<JButton> seats : selectedSeatsPerRoom.values()) {
            for (JButton b : seats) {
                b.setBackground(null);
            }
            seats.clear();
        }
        selectedSeatsPerRoom.clear();
        allowedSeatsPerRoom.clear();
        // DEBUG System.out.println("Alle Sitz- und Ticketdaten wurden zurückgesetzt.");
    }

    private int countTotalSelectedSeats(String roomName) {
        Set<JButton> selected = selectedSeatsPerRoom.getOrDefault(roomName, new HashSet<>());
        int total = 0;
        for (JButton b : selected) {
            Object prop = b.getClientProperty("seatCount");
            if (prop instanceof Integer) total += (Integer) prop;
            else total++;
        }
        return total;
    }

    private void addSeatMultiple(Set<JButton> set, JButton seat, int count) {
        set.add(seat);
    }

    private void removeSeatMultiple(Set<JButton> set, JButton seat, int count) {
        set.remove(seat);
    }

    private String getSeatList(String roomName) {
        Set<JButton> seats = selectedSeatsPerRoom.getOrDefault(roomName, Collections.emptySet());
        List<String> names = new ArrayList<>();
        for (JButton b : seats) {
            names.add(b.getText());
        }
        return String.join(", ", names);
    }
    public String getSeatListForDialog(String roomName) {
        Set<JButton> seats = selectedSeatsPerRoom.getOrDefault(roomName, Collections.emptySet());
        if (seats.isEmpty()) {
            return "KEine Sitzplätze ausgewählt";
        }

        List<String> names = new ArrayList<>();
        for (JButton b : seats) {
            names.add(b.getText());
        }
        return String.join(", ", names);
    }


    // ============ Admin-Panel =========
    public static boolean setPrice(DataAccess db, String saal, String kategorie, double neuerPreis) {
        String sql = "UPDATE preise SET preis = ? WHERE saal = ? AND kategorie = ?";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setDouble(1, neuerPreis);
            stmt.setString(2, saal);
            stmt.setString(3, kategorie);
            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}

