package fr.ans.psc.pscload.service.task;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

/**
 * The type Task.
 */
public abstract class Task {

    private static final Logger log = LoggerFactory.getLogger(Task.class);

    private final OkHttpClient client = new OkHttpClient();

    // We use this header in order to close the connection after each request
    final Request.Builder requestBuilder = new Request.Builder().header("Connection", "close");

    public void send() {}

    void sendRequest(Request request) {
        Call call = client.newCall(request);
        try {
            Response response = call.execute();
            String responseBody = Objects.requireNonNull(response.body()).string();
            log.debug("response body: {}", responseBody);
            response.close();
        } catch (IOException e) {
            log.error("error: {}", e.getMessage());
        }
    }

}
