package ekenya.co.ke.dbapiv3;

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
    public Object fetchStoredProcedures(){
        return apiService.getStoredProcedures();
    }

}
