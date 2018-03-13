package service;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class ColombianFootballControllerTest {

    private RestClient restClient = new RestClient("http://localhost:8080");

    @Rule
    public WireMockRule wireMock = new WireMockRule(8990);

    @Test
    public void happyPath() {
        //given
        String year = "2018";
        String league = "LIGA_APERTURA";
        stubFor(get(
                urlMatching("/city/stadium/.*"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "text/plain")
                                .withBody("Bogota")
                                .withStatus(200)
                        ));

        //when
        String response = restClient.get("todosLosPartidos/" + year + "/torneo/" + league);

        //then
        Assert.assertEquals(restClient.getStatus(), HttpStatus.OK);
        Assert.assertNotNull(response);
        JSONObject responseObj = new JSONObject(response);
        JSONArray matches =  responseObj.getJSONArray("matches");
        for (Object obj : matches) {
            Assert.assertEquals(((JSONObject) obj).get("city"), "Bogota");
        }
    }
}
