package history_database;

import java.sql.*;
import java.util.ArrayList;

/*
    Klasse som skal håndtere all datahåndtering til og fra database og
    data fra scanner
 */

public class DataHandler
{
    //# Fields
    private Database database;
    private FileScanner input;
    private ArrayList<Person> peopleInDatabase = new ArrayList<>();
    private ArrayList<Museum> museumsInDatabase = new ArrayList<>();
    private ArrayList<FoundItem> itemsInDatabase = new ArrayList<>();


    //# Constructor
    public DataHandler(Database database) {
        this.database = database;
        input = new FileScanner(database);
    }

    void parseFile() {
        input.readFile();
    }

    // Methods for adding new data to database
    public void addNewDataToDatabase() {
        addNewPeopleToDatabase();
    }

    public void addNewPeopleToDatabase() {
        var people = input.getPeople();

        for (Person person : people) {
            var duplicate = false;

            for (Person databasePerson : peopleInDatabase) {

                if (databasePerson.name().equals(person.name())) {
                    duplicate = true;
                    break;
                }
            }

            if (!duplicate) {
                if (addPersonToDatabase(person)) {
                    System.out.println(STR."//$ \{person.name()} added to database");
                }
            }

        }
    }

    private boolean addPersonToDatabase (Person person) {
        try (Connection connection = database.getConnection()) {

            String query = "INSERT INTO person VALUES(?, ?, ?, ?)";

            PreparedStatement statement = connection.prepareStatement(query);

            statement.setInt(1, person.id());
            statement.setString(2, person.name());
            statement.setInt(3, person.phone_number());
            statement.setString(4, person.email());

            int update = statement.executeUpdate();

            if (update > 0) {
                return true;
            }


        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Methods for printing info about items in Database (that is saved in local memory)
    void printAllCoins() {
        System.out.println("*** MYNTER FUNNET ***");
        int count = 1;
        for (FoundItem item : itemsInDatabase) {
            if (item instanceof ItemCoin) {
                ItemCoin coin = (ItemCoin) item;

                System.out.print(STR."Mynt #\{count} fra rundt år \{coin.expectedYearOfCreation} (ID: \{coin.id}). ");
                System.out.println(STR."Funnet av \{getPersonNameBasedOnID(coin.finder_id)} i \{coin.dateFound.substring(0,4)}.");
                System.out.println(STR."- \{coin.getDiameter()} mm i diameter og lagd av \{coin.getMetal().toLowerCase()}.");
                if (coin.museum_id != 0) {
                    System.out.println(STR."- For øyeblikket utstilt på \{getMuseumNameBasedOnID(coin.museum_id)}.");
                } else {
                    System.out.println("- Ikke utstilt på museum for øyeblikket. Så ligger i en boks i kjelleren på klubbhuset.");
                }
                System.out.println("");

                count++;
            }
        }
    }

    void printAllJewelry() {
        System.out.println("*** SMYKKER FUNNET ***");
        int count = 1;

        for (FoundItem item : itemsInDatabase) {
            if (item instanceof ItemJewelry) {
                ItemJewelry jewelry = (ItemJewelry) item;

                System.out.print(STR."Smykke #\{count}, \{jewelry.getJewelryType()} fra rundt år \{jewelry.expectedYearOfCreation} (ID: \{jewelry.id}). ");
                System.out.println(STR."Funnet av \{getPersonNameBasedOnID(jewelry.finder_id)} i \{jewelry.dateFound.substring(0,4)}.");
                System.out.println(STR."- Verdi estimert til \{jewelry.getValueEstimate()} kroner (se bilde: \{jewelry.getImageFilename()}).");
                if (jewelry.museum_id != 0) {
                    System.out.println(STR."- For øyeblikket utstilt på \{getMuseumNameBasedOnID(jewelry.museum_id)}.");
                } else {
                    System.out.println("- Ikke utstilt på museum for øyeblikket. Så ligger i safen på klubbhuset.");
                }
                System.out.println("");
                count++;

            }
        }
    }

    void printAllWeapons() {

    }


    private String getPersonNameBasedOnID(int person_id) {
        for (Person person : peopleInDatabase) {
            if (person.id() == person_id) {
                return person.name();
            }
        }
        return null;
    }

    private String getMuseumNameBasedOnID(int museum_id) {
        for (Museum museum : museumsInDatabase) {
            if(museum.id() == museum_id) {
                return museum.name();
            }
        }
        return null;
    }

    // Methods for loading data from database into program at start
    public void loadDataFromDatabaseAtStart() {
        loadPeopleFromDatabase();
        loadMuseumsFromDatabase();
        loadItemsFromDatabase();
    }

    public void loadPeopleFromDatabase() {
        try (Connection connection = database.getConnection()) {

            Statement statement = connection.createStatement();

            ResultSet resultSet = statement.executeQuery("SELECT * FROM person");

            while (resultSet.next()) {
                var person = new Person(
                        resultSet.getInt("id"),
                        resultSet.getString("navn"),
                        resultSet.getInt("tlf"),
                        resultSet.getString("e_post")
                );

                peopleInDatabase.add(person);
            }

            System.out.println(STR."//$ \{peopleInDatabase.size()} people loaded from database");

        } catch (SQLException e) {
            System.out.println("Could not load people from database");
            throw new RuntimeException(e);
        }
    }

    public void loadMuseumsFromDatabase() {
        try (Connection connection = database.getConnection()) {

            Statement statement = connection.createStatement();

            ResultSet resultSet = statement.executeQuery("SELECT * FROM museum");

            while (resultSet.next()) {
                var museum = new Museum(
                        resultSet.getInt("id"),
                        resultSet.getString("navn"),
                        resultSet.getString("sted")
                );

                museumsInDatabase.add(museum);
            }

            System.out.println(STR."//$ \{museumsInDatabase.size()} museums loaded from database");

        } catch (SQLException e) {
            System.out.println("Could not load people from database");
            throw new RuntimeException(e);
        }
    }

    public void loadItemsFromDatabase() {
        try (Connection connection = database.getConnection()) {

            Statement statementC = connection.createStatement();

            // Load coins
            ResultSet resultCoins = statementC.executeQuery("SELECT * FROM mynt");

            while (resultCoins.next()) {
                var coin = new ItemCoin(
                        resultCoins.getInt("id"),
                        resultCoins.getString("funnsted"),
                        resultCoins.getInt("finner_id"),
                        resultCoins.getString("funntidspunkt"),
                        resultCoins.getInt("antatt_aarstall"),
                        resultCoins.getInt("museum_id"),
                        "Mynt",
                        resultCoins.getInt("diameter"),
                        resultCoins.getString("metall")
                );
                itemsInDatabase.add(coin);
            }

            // Load weapons
            Statement statementW = connection.createStatement();

            ResultSet resultWeapons = statementW.executeQuery("SELECT * FROM vaapen");

            while (resultWeapons.next()) {
                var weapon = new ItemWeapon(
                        resultWeapons.getInt("id"),
                        resultWeapons.getString("funnsted"),
                        resultWeapons.getInt("finner_id"),
                        resultWeapons.getString("funntidspunkt"),
                        resultWeapons.getInt("antatt_aarstall"),
                        resultWeapons.getInt("museum_id"),
                        "Våpen",
                        resultWeapons.getString("type"),
                        resultWeapons.getString("materiale"),
                        resultWeapons.getInt("vekt")
                );

                System.out.println(weapon);

                itemsInDatabase.add(weapon);
            }

            // Load jewelry
            Statement statementJ = connection.createStatement();

            ResultSet resultJewelry = statementJ.executeQuery("SELECT * FROM smykke");

            while (resultJewelry.next()) {
                var jewelry = new ItemJewelry(
                        resultJewelry.getInt("id"),
                        resultJewelry.getString("funnsted"),
                        resultJewelry.getInt("finner_id"),
                        resultJewelry.getString("funntidspunkt"),
                        resultJewelry.getInt("antatt_aarstall"),
                        resultJewelry.getInt("museum_id"),
                        "Smykke",
                        resultJewelry.getString("type"),
                        resultJewelry.getInt("verdiestimat"),
                        resultJewelry.getString("filnavn")
                );

                System.out.println(jewelry);

                itemsInDatabase.add(jewelry);
            }


            System.out.println(STR."//$ \{itemsInDatabase.size()} items loaded from database");

        } catch (SQLException e) {
            System.out.println("Could not load items from database");
            throw new RuntimeException(e);
        }
    }

}
