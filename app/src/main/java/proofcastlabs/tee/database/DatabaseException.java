package proofcastlabs.tee.database;

public class DatabaseException extends Exception {
    DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(Exception e) {
        super(e);
    }
}
