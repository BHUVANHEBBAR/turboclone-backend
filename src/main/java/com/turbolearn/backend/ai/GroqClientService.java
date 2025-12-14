package com.turbolearn.backend.ai;

import jakarta.annotation.PostConstruct;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;



@Service
public class GroqClientService {

    private final OkHttpClient client = new OkHttpClient.Builder().build();

    @Value("${groq.api.key}")
    private String apiKey;

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    public String generate(String prompt) throws Exception{

        JSONObject msg = new JSONObject()
                .put("role","user")
                .put("content",prompt);

        JSONObject bodyJson = new JSONObject()
                .put("model", "llama-3.1-8b-instant")
                .put("messages", new JSONArray().put(msg))
                .put("temperature",0.7);

        RequestBody body = RequestBody.create(
                bodyJson.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(GROQ_URL)
                .header("Authorization", "Bearer " +apiKey)
                .post(body)
                .build();

        try(Response response = client.newCall(request).execute()){


            String result = response.body().string();
            JSONObject json = new JSONObject(result);

            System.out.println(json);
            return json.
                    getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");

        }

    }

//    @PostConstruct
//    public void testGroq() throws Exception{
//        System.out.println(generate("Say hello in 3 words."));
//    }
}
