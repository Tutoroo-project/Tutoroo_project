package com.tutoroo.config;

import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAiConfig {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    /**
     * [기능: OpenAI 채팅 모델 (GPT-4o 등)]
     */
    @Bean
    public OpenAiChatModel chatModel() {
        return new OpenAiChatModel(new OpenAiApi(apiKey));
    }

    /**
     * [기능: OpenAI 음성 합성 모델 (TTS)]
     * 설명: 텍스트를 입력받아 MP3 오디오 바이트로 변환하는 모델입니다.
     */
    @Bean
    public OpenAiAudioSpeechModel speechModel() {
        return new OpenAiAudioSpeechModel(new OpenAiAudioApi(apiKey));
    }
}