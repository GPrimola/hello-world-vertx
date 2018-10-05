package com.softowers;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class MyRestIT {

    @BeforeClass
    public static void tearUpRestAssured() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = Integer.getInteger("http.port", 8080);
    }

    @AfterClass
    public static void tearDownRestAssured() {
        RestAssured.reset();
    }

    @Test
    public void checkIndex() {
        get("/api/whiskies").then()
            .assertThat()
            .statusCode(200)
            .contentType(ContentType.JSON);
    }

    @Test
    public void checkFind() {
        final int id = get("/api/whiskies").then()
                .assertThat()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getInt("find { it.name=='Bowmore 15 Years Laimrig' }.id");

        get("/api/whiskies/" + id).then()
                .assertThat()
                .statusCode(200)
                .body("name", equalTo("Bowmore 15 Years Laimrig"))
                .body("origin", equalTo("Scotland, Islay"))
                .body("id", equalTo(id));
    }

    @Test
    public void checkCreate() {
        Whisky whisky = given()
                .body("{\"name\":\"Jameson\", \"origin\":\"Ireland\"}")
                .request().post("/api/whiskies").thenReturn().as(Whisky.class);

        assertThat(whisky.getName()).isEqualToIgnoringCase("Jameson");
        assertThat(whisky.getOrigin()).isEqualToIgnoringCase("Ireland");
        assertThat(whisky.getId()).isNotZero();

        // Check that it has created an individual resource, and check the content.
        get("/api/whiskies/" + whisky.getId()).then()
                .assertThat()
                .statusCode(200)
                .body("name", equalTo("Jameson"))
                .body("origin", equalTo("Ireland"))
                .body("id", equalTo(whisky.getId()));
    }

    @Test
    public void checkDelete() {
        Whisky whisky = given()
                .body("{\"name\":\"Jameson\", \"origin\":\"Ireland\"}")
                .request().post("/api/whiskies").thenReturn().as(Whisky.class);

        assertThat(whisky.getName()).isEqualToIgnoringCase("Jameson");
        assertThat(whisky.getOrigin()).isEqualToIgnoringCase("Ireland");
        assertThat(whisky.getId()).isNotZero();

        // Check that it has created an individual resource, and check the content.
        get("/api/whiskies/" + whisky.getId()).then()
                .assertThat()
                .statusCode(200)
                .body("name", equalTo("Jameson"))
                .body("origin", equalTo("Ireland"))
                .body("id", equalTo(whisky.getId()));

        // Delete the bottle
        delete("/api/whiskies/" + whisky.getId()).then()
                .assertThat()
                .statusCode(204);

        // Check that the resource is not available anymore
        get("/api/whiskies/" + whisky.getId()).then()
                .assertThat()
                .statusCode(404);
    }

    @Test
    public void checkUpdate() {
        Whisky whisky = given()
            .body("{\"name\":\"Jameson\", \"origin\":\"Ireland\"}")
            .request()
            .post("/api/whiskies")
            .thenReturn()
            .as(Whisky.class);

        assertThat(whisky.getId()).isNotNull();
        assertThat(whisky.getId()).isInstanceOf(Integer.class);

        given()
            .header("content-type", "application/json; charset=utf-8")
            .body("{ \"name\": \"Giorgio\", \"origin\": \"João Monlevade\" }")
        .put("/api/whiskies/" + whisky.getId())
            .then()
            .statusCode(200)
            .body("name", equalTo("Giorgio"))
            .body("origin", equalTo("João Monlevade"))
            .body("id", equalTo(whisky.getId()));
    }
}

