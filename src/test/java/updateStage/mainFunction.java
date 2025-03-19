package updateStage;

import com.github.javafaker.Faker;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;

import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.given;

public class mainFunction {

    Faker randomGenerator = new Faker();
    RequestSpecification reqSpec;
    String authTokenFirst;
    String authTokenSec;
    List<String> IDs;


    public static String tcKimlikNoOlustur() {
        Random random = new Random();
        int[] tcNo = new int[11];

        tcNo[0] = random.nextInt(9) + 1; // İlk rakam 0 olamaz
        for (int i = 1; i < 9; i++) {
            tcNo[i] = random.nextInt(10);
        }
        int oddSum = tcNo[0] + tcNo[2] + tcNo[4] + tcNo[6] + tcNo[8];
        int evenSum = tcNo[1] + tcNo[3] + tcNo[5] + tcNo[7];
        tcNo[9] = (7 * oddSum + 9 * evenSum) % 10;

        tcNo[10] = (8 * oddSum) % 10;
        if ((oddSum + evenSum + tcNo[9]) % 10 != tcNo[10]) {
            return tcKimlikNoOlustur();
        }
        StringBuilder sb = new StringBuilder();
        for (int num : tcNo) {
            sb.append(num);
        }
        return sb.toString();
    }


    @BeforeClass
    public void setup() {

        baseURI = "https://stage.e12test.com";

        // Sign in and get the authentication token
        Map<String, String> loginCredentials = new HashMap<>();
        loginCredentials.put("organizationID", "11111111-1111-1111-1111-111111111111");
        loginCredentials.put("identifier", "buraktabanli.e12@gmail.com");
        loginCredentials.put("password", "Ankara123!");

        Response responseFirstLogin =
                given()
                        .contentType(ContentType.JSON)
                        .body(loginCredentials)
                        .when()
                        .post("/api/authentication/login")
                        .then()
                        .log().all()  // Log the response for debugging
                        .statusCode(200)
                        .extract().response();

        authTokenFirst = responseFirstLogin.path("accessToken");
        String setCookieHeader = responseFirstLogin.getHeader("Set-Cookie");

        authTokenSec =
                given()
                        .header("Authorization", "Bearer " + authTokenFirst)
                        .header("Cookie", setCookieHeader)
                        .contentType(ContentType.JSON)
                        .when()
                        .post("/api/authentication/access?campus_id=11111111-1111-1111-1111-222222222222")
                        .then()
                        .log().all()
                        .statusCode(200)
                        .extract().path("data.accessToken");

        reqSpec = new RequestSpecBuilder()
                .addHeader("Authorization", "Bearer " + authTokenSec)
                .setContentType(ContentType.JSON)
                .build();
    }

    @Test
    public void getCommentsAndIDs() {
        IDs = new ArrayList<>();
        int page = 1;
        int perPage = 100;
        List<String> currentBatch;

        do {
            currentBatch =
                    given()
                            .header("campusid", "all")
                            .spec(reqSpec)
                            .when()
                            .get("/api/students?page=" + page + "&perPage=" + perPage + "&isActive=false")
                            .then()
                            .log().body()
                            .statusCode(200)
                            .extract().path("data.items.id"); // Assuming the IDs are in a list under "data"
            IDs.addAll(currentBatch);
            page++;
        } while (currentBatch.size() == perPage); // continue until a page with less than perPage elements is found

        System.out.println("Total IDs fetched: " + IDs.size());
    }

    @Test(dependsOnMethods = "getCommentsAndIDs")
    public void fetchAndUpdateStudent() {
        for (String ID : IDs) {  // IDs list should be of type List<String>

            String NameUpdate = randomGenerator.name().firstName();
            String SurnameUpdate = randomGenerator.name().lastName();
            String EmailUpdate = randomGenerator.internet().emailAddress();
            String UsernameUpdate = randomGenerator.name().username();
            String AddressUpdate = randomGenerator.address().fullAddress();
            String FirstNameParentUpdate = randomGenerator.name().firstName();
            String LastNameParentUpdate = randomGenerator.name().lastName();
            String EmailParentUpdate = randomGenerator.internet().emailAddress();
            String UsernameParentUpdate = randomGenerator.name().username();
            String randomnumber = randomGenerator.number().digits(2);

            // Fetch the current student data
            Response response = given()
                    .header("campusid", "11111111-1111-1111-1111-222222222222")
                    .spec(reqSpec)
                    .when()
                    .get("/api/students/" + ID)
                    .then()
                    .log().body()
                    .statusCode(200)
                    .extract().response();

            List<Map<String, Object>> parents = response.jsonPath().getList("data.parents");

            if (parents.isEmpty() || parents.size() >= 3) {
                given()
                        .header("campusid", "11111111-1111-1111-1111-222222222222")
                        .spec(reqSpec)
                        .when()
                        .delete("/api/students/" + ID)
                        .then()
                        .log().body()
                        .statusCode(200)
                        .extract().response();

            } else {

                Map<String, Object> update = response.jsonPath().getMap("data");
                String currentFirstName = (String) update.get("firstname");
                String currentLastName = (String) update.get("lastname");

                if (currentFirstName != null && !currentFirstName.contains("qa") && currentLastName != null && !currentLastName.contains("qa")) {
                    update.put("firstname", NameUpdate + randomnumber);
                    update.put("lastname", SurnameUpdate + randomnumber);
                    update.put("email", EmailUpdate + randomnumber);
                    update.put("identityNumber", tcKimlikNoOlustur());
                }


                // Update addresses
                List<Map<String, Object>> addresses = response.jsonPath().getList("data.addresses");
                if (addresses != null && !addresses.isEmpty()) {
                    addresses.get(0).put("detail", AddressUpdate);
                }
                update.put("addresses", addresses);

                // Update parents
                // Update the first parent
                parents.get(0).put("firstname", FirstNameParentUpdate + randomnumber);
                parents.get(0).put("lastname", LastNameParentUpdate + randomnumber);
                parents.get(0).put("email", EmailParentUpdate + randomnumber);
                parents.get(0).put("identityNumber", tcKimlikNoOlustur());

                // Check if there is a second parent and update if exists
                if (parents.size() > 1) {
                    parents.get(1).put("firstname", FirstNameParentUpdate + "2");
                    parents.get(1).put("lastname", LastNameParentUpdate + "2");
                    parents.get(1).put("email", EmailParentUpdate + "2");
                    parents.get(0).put("identityNumber", tcKimlikNoOlustur());

                }

                update.put("parents", parents);

                given()
                        .header("campusid", "11111111-1111-1111-1111-222222222222")
                        .spec(reqSpec)
                        .body(update)
                        .when()
                        .put("/api/students/" + ID)
                        .then()
                        .log().body()
                        .statusCode(200);

            }
        }
    }
}