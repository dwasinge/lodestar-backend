package com.redhat.labs.utils;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class MongoTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoTestResource.class);
    private static MongodExecutable MONGO;
    private static MongoClient mongoClient;

    @Override
    public Map<String, String> start() {
        try {
            Version.Main version = Version.Main.V3_6;
            int port = 12345;
            LOGGER.info("Starting Mongo {} on port {}", version, port);
            IMongodConfig config = new MongodConfigBuilder()
                    .version(version)
                    .net(new Net(port, Network.localhostIsIPv6()))
                    .build();
            MONGO = getMongodExecutable(config);
            try {
                MONGO.start();
            } catch (Exception e) {
                //every so often mongo fails to start on CI runs
                //see if this helps
                Thread.sleep(1000);
                MONGO.start();
            }
            // create the client after mongo starts
            createMongoClient();
            return Collections.emptyMap();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private MongodExecutable getMongodExecutable(IMongodConfig config) {
        try {
            return doGetExecutable(config);
        } catch (Exception e) {
            // sometimes the download process can timeout so just sleep and try again
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {

            }
            return doGetExecutable(config);
        }
    }

    private MongodExecutable doGetExecutable(IMongodConfig config) {
        return MongodStarter.getDefaultInstance().prepare(config);
    }

    private void createMongoClient() {

        if (null == mongoClient) {
            mongoClient = MongoClients.create("mongodb://localhost:12345");
        }

    }

    @Override
    public void stop() {
        if (MONGO != null) {
            MONGO.stop();
        }
    }

    @Override
    public void inject(Object testInstance) {
        Class<?> c = testInstance.getClass();
        while (c != Object.class) {
            System.out.println("Find class: " + c.getName());
            for (Field f : c.getDeclaredFields()) {
                System.out.println("Find field: " + f.getName());
                InjectMongoClient ano = f.getAnnotation(InjectMongoClient.class);
                System.out.println("Find annotation: " + ano);
                if (ano != null) {
                    if (!MongoClient.class.isAssignableFrom(f.getType())) {
                        throw new RuntimeException("@InjectMongoClient can only be used on fields of type MongoClient");
                    }

                    f.setAccessible(true);
                    try {
                        f.set(testInstance, mongoClient);
                        return;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            c = c.getSuperclass();
        }
    }

}
