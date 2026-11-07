package pt.ubi.pdm.quizpenaltis.data.questions;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import pt.ubi.pdm.quizpenaltis.model.Question;

/** Lê as perguntas iniciais de {@code assets/questions.json}. */
final class QuestionLoader {
    private static final String ASSET_NAME = "questions.json";

    private QuestionLoader() {}

    static List<Question> loadFromAssets(Context context) throws IOException, JSONException {
        String json = readAsset(context);
        JSONArray array = new JSONArray(json);
        List<Question> questions = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) {
            JSONObject o = array.getJSONObject(i);
            questions.add(new Question(
                    o.getString("question"),
                    o.getString("a"), o.getString("b"), o.getString("c"),
                    o.getString("answer")));
        }
        return questions;
    }

    private static String readAsset(Context context) throws IOException {
        try (InputStream in = context.getAssets().open(ASSET_NAME)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }
}
