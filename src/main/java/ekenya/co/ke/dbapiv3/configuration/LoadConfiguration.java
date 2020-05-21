package ekenya.co.ke.dbapiv3.configuration;

import com.google.gson.JsonArray;

import ekenya.co.ke.dbapiv3.DbApiV3Application;
import ekenya.co.ke.dbapiv3.services.DatabaseExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.logging.Logger;

//@Configuration
@Component
public class LoadConfiguration {

    private final static Logger logger = Logger.getLogger(LoadConfiguration.class.getName());
    @Autowired
    private DatabaseExtractor databaseExtractor;

    @Resource
    private Environment environment;

    public void updateSpListStore() throws Exception {
        JsonArray jsonArray = databaseExtractor.fetchSqlProcedures();
        databaseExtractor.updateCurrentSpStore(jsonArray);
        String spLocation = environment.getProperty("file-storage.sp-location");
        logger.info("sp file location -- "+spLocation);
        DbApiV3Application.spJsonElements = databaseExtractor.retrieveFileContent(spLocation);
        logger.info("primary sp cache load updated...");
    }

    public void updateQueryTemplate(){
         String fileName = environment.getProperty("file-storage.query-template");
         String queryData = environment.getProperty("file-storage.sql-queries");

         DbApiV3Application.queryTemplate = databaseExtractor.retrieveFileContent(fileName);
         DbApiV3Application.sqlQueries = databaseExtractor.retrieveFileContent(queryData);

        logger.info(DbApiV3Application.queryTemplate.toString());
        logger.info(DbApiV3Application.sqlQueries.toString());

    }



}
