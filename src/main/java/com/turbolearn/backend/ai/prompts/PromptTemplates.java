package com.turbolearn.backend.ai.prompts;

public class PromptTemplates {

    public static String summary(String text) {
        return """
            Summarize the following text into clear, structured sections.

            Text:
            %s

            Guidelines:
            - Keep it concise
            - Capture main themes
            - Use bullet points if helpful
        """.formatted(text);
    }

    public static String flashcards(String text) {
        return """
            Create 5 flashcards from the text.
            
            Format strictly like this:
            Q: <question>
            A: <answer>

            Text:
            %s
        """.formatted(text);
    }

    public static String quiz(String text) {
        return """
            Generate 5 multiple-choice questions based on the text.

            Format strictly:
            Q: <question>
            A: <correct answer>
            Options: <B, C, D>

            Text:
            %s
        """.formatted(text);
    }
}
