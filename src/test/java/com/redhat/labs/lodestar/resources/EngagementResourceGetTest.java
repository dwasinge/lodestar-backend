package com.redhat.labs.lodestar.resources;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import com.google.common.collect.Lists;
import com.redhat.labs.lodestar.model.Category;
import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.utils.MockUtils;
import com.redhat.labs.lodestar.utils.TokenUtils;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

@QuarkusTest
class EngagementResourceGetTest extends EngagementResourceTestHelper {

    @Test
    void testGetEngagementWithAuthAndRoleSuccess() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement engagement = MockUtils.mockMinimumEngagement("c1", "e1", "1234");
        engagement.setProjectId(1234);
        Mockito.when(eRepository.findByUuid("1234", Optional.empty())).thenReturn(Optional.of(engagement));

        // GET
        given()
            .when()
                .auth()
                .oauth2(token)
                .get("/engagements/1234")
            .then()
                .statusCode(200)
                .body("customer_name", equalTo(engagement.getCustomerName()))
                .body("project_name", equalTo(engagement.getProjectName()))
                .body("project_id", equalTo(1234));

    }

    @Test
    void testGetEngagementWithAuthAndRoleDoesNotExist() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Mockito.when(eRepository.findByUuid("1234", Optional.empty())).thenReturn(Optional.empty());

        // GET
        given()
            .when()
                .auth()
                .oauth2(token)
                .get("/engagements/1234")
            .then()
                .statusCode(404);

    }

    /*
     *  GET ALL SCENARIOS:
     *  Positive:
     *   - get, no engagements, empty List
     *   - get, engagements, List
     */
    @Test
    void testGetEngagementWithAuthAndRoleSuccessNoEngagements() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Mockito.when(eRepository.findAll(Optional.empty())).thenReturn(Lists.newArrayList());

        // GET engagement
        Response response = 
        given()
            .when()
                .auth()
                .oauth2(token)
                .contentType(ContentType.JSON)
                .get("/engagements");

        assertEquals(200, response.getStatusCode());
        Engagement[] engagements = response.getBody().as(Engagement[].class);
        assertEquals(0, engagements.length);

        Mockito.verify(eRepository).findAll(Optional.empty());

    }

    @Test
    void testGetEngagementWithAuthAndRoleSuccessEngagments() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "e2", "1234");
        Engagement e2 = MockUtils.mockMinimumEngagement("c1", "e3", "4321");
        
        Mockito.when(eRepository.findAll(Optional.empty())).thenReturn(Lists.newArrayList(e1,e2));

        // GET engagement
        Response response = 
        given()
            .when()
                .auth()
                .oauth2(token)
                .contentType(ContentType.JSON)
                .get("/engagements");

        assertEquals(200, response.getStatusCode());
        Engagement[] engagements = quarkusJsonb.fromJson(response.getBody().asString(), Engagement[].class);
        assertEquals(2, engagements.length);

        Mockito.verify(eRepository).findAll(Optional.empty());

    }

    @ParameterizedTest
    @MethodSource("nullEmptyBlankSource")
    void testGetCategories(String input) throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);
        
        Mockito.when(eRepository.findAllCategoryWithCounts()).thenReturn(Lists.newArrayList(MockUtils.mockCategory("cat1")));
        
        // get suggestions
        Response r =given()
            .auth()
            .oauth2(token)
            .queryParam("suggest", input)
            .contentType(ContentType.JSON)
        .when()
            .get("/engagements/categories");

        assertEquals(200, r.getStatusCode());
        Category[] results = r.as(Category[].class);
        assertEquals(1, results.length);

    }

    @Test
    void testGetCategories() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);
        
        Mockito.when(eRepository.findCategorySuggestions("sug")).thenReturn(Lists.newArrayList(MockUtils.mockCategory("sugar")));
        
        // get suggestions
        Response r =given()
            .auth()
            .oauth2(token)
            .queryParam("suggest", "sug")
            .contentType(ContentType.JSON)
        .when()
            .get("/engagements/categories");

        assertEquals(200, r.getStatusCode());
        Category[] results = r.as(Category[].class);
        assertEquals(1, results.length);

    }
    
    

    
    // TODO:  This is probably a repository test
//    @Test
//    void testGetAllCategoriesAndGetSuggestion() throws Exception {
//
//        HashMap<String, Long> timeClaims = new HashMap<>();
//        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);
//
//        // create engagements with categories
//        mockEngagementsWithCategories().stream()
//            .forEach(e -> {
//
//                String body = quarkusJsonb.toJson(e);
//
//                given()
//                .when()
//                    .auth()
//                    .oauth2(token)
//                    .body(body)
//                    .contentType(ContentType.JSON)
//                    .post("/engagements")
//                .then()
//                    .statusCode(201);
//                    
//
//            });
//
//
//        // get all
//        Response r =
//        given()
//            .auth()
//            .oauth2(token)
//            .contentType(ContentType.JSON)
//        .when()
//            .get("/engagements/categories");
//
//        assertEquals(200, r.getStatusCode());
//        Category[] results = r.as(Category[].class);
//        assertEquals(4, results.length);
//        Map<String, Boolean> resultsMap = validateCategories(results);
//
//        assertTrue(resultsMap.containsKey("c1") && 
//                resultsMap.containsKey("c2") && 
//                resultsMap.containsKey("c4") && 
//                resultsMap.containsKey("e5"));
//
//        // get suggestions
//        r =given()
//            .auth()
//            .oauth2(token)
//            .queryParam("suggest", "c")
//            .contentType(ContentType.JSON)
//        .when()
//            .get("/engagements/categories");
//
//        assertEquals(200, r.getStatusCode());
//        results = r.as(Category[].class);
//        assertEquals(3, results.length);
//        resultsMap = validateCategories(results);
//
//        assertTrue(resultsMap.containsKey("c1") && 
//                resultsMap.containsKey("c2") && 
//                resultsMap.containsKey("c4") && 
//                !resultsMap.containsKey("e5"));
//
//        r = given()
//            .auth()
//            .oauth2(token)
//            .queryParam("suggest", "e")
//            .contentType(ContentType.JSON)
//        .when()
//            .get("/engagements/categories");
//
//        assertEquals(200, r.getStatusCode());
//        results = r.as(Category[].class);
//        assertEquals(1, results.length);
//        resultsMap = validateCategories(results);
//
//        assertTrue(!resultsMap.containsKey("c1") && 
//                !resultsMap.containsKey("c2") && 
//                !resultsMap.containsKey("c4") && 
//                resultsMap.containsKey("e5"));
//
//    }

    @ParameterizedTest
    @MethodSource("nullEmptyBlankSource")
    void testGetArtifactTypes(String input) throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Mockito.when(eRepository.findAllArtifactTypes()).thenReturn(Lists.newArrayList("a1","a2"));

        given()
            .auth()
            .oauth2(token)
            .contentType(ContentType.JSON)
        .when()
            .get("/engagements/artifact/types")
        .then()
            .statusCode(200)
            .body(containsString("a1"))
            .body(containsString("a2"));

    }
    
    @Test
    void testGetArtifacts() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Mockito.when(eRepository.findArtifactTypeSuggestions("a")).thenReturn(Lists.newArrayList("a1"));

        given()
            .auth()
            .oauth2(token)
            .contentType(ContentType.JSON)
            .queryParam("suggest", "a")
        .when()
            .get("/engagements/artifact/types")
        .then()
            .statusCode(200)
            .body(containsString("a1"));

    }

    // TODO:  should be repository test
//    @Test
//    void testGetArtifactTypes() throws Exception {
//
//        HashMap<String, Long> timeClaims = new HashMap<>();
//        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);
//
//        // create engagements with artifacts
//        mockEngagementWithArtifacts().stream()
//            .forEach(e -> {
//
//                String body = quarkusJsonb.toJson(e);
//
//                given()
//                .when()
//                    .auth()
//                    .oauth2(token)
//                    .body(body)
//                    .contentType(ContentType.JSON)
//                    .post("/engagements")
//                .then()
//                    .statusCode(201);
//
//
//            });
//
//        // get all artifact types
//        given()
//            .auth()
//            .oauth2(token)
//            .contentType(ContentType.JSON)
//        .when()
//            .get("/engagements/artifact/types")
//        .then()
//            .statusCode(200)
//            .body(containsString("demo"))
//            .body(containsString("report"))
//            .body(containsString("note"));
//
//        // get all artifact types by suggestion
//        given()
//            .auth()
//            .oauth2(token)
//            .contentType(ContentType.JSON)
//            .queryParam("suggest", "de")
//        .when()
//            .get("/engagements/artifact/types")
//        .then()
//            .statusCode(200)
//            .body(containsString("demo"));
//
//        given()
//            .auth()
//            .oauth2(token)
//            .contentType(ContentType.JSON)
//            .queryParam("suggest", "rE")
//        .when()
//            .get("/engagements/artifact/types")
//        .then()
//            .statusCode(200)
//            .body(containsString("report"));
//
//        given()
//            .auth()
//            .oauth2(token)
//            .contentType(ContentType.JSON)
//            .queryParam("suggest", "E")
//        .when()
//            .get("/engagements/artifact/types")
//        .then()
//            .statusCode(200)
//            .body(containsString("demo"))
//            .body(containsString("report"))
//            .body(containsString("note"));
//
//    }

}
