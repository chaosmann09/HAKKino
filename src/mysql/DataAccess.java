package mysql;

import java.sql.*;

public class DataAccess {

    private final String host;
    private final String port;
    private final String database;
    private final String user;
    private final String password;
    private Connection dbConnection;

    public DataAccess(String host, String port, String database, String user, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
    }

    public boolean openConnection() {
        boolean connection = false;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("MySQL JDBC-Treiber nicht gefunden: " + e);
            return false;
        }

        String url = "jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database + "?useSSL=false&serverTimezone=UTC";
        try {
            dbConnection = DriverManager.getConnection(url, this.user, this.password);
            connection = true;
            System.out.println("Datenbankverbindung hergestellt!");
        } catch (SQLException e) {
            System.out.println("Datenbankverbindung ist fehlgeschlagen: " + e);
        }

        return connection;
    }


    public void closeConnection() {
        if (dbConnection != null) {
            try {
                dbConnection.close();
                System.out.println("Datenbankverbindung getrennt");
            } catch (SQLException e) {
                System.out.println("Verbindung konnte nicht geschlossen werden. " + e);
            }
        }
    }

    public ResultSet executeQuery(String sql) throws SQLException {
        Statement stmt = dbConnection.createStatement();
        return stmt.executeQuery(sql);
        //
    }

    public String raumMitSQL(String ziel, String tabelle, int raumnummer) {
        String sql = "SELECT " + ziel + " FROM " + tabelle + " WHERE saal = ?";
        try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
            stmt.setInt(1, raumnummer);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString(ziel); // gibt den Wert der Spalte 'ziel' zurück
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null; // wenn nichts gefunden oder Fehler
    }
    public int executeUpdate(String sql) throws SQLException {
        Statement stmt = dbConnection.createStatement();
        return stmt.executeUpdate(sql);

    }

    public Connection getConnection() {
        return dbConnection;
    }
}
