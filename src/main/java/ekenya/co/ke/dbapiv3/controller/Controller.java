package ekenya.co.ke.dbapiv3.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import ekenya.co.ke.dbapiv3.services.ApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {

    @Autowired private ApiService apiService;

    @PostMapping(value = "db-api/execute-raw-query",produces = "application/json")
    public Object executeRawSqlStatement(@RequestBody String query){
        return apiService.executeRawSqlStatement(query);
    }
    @PostMapping(value = "/db-api/execute-stored-procedure", produces = "application/json")
    public Object executeStoreProcedureQuery(@RequestBody String query){
        return apiService.executeStoredProcedure(query);
    }
    @PostMapping(value = "/db-api/fetch-stored-procedures", produces = "application/json")
    public Object fetchStoredProcedures() throws Exception {
        return apiService.getStoredProcedures();
    }
    @PostMapping(value = "/db-api/execute-operation", produces = "application/json")
    public Object executeServicedQuery(@RequestBody String request){
        return apiService.executeSavedSqlStatements(request);
    }
    @PostMapping(value = "/db-api/fetch-database-operations",produces = "application/json")
    public Object fetchDatabaseOperations() throws JsonProcessingException {
        return apiService.fetchDatabaseOperations();
    }
}
