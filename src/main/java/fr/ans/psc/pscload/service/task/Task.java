package fr.ans.psc.pscload.service.task;

import fr.ans.psc.pscload.component.JsonFormatter;
import okhttp3.*;
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

    JsonFormatter JsonFormatter = new JsonFormatter();

    // We use this header in order to close the connection after each request
    final Request.Builder requestBuilder = new Request.Builder().header("Connection", "close");

    public void send() {}

    void sendRequest(Request request) {
        Call call = client.newCall(request);
        try {
            Response response = call.execute();
            String responseBody = Objects.requireNonNull(response.body()).string();
            ApiResponse apiResponse = JsonFormatter.apiResponseFromJson(responseBody);

            handleApiResponseLogging(apiResponse, responseBody);
            response.close();
        } catch (IOException e) {
            log.error("error: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleApiResponseLogging(ApiResponse apiResponse, String stringifiedBody) {
        if (apiResponse != null && apiResponse.getCode() != null) {
            switch (apiResponse.getCode()) {
                case 200:
                    log.debug("mongodb operation OK : " + stringifiedBody);
                    break;
                case 500:
                    log.error("mongodb internal server error : " + stringifiedBody);
                    break;
                case 404:
                    log.debug("entity not found" + stringifiedBody);
                    break;
                case 409:
                    log.debug("entity already exist : " + stringifiedBody);
                    break;
                default:
                    log.error("unknown api response type" + stringifiedBody);
                    break;
            }
        }

    }

    public static class ApiResponse {
        private String status;
        private String method;
        private String uri;
        private String message;
        private Integer code;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Integer getCode() {
            return code;
        }

        public void setCode(Integer code) {
            this.code = code;
        }
    }

}
