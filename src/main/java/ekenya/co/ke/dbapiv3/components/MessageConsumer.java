package ekenya.co.ke.dbapiv3.components;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ekenya.co.ke.dbapiv3.controller.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
public class MessageConsumer {

    @Autowired private ObjectMapper objectMapper;
    @Autowired private JsonParser jsonParser;

    private final static Logger logger = Logger.getLogger(Controller.class.getName());

    @JmsListener(destination = "log-processor")
    public void writeDbApiLogs(String  jsonObject){

        try {
            Object j = objectMapper.readValue(jsonObject , Object.class);
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(j);
            logger.info("======================================================\n"+json+
                    "\n================================");
        } catch (JsonProcessingException e) {
            logger.info("failed to right log for : "+e.getMessage());
        }
    }
}
