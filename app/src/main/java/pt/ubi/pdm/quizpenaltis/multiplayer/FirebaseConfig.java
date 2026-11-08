package pt.ubi.pdm.quizpenaltis.multiplayer;

import com.google.firebase.database.FirebaseDatabase;

/** Ligação à Realtime Database (região Europa: o URL não é o por omissão do SDK). */
public final class FirebaseConfig {

    public static final String DATABASE_URL =
            "https://walk2win-c8f21-default-rtdb.europe-west1.firebasedatabase.app";

    private FirebaseConfig() {}

    public static FirebaseDatabase database() {
        return FirebaseDatabase.getInstance(DATABASE_URL);
    }
}
