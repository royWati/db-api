package ekenya.co.ke.dbapiv3.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ekenya.co.ke.dbapiv3.exceptions.SqlInjectionException;
import ekenya.co.ke.dbapiv3.services.ApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.logging.Logger;

@RestController
public class Controller {
    private final static Logger logger = Logger.getLogger(Controller.class.getName());

    @Autowired
    ObjectMapper objectMapper;

    @Autowired JsonParser jsonParser;

    @Autowired
    JmsTemplate jmsTemplate;

    private static  final String logProcessorLocation = "log-processor";

    @Autowired private ApiService apiService;

    @PostMapping(value = "db-api/execute-raw-query",produces = "application/json")
    public Object executeRawSqlStatement(@RequestBody String query){

        Object response = apiService.executeRawSqlStatement(query);
        String uuid = UUID.randomUUID().toString();
        printLog(query, response, uuid);
        return response;
    }
    @PostMapping(value = "/db-api/execute-stored-procedure", produces = "application/json")
    public Object executeStoreProcedureQuery(@RequestBody String query){
        Object response = apiService.executeStoredProcedure(query);
        String uuid = UUID.randomUUID().toString();
        printLog(query, response, uuid);
        return response;
    }
    @PostMapping(value = "/db-api/fetch-stored-procedures", produces = "application/json")
    public Object fetchStoredProcedures() throws Exception {
        return apiService.getStoredProcedures();
    }
    @PostMapping(value = "/db-api/execute-operation", produces = "application/json")
    public Object executeServicedQuery(@RequestBody String request){

        Object response = apiService.executeSavedSqlStatements(request);
        String uuid = UUID.randomUUID().toString();

        printLog(request, response, uuid);
        try {
            return response;
        }catch (SqlInjectionException e){
            System.out.println(e.getMessage());
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("status","500");
            jsonObject.addProperty("message",e.getMessage());

            return jsonObject.toString();
        }

    }
    @PostMapping(value = "/db-api/fetch-database-operations",produces = "application/json")
    public Object fetchDatabaseOperations() throws JsonProcessingException {
        return apiService.fetchDatabaseOperations();
    }

    void printLog(String request, Object response , String trxId){

            String s;
            s =String.valueOf(response);
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("dbApiReference",trxId);
            jsonObject.add("request",jsonParser.parse(request));

           logger.info(trxId+"===============\n"+request+"\n==================");

            if (s.length() > 500){
                logger.info(trxId+"========================================\n"+s.substring(0,499)+"\n==================");
          //      jsonObject.addProperty("response",s.substring(0,499));
            }else{
                logger.info(trxId+"========================================\n"+s+"\n==================");
    //            jsonObject.add("response",jsonParser.parse(s));
            }





        //    jmsTemplate.convertAndSend(logProcessorLocation, jsonObject.toString());

    }
}
