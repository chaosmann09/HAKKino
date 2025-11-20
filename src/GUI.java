import api.TYPE_MESSAGE;
import api.TradeAPI;
import mysql.DataAccess;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class GUI extends JFrame {
    private final CardLayout cardLayout;
    private final JPanel contentPanel;
    private final TradeAPI tradeAPI = new TradeAPI();
    private final DataAccess dataAccess;

    public GUI() {
        // ======= Datenbank =======
        Properties props = loadProperties();
        dataAccess = new DataAccess(
                props.getProperty("db.host"),
                props.getProperty("db.port"),
                props.getProperty("db.name"),
                props.getProperty("db.user"),
                props.getProperty("db.pass")
        );

        dataAccess.openConnection();
        exitdatabase(this);

        // ======= Grundeinstellungen =======
        setTitle("Hak-Kino - Saal 1");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setSize(625, 550);

        // ======= Buttons switchen oben =======
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton btnSaal1 = new JButton("Saal 1");
        JButton btnSaal2 = new JButton("Saal 2");
        JButton btnSaal3 = new JButton("Saal 3");

        buttonPanel.add(btnSaal1);
        buttonPanel.add(btnSaal2);
        buttonPanel.add(btnSaal3);
        add(buttonPanel, BorderLayout.NORTH);

        // ======= Content Panel mit CardLayout =======
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        add(contentPanel, BorderLayout.CENTER);

        // ======= Saäle =======
        JPanel room = createRoom(contentPanel);
        JPanel room2 = createRoom2(contentPanel);
        JPanel room3 = createRoom3(contentPanel);

        contentPanel.add(room, "Saal1");
        contentPanel.add(room2, "Saal2");
        contentPanel.add(room3, "Saal3");

        // ======= Buttons =======
        btnSaal1.addActionListener(e -> switchRoom("Saal1", "Hak-Kino - Saal 1", room));
        btnSaal2.addActionListener(e -> switchRoom("Saal2", "Hak-Kino - Saal 2", room2));
        btnSaal3.addActionListener(e -> switchRoom("Saal3", "Hak-Kino - Saal 3", room3));

        setVisible(true);
    }

    public void exitdatabase(JFrame frame) {
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int result = JOptionPane.showConfirmDialog(
                        frame,
                        "Deine Buchung wird nicht gespeichert! Beenden?",
                        "Beenden bestätigen",
                        JOptionPane.YES_NO_OPTION
                );

                if (result == JOptionPane.YES_OPTION) {
                    dataAccess.closeConnection();
                    frame.dispose();
                    System.exit(0);
                }
            }
        });
    }


    private void switchRoom(String roomName, String title, JPanel room) {
        cardLayout.show(contentPanel, roomName);
        setTitle(title);
        room.requestFocusInWindow();
    }

    private JPanel createRoom(JPanel contentPanel) {
        String saal = "Saal 1";
        JPanel room = new JPanel(null);
        JPanel pricePanel = defaultroomLayout(saal);
        pricePanel.setBounds(30,20,580,100);
        room.add(pricePanel);
        // ===== BuchungsSystem-System  =====
        int lastRowY = 0;


        // ===== Sitzplätze-System  =====
        int seatWidth = 50;
        int seatHeight = 28;
        int seatGapX = 8;
        int seatGapY = 10;
        int rows = 5; // A B C D E F G H
        int leftSeats = 4;
        int rightSeats = 3;
        int aisleGap = 60;
        int startY = 180;
        int startX = 80;
        Font seatFont = new Font("Arial", Font.BOLD, 11);

        for (int row = 0; row < rows; row++) {
            char rowLetter = (char) ('A' + row); // A, B, C, D, E

            // Linke Seite
            for (int col = 0; col < leftSeats; col++) {
                int x = startX + col * (seatWidth + seatGapX);
                int y = startY + row * (seatHeight + seatGapY);
                String label = (rowLetter + String.valueOf(col + 1)).toUpperCase();
                JButton seat = new JButton(label);
                seat.setBounds(x, y, seatWidth, seatHeight);
                seat.setFont(seatFont);

                seat.addActionListener(e -> tradeAPI.trySelectSeat(saal,seat));

                room.add(seat);
                lastRowY = y;
            }

            // Rechte Seite
            for (int col = 0; col < rightSeats; col++) {
                int x = startX + (leftSeats * (seatWidth + seatGapX)) + aisleGap + col * (seatWidth + seatGapX);
                int y = startY + row * (seatHeight + seatGapY);
                String label = (rowLetter + String.valueOf(leftSeats + col + 1)).toUpperCase();
                JButton seat = new JButton(label);
                seat.setBounds(x, y, seatWidth, seatHeight);
                seat.setFont(seatFont);

                seat.addActionListener(e -> tradeAPI.trySelectSeat(saal,seat));

                room.add(seat);
                lastRowY = y;
            }

        }
        createBookingButton(room,lastRowY+seatHeight+40, saal);
        return room;
    }



    private JPanel createRoom3(JPanel contentPanel) {
        String saal = "Saal 3";
        JPanel room = new JPanel(null); // MUSS null sein, sonst Crashout
        JPanel pricePanel = defaultroomLayout(saal);
        pricePanel.setBounds(30,20,580,100);
        room.add(pricePanel);

        // ===== Sitzplätze-System =====
        int totalRows = 6;          // A–F
        int baseSeats = 7;          // erste 4 Reihen
        int reduceAfter = 4;
        int reduceStep = 1;

        int seatWidth = 50;
        int seatHeight = 28;
        int gapX = 8;
        int gapY = 12;
        int startY = 10;
        Font seatFont = new Font("Arial", Font.BOLD, 11);


        JPanel seatPanel = new JPanel(null);
        seatPanel.setBounds(30, 130, 540, 340);
        room.add(seatPanel);

        int currentY = startY;

        for (int row = 0; row < totalRows; row++) {
            char rowLetter = (char) ('A' + row);


            int seatsInRow;
            if (row < reduceAfter) {
                seatsInRow = baseSeats;
            } else {
                seatsInRow = baseSeats - ((row - reduceAfter + 1) * reduceStep);
                if (seatsInRow < 1) seatsInRow = 1;
            }


            int totalRowWidth = (seatWidth * seatsInRow) + (gapX * (seatsInRow - 1));
            int startXCentered = Math.max(30, (540 - totalRowWidth) / 2);

            int x = startXCentered;
            for (int i = 1; i <= seatsInRow; i++) {
                JButton seat = new JButton((rowLetter + String.valueOf(i)).toUpperCase());
                seat.setBounds(x, currentY, seatWidth, seatHeight);
                seat.setFont(seatFont);
                seat.putClientProperty("seatCount", 1);
                seat.addActionListener(e -> tradeAPI.trySelectSeat(saal, seat));
                seatPanel.add(seat);
                x += seatWidth + gapX;
            }

            currentY += seatHeight + gapY;

        }
        createBookingButton(room,seatPanel.getY() + currentY + 30, saal);
        return room;
    }

    private JPanel createRoom2(JPanel contentPanel) {
        String saal = "Saal 2";
        JPanel room = new JPanel(null);// MUSS null sein, sonst Crashout
        JPanel pricePanel = defaultroomLayout(saal);
        pricePanel.setBounds(30,20,580,100);
        room.add(pricePanel);

        // ===== Sitzplätze-System  =====
        JPanel seatPanel = new JPanel(null);
        seatPanel.setBounds(30, 130, 540, 340);


        int seatWidth = 50;
        int seatHeight = 28;
        int gapX = 8;
        int gapY = 12;
        int startX = 50;
        int startY = 10;
        Font seatFont = new Font("Arial", Font.BOLD, 11);

        int currentY = startY;

        // ==== Reihen A–C: je 7 Einzelsitze ====
        for (int row = 0; row < 3; row++) {
            char rowLetter = (char) ('A' + row);
            int x = startX;

            for (int i = 1; i <= 7; i++) {
                JButton seat = new JButton((rowLetter + String.valueOf(i)).toUpperCase());
                seat.setBounds(x, currentY, seatWidth, seatHeight);
                seat.setFont(seatFont);
                seat.putClientProperty("seatCount", 1);
                seat.addActionListener(e -> tradeAPI.trySelectSeat(saal, seat));
                seatPanel.add(seat);
                x += seatWidth + gapX;
            }
            currentY += seatHeight + gapY;
        }

        // ==== Reihe D: 1 Einzel, dann 2 Doppelsitze ====
        {
            char rowLetter = 'D';
            int x = startX;

            JButton d1 = new JButton("D1");
            d1.setBounds(x, currentY, seatWidth, seatHeight);
            d1.setFont(seatFont);
            d1.putClientProperty("seatCount", 1);
            d1.addActionListener(e -> tradeAPI.trySelectSeat(saal, d1));
            seatPanel.add(d1);
            x += seatWidth + gapX;

            JButton d2 = new JButton("D2-D3");
            d2.setBounds(x, currentY, seatWidth * 2 + gapX, seatHeight);
            d2.setFont(seatFont);
            d2.putClientProperty("seatCount", 2);
            d2.addActionListener(e -> tradeAPI.trySelectSeat(saal, d2));
            seatPanel.add(d2);
            x += (seatWidth * 2 + gapX) + gapX;

            JButton d3 = new JButton("D4-D5");
            d3.setBounds(x, currentY, seatWidth * 2 + gapX, seatHeight);
            d3.setFont(seatFont);
            d3.putClientProperty("seatCount", 2);
            d3.addActionListener(e -> tradeAPI.trySelectSeat(saal, d3));
            seatPanel.add(d3);

            currentY += seatHeight + gapY;
        }

        // ==== Reihe E: 2 Partnersitze, Abstand, dann 3 Einzelsitze ====
        {
            char rowLetter = 'E';
            int x = startX;

            JButton e1 = new JButton("E1-E2");
            e1.setBounds(x, currentY, seatWidth * 2 + gapX, seatHeight);
            e1.setFont(seatFont);
            e1.putClientProperty("seatCount", 2);
            e1.addActionListener(e -> tradeAPI.trySelectSeat(saal, e1));
            seatPanel.add(e1);
            x += (seatWidth * 2 + gapX) + (gapX*2);

            JButton e2 = new JButton("E3-E4");
            e2.setBounds(x, currentY, seatWidth * 2 + gapX, seatHeight);
            e2.setFont(seatFont);
            e2.putClientProperty("seatCount", 2);
            e2.addActionListener(e -> tradeAPI.trySelectSeat(saal, e2));
            seatPanel.add(e2);

            x += (seatWidth *2) + (gapX *3) + 25; // Abstand

            for (int i = 5; i <= 7; i++) {
                JButton seat = new JButton(("E" + i).toUpperCase());
                seat.setBounds(x, currentY, seatWidth, seatHeight);
                seat.setFont(seatFont);
                seat.putClientProperty("seatCount", 1);
                seat.addActionListener(e -> tradeAPI.trySelectSeat(saal, seat));
                seatPanel.add(seat);
                x += seatWidth + gapX;
            }

            currentY += seatHeight + gapY;
        }

        // ==== Reihen F–G: je 2 Partnersitze ====
        for (int r = 0; r < 2; r++) {
            char rowLetter = (char) ('F' + r);
            int x = startX;

            JButton d1 = new JButton(rowLetter + "1-" + rowLetter + "2");
            d1.setBounds(x, currentY, seatWidth * 2 + gapX, seatHeight);
            d1.setFont(seatFont);
            d1.putClientProperty("seatCount", 2);
            d1.addActionListener(e -> tradeAPI.trySelectSeat(saal, d1));
            seatPanel.add(d1);
            x += (seatWidth * 2 + gapX) + gapX;

            JButton d2 = new JButton(rowLetter + "3-" + rowLetter + "4");
            d2.setBounds(x, currentY, seatWidth * 2 + gapX, seatHeight);
            d2.setFont(seatFont);
            d2.putClientProperty("seatCount", 2);
            d2.addActionListener(e -> tradeAPI.trySelectSeat(saal, d2));
            seatPanel.add(d2);

            currentY += seatHeight + gapY;
        }
        createBookingButton(room, seatPanel.getY()+currentY+30, saal);
        room.add(seatPanel);

        // ==== DEBUGGING das behebt es ka warum====
        seatPanel.revalidate();
        seatPanel.repaint();
        room.revalidate();
        room.repaint();

        return room;
    }


    public DataAccess getDataAccess() {
        return dataAccess;
    }
    private JButton createBookingButton(JPanel parentPanel, int yPosition, String saal) {
        JButton bookButton = new JButton("BUCHEN");
        bookButton.setFont(new Font("Arial", Font.BOLD, 18));
        bookButton.setBackground(new Color(0, 120, 255));
        bookButton.setForeground(Color.WHITE);
        bookButton.setFocusPainted(false);
        bookButton.setBounds(200, yPosition, 200, 50);

        // ==== Buchungslogik ====
        bookButton.addActionListener(e -> {
            boolean ready = tradeAPI.confirmBooking(saal);
            if (!ready) return;

            // ==== Dropdown finden ====
            JComboBox<String> filmDropdown = null;
            for (Component comp : parentPanel.getComponents()) {
                if (comp instanceof JPanel panel) {
                    for (Component c : panel.getComponents()) {
                        if (c instanceof JComboBox<?>) {
                            filmDropdown = (JComboBox<String>) c;
                            break;
                        }
                    }
                }
            }

            if (filmDropdown == null || filmDropdown.getSelectedItem() == null) {
                tradeAPI.message("Bitte zuerst einen Film auswählen!", TYPE_MESSAGE.WARNING);
                return;
            }

            String filmAnzeige = (String) filmDropdown.getSelectedItem();

            // ==== Name ====
            JTextField txtVorname = new JTextField();
            JTextField txtNachname = new JTextField();
            JPanel inputPanel = new JPanel(new GridLayout(2, 2, 5, 5));
            inputPanel.add(new JLabel("Vorname:"));
            inputPanel.add(txtVorname);
            inputPanel.add(new JLabel("Nachname:"));
            inputPanel.add(txtNachname);

            int userInput = JOptionPane.showConfirmDialog(
                    parentPanel,
                    inputPanel,
                    "Name für Buchung eingeben",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (userInput != JOptionPane.OK_OPTION) return;

            String vorname = txtVorname.getText().trim();
            String nachname = txtNachname.getText().trim();

            if (vorname.isEmpty() || nachname.isEmpty()) {
                tradeAPI.message("Bitte Vor- und Nachname eingeben!", TYPE_MESSAGE.WARNING);
                return;
            }

            String sitze = tradeAPI.getSeatListForDialog(saal);
            double gesamtpreis = tradeAPI.calculateTotalPrice(dataAccess, saal, sitze);

            // ==== Film-ID & Saal-ID ====
            int filmId = 0;
            int saalId = 0;
            try (PreparedStatement stmt = dataAccess.getConnection()
                    .prepareStatement("SELECT f.id, s.id AS saal_id FROM film f JOIN saal s ON f.saal_id = s.id WHERE CONCAT(f.titel, ' (', f.dauer, ' min)') = ?")) {
                stmt.setString(1, filmAnzeige);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    filmId = rs.getInt("id");
                    saalId = rs.getInt("saal_id");
                }
            } catch (Exception ex) {
                tradeAPI.message("Fehler beim Filmabruf: " + ex.getMessage(), TYPE_MESSAGE.ERROR);
                return;
            }

            // ==== DB final schicken  ====
            String summary = "Bitte bestätigen Sie Ihre Buchung:\n\n"
                    + "Name: " + vorname + " " + nachname + "\n"
                    + "Film: " + filmAnzeige + "\n"
                    + "Sitze: " + sitze + "\n"
                    + "Gesamtpreis: " + gesamtpreis + " €\n\n"
                    + "Jetzt speichern?";

            int confirm = JOptionPane.showConfirmDialog(
                    parentPanel,
                    summary,
                    "Buchung bestätigen",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirm != JOptionPane.YES_OPTION) return;

            // ==== In DB senden ====
            try (PreparedStatement stmt = dataAccess.getConnection().prepareStatement(
                    "INSERT INTO buchung (vorname, nachname, film_id, saal_id, sitze, gesamtpreis) VALUES (?,?,?,?,?,?)"
            )) {
                stmt.setString(1, vorname);
                stmt.setString(2, nachname);
                stmt.setInt(3, filmId);
                stmt.setInt(4, saalId);
                stmt.setString(5, sitze);
                stmt.setDouble(6, gesamtpreis);
                stmt.executeUpdate();

                System.out.println("[INFO] Buchung erfolgreich gespeichert: " + vorname + " " + nachname
                        + " | Film-ID: " + filmId + " | Saal-ID: " + saalId
                        + " | Sitze: " + sitze + " | Preis: " + gesamtpreis);

                tradeAPI.message("Buchung erfolgreich gespeichert!", TYPE_MESSAGE.INFORMATION_MESSAGE);
                tradeAPI.resetSelection();
            } catch (Exception ex) {
                tradeAPI.message("Fehler beim Speichern: " + ex.getMessage(), TYPE_MESSAGE.ERROR);
            }
        });

        parentPanel.add(bookButton);
        return bookButton;
    }



    private JPanel defaultroomLayout(String saal) {
        JPanel pricePanel = new JPanel(null); // wichtig: null sonst crashout

        Font lFont = new Font("Arial", Font.BOLD, 18);

        // ===== Eltern =====
        double parentprice = tradeAPI.getPrice(dataAccess, saal, "erwachsen");
        JLabel lblEltern = new JLabel("Eltern: " + parentprice + "€");
        lblEltern.setFont(lFont);
        lblEltern.setBounds(30, 10, 180, 30);
        pricePanel.add(lblEltern);

        JButton btnParentMinus = new JButton("-");
        JTextField txtParent = new JTextField("0", 3);
        JButton btnParentPlus = new JButton("+");
        btnParentPlus.setFont(new Font("Arial", Font.BOLD, 12));
        btnParentMinus.setFont(new Font("Arial", Font.BOLD, 12));
        txtParent.setEditable(false);
        txtParent.setHorizontalAlignment(JTextField.CENTER);

        btnParentPlus.setBounds(180, 15, 45, 25);
        txtParent.setBounds(230, 15, 30, 25);
        btnParentMinus.setBounds(265, 15, 45, 25);

        pricePanel.add(btnParentPlus);
        pricePanel.add(txtParent);
        pricePanel.add(btnParentMinus);

        // ===== Kinder =====
        double pricekids = tradeAPI.getPrice(dataAccess, saal, "kind");
        JLabel lblKinder = new JLabel("Kinder: " + pricekids + "€");
        lblKinder.setFont(lFont);
        lblKinder.setBounds(30, 55, 180, 30);
        pricePanel.add(lblKinder);

        JButton btnKidsMinus = new JButton("-");
        JTextField txtKids = new JTextField("0", 3);
        JButton btnKidsPlus = new JButton("+");

        JTextField nowprice = new JTextField("0", 3);
        nowprice.setEditable(false);

        btnKidsPlus.setFont(new Font("Arial", Font.BOLD, 12));
        btnKidsMinus.setFont(new Font("Arial", Font.BOLD, 12));
        txtKids.setEditable(false);
        txtKids.setHorizontalAlignment(JTextField.CENTER);

        btnKidsPlus.setBounds(180, 60, 45, 25);
        txtKids.setBounds(230, 60, 30, 25);
        btnKidsMinus.setBounds(265, 60, 45, 25);
        nowprice.setBounds(350,60,45,25);

        pricePanel.add(btnKidsPlus);
        pricePanel.add(txtKids);
        pricePanel.add(btnKidsMinus);
        pricePanel.add(nowprice);

        // ===== Dropdown Film  =====
        JLabel lblFilm = new JLabel("Film:");
        lblFilm.setFont(new Font("Arial", Font.BOLD, 14));
        lblFilm.setBounds(390, 15, 60, 25);

        JComboBox<String> filmDropdown = new JComboBox<>();
        filmDropdown.setBounds(435, 15, 146, 25);

        // Filme laden aus der Datenbank(hopefully)
        try (PreparedStatement stmt = dataAccess.getConnection()
                .prepareStatement("SELECT f.id, f.titel, f.dauer FROM film f JOIN saal s ON f.saal_id = s.id WHERE s.name = ?")) {
            stmt.setString(1, saal);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String displayName = rs.getString("titel") + " (" + rs.getInt("dauer") + " min)";
                filmDropdown.addItem(displayName);
            }
        } catch (Exception ex) {
            System.err.println("Fehler beim Laden der Filme: " + ex.getMessage());
        }

        pricePanel.add(lblFilm);
        pricePanel.add(filmDropdown);

        // ===== Buchungssystem =====
        btnParentPlus.addActionListener(e -> {
            int newparent = tradeAPI.addTickets(txtParent);
            int kids = Integer.parseInt(txtKids.getText());
            tradeAPI.resetSelection();
            tradeAPI.setAllowedSeats(saal, newparent, kids);
            nowprice.setText(String.valueOf(newparent*tradeAPI.getPrice(dataAccess, saal, "erwachsen")));
        });
        btnParentMinus.addActionListener(e -> {
            int newparent = tradeAPI.removeTickets(txtParent);
            int kids = Integer.parseInt(txtKids.getText());
            tradeAPI.resetSelection();
            tradeAPI.setAllowedSeats(saal, newparent, kids);
        });
        btnKidsPlus.addActionListener(e -> {
            int newkids = tradeAPI.addTickets(txtKids);
            int parent = Integer.parseInt(txtParent.getText());
            tradeAPI.resetSelection();
            tradeAPI.setAllowedSeats(saal, parent, newkids);
        });
        btnKidsMinus.addActionListener(e -> {
            int newkids = tradeAPI.removeTickets(txtKids);
            int parent = Integer.parseInt(txtParent.getText());
            tradeAPI.resetSelection();
            tradeAPI.setAllowedSeats(saal, parent, newkids);
        });

        return pricePanel;
    }
    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (is == null) {
                throw new RuntimeException("config.properties nicht im resources Ordner gefunden");
            }
            props.load(is);
        } catch (IOException e) {
            throw new RuntimeException("config.properties konnte nicht geladen werden", e);
        }
        return props;
    }


}



