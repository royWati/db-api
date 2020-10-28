package ekenya.co.ke.dbapiv3.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import ekenya.co.ke.dbapiv3.exceptions.SqlInjectionException;
import ekenya.co.ke.dbapiv3.services.ApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.logging.Logger;

@RestController
public class Controller {
    private final static Logger logger = Logger.getLogger(Controller.class.getName());


    @Autowired private ApiService apiService;

    @PostMapping(value = "db-api/execute-raw-query",produces = "application/json")
    public Object executeRawSqlStatement(@RequestBody String query){
        return apiService.executeRawSqlStatement(query);
    }
    @PostMapping(value = "/db-api/execute-stored-procedure", produces = "application/json")
    public Object executeStoreProcedureQuery(@RequestBody String query){
        Object response = apiService.executeStoredProcedure(query);
        String uuid = UUID.randomUUID().toString();
        logger.info("=========================================================================================");
        logger.info("TRX ID : "+uuid);
        logger.info("REQUEST : "+query);
        logger.info("RESPONSE : "+response);
        logger.info("=========================================================================================");
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
        try {
            logger.info("=========================================================================================");
            logger.info("TRX ID : "+uuid);
            logger.info("REQUEST : "+request);
            logger.info("RESPONSE : "+response);
            logger.info("=========================================================================================");
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
}
