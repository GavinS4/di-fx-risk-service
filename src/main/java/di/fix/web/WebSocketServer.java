package di.fix.web;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.socket.*;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSocket
@EnableScheduling
public class WebSocketServer implements WebSocketConfigurer {

    private WebSocketSession session;
    private List<String[]> csvData;
    private int currentIndex = 0;

    public WebSocketServer() {
        loadCsvData();
    }

    private void loadCsvData() {
        try (CSVReader reader = new CSVReader(new FileReader("src/main/resources/prices_export.csv"))) {
            csvData = reader.readAll();
        } catch (IOException | CsvException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        System.out.println("Registering WebSocket handler at /ws");
        registry.addHandler(new TextWebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                WebSocketServer.this.session = session;
                System.out.println("WebSocket connection established");
            }

            @Override
            public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                System.out.println("Received message: " + message.getPayload());
            }
        }, "/ws").setAllowedOrigins("*");
    }

    @Scheduled(fixedRate = 15000)
    public void sendCsvData() throws IOException {
        if (session != null && session.isOpen() && csvData != null && !csvData.isEmpty()) {
            String[] row = csvData.get(currentIndex);
            session.sendMessage(new TextMessage(String.join(",", row)));
            currentIndex = (currentIndex + 1) % csvData.size();
        }
    }
}