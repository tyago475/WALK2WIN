// QuestionDao.java
package pt.ubi.pdm.quizpenaltis.data.db;

import pt.ubi.pdm.quizpenaltis.model.Question;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface QuestionDao {

    @Query("SELECT * FROM questions ORDER BY RANDOM() LIMIT 1")
    Question getRandomQuestion();

    @Query("SELECT * FROM questions WHERE id = :id")
    Question getQuestionById(int id);

    @Query("SELECT id FROM questions")
    List<Integer> getAllQuestionIds();

    @Insert
    void insertQuestion(Question question);

    @Insert
    void insertAllQuestions(List<Question> questions);

    @Query("SELECT COUNT(*) FROM questions")
    int getQuestionCount();

    @Query("DELETE FROM questions")
    void deleteAllQuestions();
}