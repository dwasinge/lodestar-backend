package com.redhat.labs.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;

import io.quarkus.test.common.QuarkusTestResource;

@QuarkusTestResource(MongoTestResource.class)
public abstract class AbstractBackendTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBackendTest.class);
    private static final List<String> DEFAULT_DATABASES = new ArrayList<>(Arrays.asList("admin", "config", "local"));

    @InjectMongoClient("testMongoClient")
    MongoClient mongoClient;

    @BeforeEach
    public void setup() {

        for(String dbName : mongoClient.listDatabaseNames()) {

            // skip any of the default mongo dbs
            if(DEFAULT_DATABASES.contains(dbName)) {
                continue;
            }

            LOGGER.info("dropping collections for database: {}", dbName);

            // get database
            MongoDatabase db = mongoClient.getDatabase(dbName);

            // drop each collection from db
            for (String collectionName : db.listCollectionNames()) {
                LOGGER.info("...dropping collection {}", collectionName);
                db.getCollection(collectionName).drop();
            }

        }

    }

}
