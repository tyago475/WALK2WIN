// AppDatabase.java
package pt.ubi.pdm.quizpenaltis.data.db;

import pt.ubi.pdm.quizpenaltis.model.Question;
import pt.ubi.pdm.quizpenaltis.model.UserProgress;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import android.content.Context;

@Database(entities = {Question.class, UserProgress.class}, version = 3, exportSchema = false)
@TypeConverters(DateConverter.class)
public abstract class AppDatabase extends RoomDatabase {

    public abstract QuestionDao questionDao();
    public abstract UserProgressDao userProgressDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "quiz_penaltis.db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }


}