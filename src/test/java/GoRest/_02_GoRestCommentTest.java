package GoRest;

import com.github.javafaker.Faker;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class _02_GoRestCommentTest {

    Faker randomGenerator = new Faker();
    int commentID = 0;
    RequestSpecification reqSpec;
    @BeforeClass
    public void setup() {

        baseURI = "https://gorest.co.in/public/v2/comments";

        reqSpec = new RequestSpecBuilder()
                .addHeader("Authorization", "Bearer b1f7c7789f92ce78dc739afe1bdb192eb27b9c97b328ae98409e0b12639e4340")
                .setContentType(ContentType.JSON)
                .build();
    }

    @Test
    public void createComment() {

        String fullName = randomGenerator.name().fullName();
        String email = randomGenerator.internet().emailAddress();
        String body = randomGenerator.lorem().paragraph();

        Map<String, String> newComment = new HashMap<>();
        newComment.put("post_id", "148802");
        newComment.put("name", fullName);
        newComment.put("email", email);
        newComment.put("body", body);

        commentID =
            given()
                .spec(reqSpec)
                .body(newComment)

                .when()
                .post("")

                .then()
                .log().body()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract().path("id");
    }

    @Test(dependsOnMethods = "createComment")
    public void getComment() {

        given()
            .spec(reqSpec)
            .when()
            .get("/"+commentID)
            .then()
            .log().body()
            .body("id", equalTo(commentID));
    }

    @Test(dependsOnMethods = "getComment")
    public void updateComment() {
        Map<String, String> update = new HashMap<>();
        update.put("name","bt");


        given()
            .spec(reqSpec)
            .body(update)
            .when()
            .put("/"+commentID)

            .then()
            .log().body()
            .statusCode(200)
            .body("id", equalTo(commentID))
            .body("name", equalTo("bt"));
    }

    @Test(dependsOnMethods = "updateComment")
    public void deleteComment() {

        given()
            .spec(reqSpec)
            .when()
            .delete("/"+commentID)

            .then()
            .log().all()
            .statusCode(204);
    }

    @Test(dependsOnMethods = "deleteComment")
    public void deleteCommentNegative(){

        given()
            .spec(reqSpec)

            .when()
            .delete("/"+commentID)

            .then()
            .log().body()
            .statusCode(404);
    }
}
