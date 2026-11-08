package pt.ubi.pdm.quizpenaltis.multiplayer;

import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/** Login anónimo; reutiliza a sessão se já existir. */
public final class AuthManager {

    public interface Callback {
        void onSuccess(String uid);
        void onError(Exception e);
    }

    private AuthManager() {}

    public static void signIn(Callback cb) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser current = auth.getCurrentUser();
        if (current != null) {
            cb.onSuccess(current.getUid());
            return;
        }
        auth.signInAnonymously().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                AuthResult result = task.getResult();
                FirebaseUser user = result != null ? result.getUser() : null;
                if (user != null) {
                    cb.onSuccess(user.getUid());
                    return;
                }
            }
            Exception e = task.getException();
            cb.onError(e != null ? e : new IllegalStateException("Login anonimo falhou"));
        });
    }
}
