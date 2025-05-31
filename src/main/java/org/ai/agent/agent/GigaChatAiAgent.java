package org.ai.agent.agent;

import chat.giga.client.auth.AuthClient;
import chat.giga.client.auth.AuthClientBuilder;
import chat.giga.langchain4j.GigaChatChatModel;
import chat.giga.langchain4j.GigaChatChatRequestParameters;
import chat.giga.model.ModelName;
import chat.giga.model.Scope;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentByLineSplitter;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Configuration
public class GigaChatAiAgent {

    @Bean
    public ChatLanguageModel gigaChatModel() {
        return GigaChatChatModel.builder()
                .defaultChatRequestParameters(GigaChatChatRequestParameters.builder()
                        .modelName(ModelName.GIGA_CHAT_PRO)
                        .build())
                .authClient(AuthClient.builder()
                        .withOAuth(AuthClientBuilder.OAuthBuilder.builder()
                                .scope(Scope.GIGACHAT_API_PERS)
                                .authKey("{auth_key}")
                                .build())
                        .build())
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Bean
    public TierAssistant tierAssistant(ChatLanguageModel gigaChatModel) throws IOException {
        Document document = Document.from(new String(Files.readAllBytes(Paths.get(
                "resources/rag.json"
        ))));

        DocumentSplitter splitter = new DocumentByLineSplitter(100, 50);
        List<TextSegment> segments = splitter.split(document);

        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();

        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

        embeddingModel.embedAll(segments);

        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        for (int i = 0; i < segments.size(); i++) {
            embeddingStore.add(embeddings.get(i), segments.get(i));
        }

        EmbeddingStoreContentRetriever rag = EmbeddingStoreContentRetriever.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .maxResults(5)
                .minScore(0.2)
                .build();

        return AiServices.builder(TierAssistant.class)
                .chatLanguageModel(gigaChatModel)
                .contentRetriever(rag)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }
}
