package com.redhat.labs.omp.resources;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import com.redhat.labs.utils.AbstractBackendTest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class VersionResourceTest extends AbstractBackendTest {

    @Test
    public void testValidResourceVersion() {
        given()
        .when()
            .contentType(ContentType.JSON)
            .get("/api/v1/version")
        .then()
            .statusCode(200)
            .body("containers.version",hasItem("master-abcdef"))
            .body("containers.version", hasItem("v1.1"));
    }   

    @Test
    public void testValidResourceVersion1() {

        given()
        .when()
            .contentType(ContentType.JSON)
            .header("Accept-version", "v1")
            .get("/api/version")
        .then()
            .statusCode(200)
            .statusCode(200)
            .body("containers.version",hasItem("master-abcdef"))
            .body("containers.version", hasItem("v1.1"));

    }

    @Test
    public void testValidResourceVersion2() {

        given()
        .when()
            .contentType(ContentType.JSON)
            .header("Accept-version", "v2")
            .get("/api/version")
        .then()
            .statusCode(200)
            .body("git_commit", is("abcdef"))
            .body("git_tag", is("master"));

    }

    @Test
    public void testValidResourceVersionNoAcceptVersionSupplied() {

        given()
        .when()
            .contentType(ContentType.JSON)
            .get("/api/version")
        .then()
            .statusCode(200)
            .body("git_commit", is("abcdef"))
            .body("git_tag", is("master"));

    }

    @Test
    public void testValidResourceVersionInvalidAcceptVersion() {

        given()
        .when()
            .contentType(ContentType.JSON)
            .header("Accept-version", "v8")
            .get("/api/version")
        .then()
            .statusCode(400);

    }

    @Test
    public void testValidResourceVersionManifest() {

        given()
        .when()
            .contentType(ContentType.JSON)
            .get("/api/version/manifest")
        .then()
            .statusCode(200)
            .body("main_version.name",is("ball"))
            .body("main_version.value", is("v2.0"))
            .body("component_versions.name", hasItem("launcher"))
            .body("component_versions.value", hasItem("v1.1"));

    }

}