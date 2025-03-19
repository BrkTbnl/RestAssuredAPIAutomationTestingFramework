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

public class mainForPersonel {

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
                            .header("campusid", "67bbed72-edc7-4e79-9724-b3e7c2e1e430")
                            .spec(reqSpec)
                            .when()
                            .get("/api/employees?page=" + page + "&perPage=" + perPage + "&isActive=true")
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
            String AddressUpdate = randomGenerator.address().fullAddress();

            // Fetch the current student data
            Response response = given()
                    .header("campusid", "all")
                    .spec(reqSpec)
                    .when()
                    .get("/api/employees/" + ID)
                    .then()
                    .log().body()
                    .statusCode(200)
                    .extract().response();

            Map<String, Object> update = response.jsonPath().getMap("data");
            String currentFirstName = (String) update.get("firstname");
            String currentLastName = (String) update.get("lastname");
            String currentEmail = (String) update.get("email");
            List<Map<String, Object>> phones = response.jsonPath().getList("data.phones");

            if (currentFirstName != null && !currentFirstName.contains("qa") &&
                    currentLastName != null && !currentLastName.contains("qa")
                    && !currentEmail.contains("oguzhanbalta.e12@gmail.com")
                    && !currentEmail.contains("zeynephoca@e12.com.tr.deneme")
                    && !currentEmail.contains("saadetroach@gmail.com")
                    && !currentEmail.contains("g.ozmen@nesibeaydin.k12.tr")
            ) {
                update.put("firstname", NameUpdate);
                update.put("lastname", SurnameUpdate);
                update.put("email", EmailUpdate);
                update.put("identityNumber", tcKimlikNoOlustur());
                if(phones.isEmpty()){
                    given()
                            .header("campusid", "11111111-1111-1111-1111-222222222222")
                            .spec(reqSpec)
                            .when()
                            .delete("/api/employees/" + ID)
                            .then()
                            .log().body()
                            .statusCode(200)
                            .extract().response();
                }else {
                    phones.get(0).put("phoneNumber", "(533) 333-3333");
                }

                List<Map<String, Object>> addresses = response.jsonPath().getList("data.addresses");
                if (addresses != null && !addresses.isEmpty()) {
                    addresses.get(0).put("detail", AddressUpdate);
                }
                update.put("addresses", addresses);

            }

            given()
                    .header("campusid", "11111111-1111-1111-1111-222222222222")
                    .spec(reqSpec)
                    .body(update)
                    .when()
                    .put("/api/employees/" + ID)
                    .then()
                    .log().body()
                    .statusCode(200);


        }
    }
}